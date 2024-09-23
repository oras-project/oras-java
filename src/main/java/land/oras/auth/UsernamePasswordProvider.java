package land.oras.auth;

import org.jspecify.annotations.NullMarked;

/**
 * A provider for username and password authentication
 */
@NullMarked
public class UsernamePasswordProvider extends AbstractUsernamePasswordProvider {

    /**
     * Create a new username and password provider
     * @param username The username
     * @param password The password
     */
    public UsernamePasswordProvider(String username, String password) {
        super(username, password);
    }
}
