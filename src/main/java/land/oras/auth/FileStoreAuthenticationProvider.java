package land.oras.auth;

import land.oras.ContainerRef;
import land.oras.credentials.FileStore;
import land.oras.credentials.FileStore.Credential;
import land.oras.exception.OrasException;

/**
 * FileStoreAuthenticationProvider is an implementation of the AuthProvider interface.
 * It retrieves credentials from a FileStore and generates a Basic Authentication header.
 */
public class FileStoreAuthenticationProvider implements AuthProvider {

    private final FileStore fileStore;
    private final ContainerRef containerRef;
    private final UsernamePasswordProvider usernamePasswordAuthProvider;

    /**
     * Constructor for FileStoreAuthenticationProvider.
     *
     * @param fileStore     The FileStore instance to retrieve credentials from.
     * @param containerRef The server address for which to retrieve credentials.
     * @throws Exception     If an error occurs during authentication initialization.
     */
    public FileStoreAuthenticationProvider(FileStore fileStore, ContainerRef containerRef) throws Exception {
        this.fileStore = fileStore;
        this.containerRef = containerRef;
        Credential credential = fileStore.get(containerRef);
        if (credential == null) {
            throw new OrasException("No credentials found for containerRef");
        }
        this.usernamePasswordAuthProvider =
                new UsernamePasswordProvider(credential.getUsername(), credential.getPassword());
    }

    @Override
    public String getAuthHeader() {
        return usernamePasswordAuthProvider.getAuthHeader();
    }
}
