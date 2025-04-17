import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LogicalResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class VaultConnector {

    public static void main(String[] args) throws Exception {
        // ENV VARS
        String vaultAddress = System.getenv("SPRING_CLOUD_VAULT_URI");
        String vaultRole = System.getenv("SPRING_CLOUD_VAULT_KUBERNETES_ROLE");
        String vaultAuthPath = System.getenv("SPRING_CLOUD_VAULT_KUBERNETES_KUBERNETES_PATH");
        String vaultDbBackend = System.getenv("SPRING_CLOUD_VAULT_DATABASE_BACKEND");
        String vaultDbRole = System.getenv("SPRING_CLOUD_VAULT_DATABASE_ROLE");

        // 1. Read the Kubernetes service account JWT
        String jwt = Files.readString(Path.of("/var/run/secrets/kubernetes.io/serviceaccount/token"));

        // 2. Set up Vault config (no token yet)
        VaultConfig config = new VaultConfig()
                .address(vaultAddress)
                .engineVersion(2)
                .build();

        Vault vault = new Vault(config);

        // 3. Login with Kubernetes Auth
        AuthResponse loginResponse = vault.auth()
                .loginByKubernetes(vaultRole, jwt, vaultAuthPath);

        String clientToken = loginResponse.getAuthClientToken();

        // 4. Use the token to get dynamic MongoDB credentials
        Vault authVault = new Vault(config.token(clientToken));

        String credsPath = vaultDbBackend + "/creds/" + vaultDbRole;
        LogicalResponse secretResponse = authVault.logical().read(credsPath);

        Map<String, String> creds = secretResponse.getData();
        String username = creds.get("username");
        String password = creds.get("password");

        System.out.println("Vault returned Mongo credentials:");
        System.out.println("Username: " + username);
        System.out.println("Password: " + password);
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



