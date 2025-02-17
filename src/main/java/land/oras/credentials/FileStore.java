package land.oras.credentials;

import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import land.oras.ContainerRef;
import land.oras.exception.OrasException;
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
     * @throws OrasException if loading the configuration fails.
     */
    public static FileStore newFileStore(String configPath) throws OrasException {
        Config cfg = Config.load(configPath);
        return new FileStore(false, cfg);
    }

    /**
     * Retrieves credentials for the given containerRef.
     *
     * @param containerRef ContainerRef.
     * @return Credential object.
     * @throws OrasException if retrieval fails.
     */
    public Credential get(ContainerRef containerRef) throws OrasException {
        return config.getCredential(containerRef);
    }

    /**
     * Saves credentials for the given ContainerRef.
     *
     * @param containerRef ContainerRef.
     * @param credential Credential object.
     * @throws Exception if saving fails.
     */
    public void put(ContainerRef containerRef, Credential credential) throws Exception {
        if (disablePut) {
            throw new UnsupportedOperationException(ERR_PLAINTEXT_PUT_DISABLED);
        }
        validateCredentialFormat(credential);
        config.putCredential(containerRef, credential);
    }

    /**
     * Deletes credentials for the given container.
     *
     * @param containerRef .
     * @throws OrasException if deletion fails.
     */
    public void delete(ContainerRef containerRef) throws OrasException {
        config.deleteCredential(containerRef);
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
         * Loads the configuration from a JSON file at the specified path and populates the credential store.
         *
         * @param configPath The path to the JSON configuration file.
         * @return A {@code Config} object populated with the credentials from the JSON file.
         * @throws OrasException If an error occurs while reading or parsing the JSON file.
         */
        public static Config load(String configPath) throws OrasException {
            Config config = new Config();
            try (FileReader reader = new FileReader(configPath)) {
                // Deserialize the JSON file into a map of ContainerRef to Credential
                Map<String, Map<String, String>> credentials = JsonUtils.fromJson(reader, Map.class);

                // Populate the credential store with the parsed credentials
                for (Map.Entry<String, Map<String, String>> entry : credentials.entrySet()) {
                    Map<String, String> values = entry.getValue();
                    if (values != null) {
                        String username = values.get("username");
                        String password = values.get("password");
                        if (username != null && password != null) {
                            config.credentialStore.put(entry.getKey(), new Credential(username, password));
                        } else {
                            throw new OrasException(
                                    "Invalid credential entry: missing username or password for " + entry.getKey());
                        }
                    }
                }
            } catch (IOException e) {
                throw new OrasException("Failed to load configuration from path: " + configPath, e);
            } catch (ClassCastException e) {
                throw new OrasException("Invalid JSON structure in configuration file: " + configPath, e);
            }
            return config;
        }

        /**
         * Retrieves the {@code Credential} associated with the specified containerRef.
         *
         * @param containerRef The containerRef whose credential is to be retrieved.
         * @return The {@code Credential} associated with the containerRef, or {@code null} if no credential is found.
         */
        public Credential getCredential(ContainerRef containerRef) throws OrasException {
            if (credentialStore.containsKey(containerRef)) {
                return credentialStore.get(containerRef);
            } else {
                throw new OrasException("No credentials found for server address");
            }
        }

        /**
         * Associates the specified {@code Credential} with the given containerRef.
         * If a credential already exists for the containerRef, it will be replaced.
         *
         * @param containerRef The containerRef to associate with the credential.
         * @param credential    The {@code Credential} to store. Must not be {@code null}.
         * @throws NullPointerException If the provided credential is {@code null}.
         */
        public void putCredential(ContainerRef containerRef, Credential credential) {
            credentialStore.put(containerRef.toString(), credential);
        }

        /**
         * Removes the {@code Credential} associated with the specified containerRef.
         * If no credential is associated with the containerRef, this method does nothing.
         *
         * @param containerRef The containerRef whose credential is to be removed.
         */
        public void deleteCredential(ContainerRef containerRef) {
            credentialStore.remove(containerRef.toString());
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
