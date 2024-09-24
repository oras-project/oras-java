package land.oras;

import java.util.Base64;
import java.util.Map;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Class for config
 */
@NullUnmarked
public final class Config {

    private final String mediaType;
    private final String digest;
    private final long size;

    /**
     * Annotations for the layer
     * Can be nullable due to serialization
     */
    private final @Nullable Map<String, String> annotations;

    /**
     * The base 64 encoded data. Never serialized because configuration
     * is always referenced by digest.
     */
    private final transient @Nullable String data;

    /**
     * Constructor
     * @param mediaType The media type
     * @param digest The digest
     * @param size The size
     */
    private Config(String mediaType, String digest, long size, @Nullable String data, Annotations annotations) {
        this.mediaType = mediaType;
        this.digest = digest;
        this.size = size;
        this.data = data;
        this.annotations = Map.copyOf(annotations.configAnnotations());
    }

    /**
     * Get the media type
     * @return The media type
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * Get the digest
     * @return The digest
     */
    public String getDigest() {
        return digest;
    }

    /**
     * Get the size
     * @return The size
     */
    public long getSize() {
        return size;
    }

    /**
     * Create a new config with annotations
     * @param annotations The annotations
     * @return The new config
     */
    public Config withAnnotations(Annotations annotations) {
        return new Config(mediaType, digest, size, data, annotations);
    }

    /**
     * Get the data as bytes
     * @return The data as bytes
     */
    public byte[] getDataBytes() {
        if (data != null) {
            return Base64.getDecoder().decode(data);
        }
        return "{}".getBytes();
    }

    /**
     * Get the annotations
     * @return The annotations
     */
    public Map<String, String> getAnnotations() {
        if (annotations == null) {
            return Map.of();
        }
        return annotations;
    }

    /**
     * Return the JSON representation of the manifest
     * @return The JSON string
     */
    public String toJson() {
        return JsonUtils.toJson(this);
    }

    /**
     * Create a manifest from a JSON string
     * @param json The JSON string
     * @return The manifest
     */
    public static Config fromJson(String json) {
        return JsonUtils.fromJson(json, Config.class);
    }

    /**
     * An empty config
     * @return The empty config
     */
    public static Config empty() {
        return new Config(
                Const.DEFAULT_EMPTY_MEDIA_TYPE,
                "sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a",
                2,
                "e30=",
                Annotations.empty());
    }
}
