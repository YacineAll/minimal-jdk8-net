<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>vault-mongo-k8s</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Vault Java Driver -->
        <dependency>
            <groupId>io.github.jopenlibs</groupId>
            <artifactId>vault-java-driver</artifactId>
            <version>5.1.0</version>
        </dependency>

        <!-- MongoDB Java Driver -->
        <dependency>
            <groupId>org.mongodb</groupId>
            <artifactId>mongodb-driver-sync</artifactId>
            <version>4.9.1</version>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.7</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>2.0.7</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                        <configuration>
                            <archive>
                                <manifest>
                                    <mainClass>com.example.Main</mainClass>
                                </manifest>
                            </archive>
                            <descriptorRefs>
                                <descriptorRef>jar-with-dependencies</descriptorRef>
                            </descriptorRefs>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>




package com.example;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            // Step 2: Load configuration from environment variables
            ConfigLoader configLoader = new ConfigLoader();
            Config config = configLoader.loadConfig();
            log.info("Configuration loaded successfully");

            // Step 3: Retrieve service account JWT from file
            String jwt = K8sUtils.readServiceAccountToken();
            log.info("Service account JWT token retrieved");

            // Step 4: Authenticate to Vault using Kubernetes auth
            VaultClient vaultClient = new VaultClient(config);
            String vaultToken = vaultClient.authenticateWithKubernetes(jwt);
            log.info("Successfully authenticated to Vault");

            // Step 5: Fetch MongoDB credentials from Vault
            Map<String, String> mongoCredentials = vaultClient.fetchMongoCredentials(vaultToken);
            log.info("MongoDB credentials fetched from Vault");

            // Step 6: Build MongoDB connection URI
            String mongoUri = String.format(
                config.getMongoDbUriTemplate(),
                mongoCredentials.get("username"),
                mongoCredentials.get("password")
            );
            log.info("MongoDB connection URI built");

            // Step 7: Connect to MongoDB and perform a simple operation
            try (MongoClient mongoClient = MongoClients.create(mongoUri)) {
                MongoDatabase database = mongoClient.getDatabase(config.getMongoDbDatabase());
                MongoCollection<Document> collection = database.getCollection("test");
                
                // Simple ping to verify connection
                Document result = database.runCommand(new Document("ping", 1));
                log.info("MongoDB connection successful: {}", result.toJson());
                
                // Count documents in collection as a simple test
                long count = collection.countDocuments();
                log.info("Number of documents in test collection: {}", count);
            }
            
            log.info("Application completed successfully");
        } catch (Exception e) {
            log.error("Application failed", e);
            System.exit(1);
        }
    }
}


package com.example;

public class Config {
    private final String vaultAddr;
    private final String vaultNamespace;
    private final String vaultK8sRole;
    private final String vaultK8sPath;
    private final String vaultSecretPath;
    private final String mongoDbUriTemplate;
    private final String mongoDbDatabase;

    public Config(String vaultAddr, String vaultNamespace, String vaultK8sRole, 
                  String vaultK8sPath, String vaultSecretPath, 
                  String mongoDbUriTemplate, String mongoDbDatabase) {
        this.vaultAddr = vaultAddr;
        this.vaultNamespace = vaultNamespace;
        this.vaultK8sRole = vaultK8sRole;
        this.vaultK8sPath = vaultK8sPath;
        this.vaultSecretPath = vaultSecretPath;
        this.mongoDbUriTemplate = mongoDbUriTemplate;
        this.mongoDbDatabase = mongoDbDatabase;
    }

    public String getVaultAddr() {
        return vaultAddr;
    }

    public String getVaultNamespace() {
        return vaultNamespace;
    }

    public String getVaultK8sRole() {
        return vaultK8sRole;
    }

    public String getVaultK8sPath() {
        return vaultK8sPath;
    }

    public String getVaultSecretPath() {
        return vaultSecretPath;
    }

    public String getMongoDbUriTemplate() {
        return mongoDbUriTemplate;
    }

    public String getMongoDbDatabase() {
        return mongoDbDatabase;
    }
}


package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    public Config loadConfig() {
        log.info("Loading configuration from environment variables");
        
        String vaultAddr = getRequiredEnv("VAULT_ADDR");
        String vaultNamespace = getRequiredEnv("VAULT_NAMESPACE");
        String vaultK8sRole = getRequiredEnv("VAULT_K8S_ROLE");
        String vaultK8sPath = getRequiredEnv("VAULT_K8S_PATH");
        String vaultSecretPath = getRequiredEnv("VAULT_SECRET_PATH");
        String mongoDbUriTemplate = getRequiredEnv("MONGODB_URI_TEMPLATE");
        String mongoDbDatabase = getRequiredEnv("MONGODB_DATABASE");
        
        return new Config(
            vaultAddr,
            vaultNamespace,
            vaultK8sRole,
            vaultK8sPath,
            vaultSecretPath,
            mongoDbUriTemplate,
            mongoDbDatabase
        );
    }
    
    private String getRequiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            String errorMessage = "Required environment variable not found: " + name;
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        return value;
    }
}


package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class K8sUtils {
    private static final Logger log = LoggerFactory.getLogger(K8sUtils.class);
    private static final String SERVICE_ACCOUNT_TOKEN_PATH = 
        "/var/run/secrets/kubernetes.io/serviceaccount/token";

    public static String readServiceAccountToken() throws IOException {
        log.info("Reading service account token from: {}", SERVICE_ACCOUNT_TOKEN_PATH);
        
        Path path = Paths.get(SERVICE_ACCOUNT_TOKEN_PATH);
        if (!Files.exists(path)) {
            String errorMessage = "Service account token file does not exist: " + SERVICE_ACCOUNT_TOKEN_PATH;
            log.error(errorMessage);
            throw new IOException(errorMessage);
        }
        
        String token = Files.readString(path).trim();
        log.debug("Service account token successfully read ({} characters)", token.length());
        return token;
    }
}



package com.example;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LogicalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class VaultClient {
    private static final Logger log = LoggerFactory.getLogger(VaultClient.class);
    
    private final Config config;
    private Vault vault;
    
    public VaultClient(Config config) {
        this.config = config;
    }
    
    public String authenticateWithKubernetes(String jwt) throws VaultException {
        log.info("Authenticating to Vault using Kubernetes auth method at path: {}", config.getVaultK8sPath());
        
        // Configure Vault client
        VaultConfig vaultConfig = new VaultConfig()
            .address(config.getVaultAddr())
            .nameSpace(config.getVaultNamespace())
            .build();
        
        this.vault = new Vault(vaultConfig);
        
        // Authenticate with Kubernetes
        Map<String, Object> authParams = new HashMap<>();
        authParams.put("jwt", jwt);
        authParams.put("role", config.getVaultK8sRole());
        
        AuthResponse response = vault.auth()
            .loginByKubernetes(config.getVaultK8sPath(), authParams);
        
        String clientToken = response.getAuthClientToken();
        log.debug("Successfully authenticated to Vault, token valid for {} seconds", response.getLeaseDuration());
        
        // Update Vault config with the new token
        vaultConfig.token(clientToken).build();
        
        return clientToken;
    }
    
    public Map<String, String> fetchMongoCredentials(String vaultToken) throws VaultException {
        log.info("Fetching MongoDB credentials from path: {}", config.getVaultSecretPath());
        
        // Update vault client with the token
        VaultConfig vaultConfig = new VaultConfig()
            .address(config.getVaultAddr())
            .nameSpace(config.getVaultNamespace())
            .token(vaultToken)
            .build();
        
        this.vault = new Vault(vaultConfig);
        
        // Read the secret
        LogicalResponse response = vault.logical().read(config.getVaultSecretPath());
        
        Map<String, String> data = response.getData();
        
        // Validate response
        if (!data.containsKey("username") || !data.containsKey("password")) {
            String errorMessage = "MongoDB credentials not found in Vault response";
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        
        log.debug("MongoDB credentials successfully retrieved for username: {}", data.get("username"));
        return data;
    }
}








