// pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>vault-mongodb-demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>vault-mongodb-demo</name>
    <description>Demo project for Spring Boot with Vault and MongoDB</description>
    
    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>
        
        <!-- Spring Cloud Vault -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-vault-config</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-vault-config-databases</artifactId>
        </dependency>
        
        <!-- Kubernetes support -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-kubernetes-client-config</artifactId>
        </dependency>
        
        <!-- Test dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>

// src/main/java/com/example/vaultmongodbdemo/VaultMongodbDemoApplication.java
package com.example.vaultmongodbdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MongoDBProperties.class)
public class VaultMongodbDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(VaultMongodbDemoApplication.class, args);
    }
}

// src/main/java/com/example/vaultmongodbdemo/MongoDBProperties.java
package com.example.vaultmongodbdemo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("mongodb")
public class MongoDBProperties {
    private String username;
    private String password;
    private String uri;
    private String database;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }
}

// src/main/java/com/example/vaultmongodbdemo/MongoDBConfig.java
package com.example.vaultmongodbdemo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoDBConfig extends AbstractMongoClientConfiguration {

    private final MongoDBProperties properties;

    public MongoDBConfig(MongoDBProperties properties) {
        this.properties = properties;
    }

    @Override
    protected String getDatabaseName() {
        return properties.getDatabase();
    }

    @Override
    public MongoClient mongoClient() {
        // Build connection string with credentials from Vault
        String connectionString = properties.getUri()
                .replace("${username}", properties.getUsername())
                .replace("${password}", properties.getPassword());
        
        ConnectionString connString = new ConnectionString(connectionString);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connString)
                .build();
        
        return MongoClients.create(settings);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }
}

// src/main/java/com/example/vaultmongodbdemo/TestController.java
package com.example.vaultmongodbdemo;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final MongoTemplate mongoTemplate;

    public TestController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/test-connection")
    public String testConnection() {
        try {
            // Test connection by listing collection names
            return "Connection successful! Collections: " + 
                   mongoTemplate.getCollectionNames().toString();
        } catch (Exception e) {
            return "Connection failed: " + e.getMessage();
        }
    }
}

// src/main/resources/application.yml
spring:
  application:
    name: vault-mongodb-demo
  config:
    import: "vault://"
  cloud:
    vault:
      uri: http://vault:8200
      authentication: KUBERNETES
      kubernetes:
        role: mongodb-role
        service-account-token-file: /var/run/secrets/kubernetes.io/serviceaccount/token
      kv:
        enabled: true
        backend: secret
        default-context: mongodb
      database:
        enabled: true
        role: mongodb-role
        backend: database

mongodb:
  uri: mongodb://${username}:${password}@mongodb:27017/${database}
  database: demo
