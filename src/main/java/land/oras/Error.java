package land.oras;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * An error object for OCI API
 * @param code The error code
 * @param message The error message
 * @param details The error details
 */
@NullMarked
public record Error(String code, String message, @Nullable String details) {}
