package land.oras.exception;

public class ConfigLoadingException extends Exception {
    public ConfigLoadingException(String message) {
        super(message);
    }

    public ConfigLoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}
