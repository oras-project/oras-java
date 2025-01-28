package land.oras.auth;

import land.oras.credentials.FileStore;
import land.oras.credentials.FileStore.Credential;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * FileStoreAuthenticationProvider is an implementation of the AuthProvider interface.
 * It retrieves credentials from a FileStore and generates a Basic Authentication header.
 */
public class FileStoreAuthenticationProvider implements AuthProvider {

    private final FileStore fileStore;
    private final String serverAddress;

    private String username;
    private String password;

    /**
     * Constructor for FileStoreAuthenticationProvider.
     *
     * @param fileStore     The FileStore instance to retrieve credentials from.
     * @param serverAddress The server address for which to retrieve credentials.
     */
    public FileStoreAuthenticationProvider(FileStore fileStore, String serverAddress) {
        this.fileStore = fileStore;
        this.serverAddress = serverAddress;
    }

    /**
     * Generates the Basic Authentication header for the provided server address.
     *
     * @return A Basic Authentication header string.
     * @throws RuntimeException if no credentials are found for the server address.
     */
    @Override
    public String getAuthHeader() {
        try {
            // Retrieve the credential for the server address
            Credential credential = fileStore.get(serverAddress);

            if (credential == null) {
                throw new RuntimeException("No credentials found for server address: " + serverAddress);
            }

            // Set the username and password fields
            this.username = credential.getUsername();
            this.password = credential.getPassword();

            // Generate Basic Auth header (Base64 encoding of "username:password")
            String authString = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));

            return "Basic " + encodedAuth;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate authentication header", e);
        }
    }

    /**
     * Gets the username of the retrieved credential.
     *
     * @return The username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the password of the retrieved credential.
     *
     * @return The password.
     */
    public String getPassword() {
        return password;
    }
}

