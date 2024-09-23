package land.oras.auth;

/**
 * Interface for auth provider
 * Must return the authentication header to pass to HTTP requests
 */
public interface AuthProvider {

    /**
     * Get the authentication header for this provider
     * @return The authentication header
     */
    String getAuthHeader();
}
