package land.oras.exception;

/**
 * Exception thrown to indicate an error occurred while loading a configuration.
 * This custom exception can be used to provide detailed error messages and propagate underlying causes.
 */
public class ConfigLoadingException extends Exception {

    /**
     * Constructs a new {@code ConfigLoadingException} with the specified detail message.
     *
     * @param message The detail message explaining the reason for the exception.
     */
    public ConfigLoadingException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code ConfigLoadingException} with the specified detail message and cause.
     *
     * @param message The detail message explaining the reason for the exception.
     * @param cause   The underlying cause of the exception. Can be {@code null} if there is no cause.
     */
    public ConfigLoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}
