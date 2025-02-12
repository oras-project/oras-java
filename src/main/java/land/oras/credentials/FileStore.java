package land.oras.credentials;

import com.google.gson.reflect.TypeToken;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import land.oras.exception.ConfigLoadingException;
import land.oras.utils.JsonUtils;

/**
 * FileStore implements a credentials store using a configuration file
 * to keep the credentials in plain-text.
 *
 * Reference: https://docs.docker.com/engine/reference/commandline/cli/#docker-cli-configuration-file-configjson-properties
 */
public class FileStore {

    private final boolean disablePut;
    private final Config config;

    /**
     * Error message indicating that putting plaintext credentials is disabled.
     * This is used to enforce security policies against storing sensitive credentials in plaintext format.
     */
    public static final String ERR_PLAINTEXT_PUT_DISABLED = "Putting plaintext credentials is disabled";

    /**
     * Error message indicating that the format of the provided credential is invalid.
     * This is typically used when credentials do not match the expected structure or format.
     */
    public static final String ERR_BAD_CREDENTIAL_FORMAT = "Bad credential format";

    /**
     * Constructor for FileStore.
     *
     * @param disablePut boolean flag to disable putting credentials in plaintext.
     * @param config configuration instance.
     */
    public FileStore(boolean disablePut, Config config) {
        this.disablePut = disablePut;
        this.config = Objects.requireNonNull(config, "Config cannot be null");
    }

    /**
     * Creates a new FileStore based on the given configuration file path.
     *
     * @param configPath Path to the configuration file.
     * @return FileStore instance.
     * @throws Exception if loading the configuration fails.
     */
    public static FileStore newFileStore(String configPath) throws Exception {
        Config cfg = Config.load(configPath);
        return new FileStore(false, cfg);
    }

    /**
     * Retrieves credentials for the given server address.
     *
     * @param serverAddress Server address.
     * @return Credential object.
     * @throws Exception if retrieval fails.
     */
    public Credential get(String serverAddress) throws Exception {
        return config.getCredential(serverAddress);
    }

    /**
     * Saves credentials for the given server address.
     *
     * @param serverAddress Server address.
     * @param credential Credential object.
     * @throws Exception if saving fails.
     */
    public void put(String serverAddress, Credential credential) throws Exception {
        if (disablePut) {
            throw new UnsupportedOperationException(ERR_PLAINTEXT_PUT_DISABLED);
        }
        validateCredentialFormat(credential);
        config.putCredential(serverAddress, credential);
    }

    /**
     * Deletes credentials for the given server address.
     *
     * @param serverAddress Server address.
     * @throws Exception if deletion fails.
     */
    public void delete(String serverAddress) throws Exception {
        config.deleteCredential(serverAddress);
    }

    /**
     * Validates the format of the credential.
     *
     * @param credential Credential object.
     * @throws Exception if the credential format is invalid.
     */
    private void validateCredentialFormat(Credential credential) throws Exception {
        if (credential.getUsername().contains(":")) {
            throw new IllegalArgumentException(ERR_BAD_CREDENTIAL_FORMAT + ": colons(:) are not allowed in username");
        }
    }

    /**
     * Nested Config class for configuration management.
     */
    public static class Config {
        private final ConcurrentHashMap<String, Credential> credentialStore = new ConcurrentHashMap<>();

        /**
         * Load configuration from a JSON file and populate the credential store.
         *
         * @param configPath Path to the JSON configuration file.
         * @return A Config instance with loaded credentials.
         * @throws ConfigLoadingException If the file cannot be read or parsed.
         */
        public static Config load(String configPath) throws ConfigLoadingException {

            Map<String, Credential> credentials =
                    JsonUtils.fromJson(Path.of(configPath), new TypeToken<Map<String, Credential>>() {}.getType());

            Config config = new Config();

            for (Map.Entry<String, Credential> entry : credentials.entrySet()) {

                String serverAddress = entry.getKey();
                Credential credential = entry.getValue();
                // Put the serverAddress and Credential into the credentialStore
                config.credentialStore.put(serverAddress, credential);
            }

            return config;
        }

        /**
         * Retrieves the {@code Credential} associated with the specified server address.
         *
         * @param serverAddress The address of the server whose credential is to be retrieved.
         * @return The {@code Credential} associated with the server address, or {@code null} if no credential is found.
         */
        public Credential getCredential(String serverAddress) {
            return credentialStore.get(serverAddress);
        }

        /**
         * Associates the specified {@code Credential} with the given server address.
         * If a credential already exists for the server address, it will be replaced.
         *
         * @param serverAddress The address of the server to associate with the credential.
         * @param credential    The {@code Credential} to store. Must not be {@code null}.
         * @throws NullPointerException If the provided credential is {@code null}.
         */
        public void putCredential(String serverAddress, Credential credential) {
            credentialStore.put(serverAddress, credential);
        }

        /**
         * Removes the {@code Credential} associated with the specified server address.
         * If no credential is associated with the server address, this method does nothing.
         *
         * @param serverAddress The address of the server whose credential is to be removed.
         */
        public void deleteCredential(String serverAddress) {
            credentialStore.remove(serverAddress);
        }
    }

    /**
     * Nested Credential class to represent username and password pairs.
     */
    public static class Credential {
        private String username;
        private String password;

        /**
         * Constructs a new {@code Credential} object with the specified username and password.
         *
         * @param username The username for the credential. Must not be {@code null}.
         * @param password The password for the credential. Must not be {@code null}.
         */
        public Credential(String username, String password) {
            this.username = Objects.requireNonNull(username, "Username cannot be null");
            this.password = Objects.requireNonNull(password, "Password cannot be null");
        }

        /**
         * Returns the username associated with this credential.
         *
         * @return The username as a {@code String}.
         */
        public String getUsername() {
            return username;
        }

        /**
         * Returns the password associated with this credential.
         *
         * @return The password as a {@code String}.
         */
        public String getPassword() {
            return password;
        }
    }
}
