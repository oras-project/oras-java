package land.oras.utils;

import org.jspecify.annotations.NullMarked;

/**
 * Constants used in the SDK.
 */
@NullMarked
public final class Const {

    /**
     * Hidden constructor
     */
    private Const() {
        // Private constructor
    }

    /**
     * Default registry
     */
    public static final String DEFAULT_REGISTRY = "docker.io";

    /**
     * Default tag
     */
    public static final String DEFAULT_TAG = "latest";

    /**
     * The default blob directory media type
     */
    public static final String DEFAULT_BLOB_DIR_MEDIA_TYPE = "application/vnd.oci.image.layer.v1.tar+gzip";

    /**
     * The default blob media type if file type cannot be determined
     */
    public static final String DEFAULT_BLOB_MEDIA_TYPE = "application/vnd.oci.image.layer.v1.tar";

    /**
     * The default descriptor media type
     */
    public static final String DEFAULT_DESCRIPTOR_MEDIA_TYPE = "application/octet-stream";

    /**
     * The default JSON media type
     */
    public static final String DEFAULT_JSON_MEDIA_TYPE = "application/json";

    /**
     * The default empty media type
     */
    public static final String DEFAULT_EMPTY_MEDIA_TYPE = "application/vnd.oci.empty.v1+json";

    /**
     * The default manifest media type
     */
    public static final String DEFAULT_MANIFEST_MEDIA_TYPE = "application/vnd.oci.image.manifest.v1+json";

    /**
     * Annotation for the title
     */
    public static final String ANNOTATION_TITLE = "org.opencontainers.image.title";

    /**
     * Annotation to unpack the content
     */
    public static final String ANNOTATION_ORAS_UNPACK = "io.deis.oras.content.unpack";

    /**
     * Authorization header
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Content type header
     */
    public static final String CONTENT_TYPE_HEADER = "Content-Type";

    /**
     * Accept header
     */
    public static final String ACCEPT_HEADER = "Accept";

    /**
     * Location header
     */
    public static final String LOCATION_HEADER = "Location";

    /**
     * WWW-Authenticate header
     */
    public static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

    /**
     * Application octet stream header value
     */
    public static final String APPLICATION_OCTET_STREAM_HEADER_VALUE = "application/octet-stream";
}
