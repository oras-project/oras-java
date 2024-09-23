package land.oras.auth;

/**
 * A provider without any authentication
 */
public class NoAuthProvider implements AuthProvider {

    /**
     * Constructor
     */
    public NoAuthProvider() {}

    @Override
    public String getAuthHeader() {
        return null;
    }
}
