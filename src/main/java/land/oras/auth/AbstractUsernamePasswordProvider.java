package land.oras.auth;

/**
 * A provider for username and password authentication
 */
public abstract class AbstractUsernamePasswordProvider implements AuthProvider {

    /**
     * The username
     */
    private final String username;

    /**
     * The password
     */
    private final String password;

    /**
     * Create a new username and password provider
     * @param username The username
     * @param password The password
     */
    public AbstractUsernamePasswordProvider(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Get the username
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get the password
     * @return The password
     */
    public String getPassword() {
        return password;
    }

    @Override
    public String getAuthHeader() {
        return "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }
}
