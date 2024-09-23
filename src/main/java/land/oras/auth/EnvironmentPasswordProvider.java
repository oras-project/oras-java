package land.oras.auth;

import org.jspecify.annotations.NullMarked;

/**
 * A provider using OCI_USERNAME and OCI_PASSWORD environment variables for authentication
 */
@NullMarked
public class EnvironmentPasswordProvider extends AbstractUsernamePasswordProvider {

    /**
     * Create a new provider using OCI_USERNAME and OCI_PASSWORD environment variables
     */
    public EnvironmentPasswordProvider() {
        this("OCI_USERNAME", "OCI_PASSWORD");
    }

    /**
     * Create a new provider using the given environment variables
     * @param usernameVar The environment variable for the username
     * @param passwordVar The environment variable for the password
     */
    public EnvironmentPasswordProvider(String usernameVar, String passwordVar) {
        super(System.getenv(usernameVar), System.getenv(passwordVar));
    }
}
