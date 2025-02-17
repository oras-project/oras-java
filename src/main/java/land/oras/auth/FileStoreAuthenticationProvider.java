package land.oras.auth;

import land.oras.ContainerRef;
import land.oras.credentials.FileStore;
import land.oras.credentials.FileStore.Credential;
import org.jspecify.annotations.NullMarked;

/**
 * FileStoreAuthenticationProvider is an implementation of the AuthProvider interface.
 * It retrieves credentials from a FileStore and generates a Basic Authentication header.
 */
@NullMarked
public class FileStoreAuthenticationProvider implements AuthProvider {

    /**
     * Delegate to username password authentication
     */
    private final UsernamePasswordProvider usernamePasswordAuthProvider;

    /**
     * Constructor for FileStoreAuthenticationProvider.
     *
     * @param fileStore     The FileStore instance to retrieve credentials from.
     * @param containerRef The server address for which to retrieve credentials.
     * @throws Exception     If an error occurs during authentication initialization.
     */
    public FileStoreAuthenticationProvider(FileStore fileStore, ContainerRef containerRef) throws Exception {
        Credential credential = fileStore.get(containerRef);
        this.usernamePasswordAuthProvider = new UsernamePasswordProvider(credential.username(), credential.password());
    }

    @Override
    public String getAuthHeader() {
        return usernamePasswordAuthProvider.getAuthHeader();
    }
}
