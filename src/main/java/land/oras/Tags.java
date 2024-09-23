package land.oras;

import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * The tags response object
 * @param name The name
 * @param tags The tags
 */
@NullMarked
public record Tags(String name, List<String> tags) {}
