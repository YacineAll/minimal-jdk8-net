import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;

public class VaultMongoConnector {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        // Step 1: Read ENV vars
        String vaultUri = System.getenv("SPRING_CLOUD_VAULT_URI");
        String vaultRole = System.getenv("SPRING_CLOUD_VAULT_KUBERNETES_ROLE");
        String vaultAuthPath = System.getenv("SPRING_CLOUD_VAULT_KUBERNETES_KUBERNETES_PATH");
        String vaultDbBackend = System.getenv("SPRING_CLOUD_VAULT_DATABASE_BACKEND");
        String vaultDbRole = System.getenv("SPRING_CLOUD_VAULT_DATABASE_ROLE");

        // Step 2: Read Kubernetes service account JWT token
        String jwt = Files.readString(Path.of("/var/run/secrets/kubernetes.io/serviceaccount/token"));

        // Step 3: Authenticate with Vault
        String loginUrl = vaultUri + "/v1/auth/" + vaultAuthPath + "/login";

        String requestBody = """
            {
              "role": "%s",
              "jwt": "%s"
            }
            """.formatted(vaultRole, jwt);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(loginUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        String vaultToken = mapper.readTree(loginResponse.body()).at("/auth/client_token").asText();

        // Step 4: Fetch Mongo credentials from Vault
        String credsUrl = vaultUri + "/v1/" + vaultDbBackend + "/creds/" + vaultDbRole;

        HttpRequest credsRequest = HttpRequest.newBuilder()
                .uri(URI.create(credsUrl))
                .header("X-Vault-Token", vaultToken)
                .GET()
                .build();

        HttpResponse<String> credsResponse = client.send(credsRequest, HttpResponse.BodyHandlers.ofString());
        JsonNode credsJson = mapper.readTree(credsResponse.body());

        String username = credsJson.at("/data/username").asText();
        String password = credsJson.at("/data/password").asText();

        // Step 5: Connect to MongoDB using retrieved credentials
        // You may also want to make `host` an env var (like: MONGODB_HOST)
        String mongoUri = "mongodb://%s:%s@host:32348/ibmclouddb"
                .formatted(username, password);

        try (MongoClient mongoClient = MongoClients.create(mongoUri)) {
            System.out.println("✅ Connected to MongoDB successfully.");
            // You can now use the mongoClient instance
        }
    }
}


<!-- For MongoDB -->
<dependency>
  <groupId>org.mongodb</groupId>
  <artifactId>mongodb-driver-sync</artifactId>
  <version>4.11.0</version>
</dependency>

<!-- For JSON handling -->
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-databind</artifactId>
  <version>2.17.0</version>
</dependency>



