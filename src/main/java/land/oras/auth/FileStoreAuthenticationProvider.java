package land.oras.auth;

import land.oras.credentials.FileStore;
import land.oras.credentials.FileStore.Credential;
import land.oras.exception.ConfigLoadingException;

/**
 * FileStoreAuthenticationProvider is an implementation of the AuthProvider interface.
 * It retrieves credentials from a FileStore and generates a Basic Authentication header.
 */
public class FileStoreAuthenticationProvider implements AuthProvider {

    private final FileStore fileStore;
    private final String serverAddress;
    private final UsernamePasswordProvider usernamePasswordAuthProvider;

    /**
     * Constructor for FileStoreAuthenticationProvider.
     *
     * @param fileStore     The FileStore instance to retrieve credentials from.
     * @param serverAddress The server address for which to retrieve credentials.
     */
    public FileStoreAuthenticationProvider(FileStore fileStore, String serverAddress) throws Exception {
        this.fileStore = fileStore;
        this.serverAddress = serverAddress;
        Credential credential = fileStore.get(serverAddress);
        if (credential == null) {
            throw new ConfigLoadingException("No credentials found for server address: " + serverAddress);
        }
        this.usernamePasswordAuthProvider =
                new UsernamePasswordProvider(credential.getUsername(), credential.getPassword());
    }

    @Override
    public String getAuthHeader() {
        return usernamePasswordAuthProvider.getAuthHeader();
    }
}
