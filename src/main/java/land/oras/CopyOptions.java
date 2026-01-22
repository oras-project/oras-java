package land.oras;

/**
 * Options for the copy operation.
 */
public class CopyOptions {

    /**
     * Whether to copy referrers recursively.
     */
    private boolean recursive;

    // TODO: In the future, we will add 'depth' and 'filter' here!

    /**
     * Constructor
     * @param recursive true to copy referrers, false otherwise
     */
    public CopyOptions(boolean recursive) {
        this.recursive = recursive;
    }

    /**
     * Gets the recursive setting.
     * @return true if recursive
     */
    public boolean isRecursive() {
        return recursive;
    }
}
