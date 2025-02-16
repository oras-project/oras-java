package land.oras;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import land.oras.utils.DigestUtils;
import land.oras.utils.JsonUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Class for layer
 */
@NullMarked
public final class Layer {

    /**
     * The media type of the layer
     */
    private final String mediaType;

    /**
     * The digest of the layer
     */
    private final String digest;

    /**
     * The size of the layer
     */
    private final long size;

    /**
     * The base 64 encoded data. Might be null if path is set
     */
    private final @Nullable String data;

    /**
     * The path to the blob. Might be null if data is set
     */
    private final transient @Nullable Path blobPath;

    /**
     * Annotations for the layer
     */
    private final @Nullable Map<String, String> annotations;

    /**
     * Constructor that can directly set the data
     * Not adapted for large blob due to memory usage but convenient for small data
     * @param mediaType The media type
     * @param digest The digest
     * @param size The size
     */
    private Layer(
            String mediaType,
            String digest,
            long size,
            @Nullable String data,
            @Nullable Map<String, String> annotations) {
        this.mediaType = mediaType;
        this.digest = digest;
        this.size = size;
        this.data = data;
        this.blobPath = null;
        this.annotations = annotations;
    }

    /**
     * Constructor that set the data from a file
     * @param mediaType The media type
     * @param digest The digest
     * @param size The size
     * @param blobPath The path to the blob
     */
    private Layer(String mediaType, String digest, long size, Path blobPath, Map<String, String> annotations) {
        this.mediaType = mediaType;
        this.digest = digest;
        this.size = size;
        this.data = null;
        this.blobPath = blobPath;
        this.annotations = annotations;
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
     * Get the data
     * @return The data
     */
    public @Nullable String getData() {
        return data;
    }

    /**
     * Get the blob path
     * @return The blob path
     */
    public @Nullable Path getBlobPath() {
        return blobPath;
    }

    /**
     * Get the annotations
     * @return The annotations
     */
    public Map<String, String> getAnnotations() {
        if (annotations == null) {
            return Map.of();
        }
        return Collections.unmodifiableMap(annotations);
    }

    /**
     * Create a new layer with annotations
     * @param annotations The annotations
     * @return The new layer
     */
    public Layer withAnnotations(Map<String, String> annotations) {
        return new Layer(mediaType, digest, size, data, annotations);
    }

    /**
     * Create a new layer with media type
     * @param mediaType The media type
     * @return The new layer
     */
    public Layer withMediaType(String mediaType) {
        return new Layer(mediaType, digest, size, data, annotations);
    }

    /**
     * Get the data as bytes
     * @return The data as bytes
     */
    public byte[] getDataBytes() {
        if (data != null) {
            return Base64.getDecoder().decode(data);
        } else if (blobPath != null) {
            try {
                return Files.readAllBytes(blobPath);
            } catch (IOException e) {
                throw new OrasException("Failed to read layer data", e);
            }
        }
        throw new OrasException("No data or blob path set");
    }

    /**
     * Return the JSON representation of the manifest
     * @return The JSON string
     */
    public String toJson() {
        return JsonUtils.toJson(this);
    }

    /**
     * Create a layer from a JSON string
     * @param json The JSON string
     * @return The manifest
     */
    public static Layer fromJson(String json) {
        return JsonUtils.fromJson(json, Layer.class);
    }

    /**
     * Create a layer from a file
     * @param file The file
     * @return The layer
     */
    public static Layer fromFile(Path file) {
        Map<String, String> annotations =
                Map.of(Const.ANNOTATION_TITLE, file.getFileName().toString());
        String mediaType = getContentType(file);
        return new Layer(mediaType, DigestUtils.sha256(file), file.toFile().length(), file, annotations);
    }

    /**
     * Create a layer from data
     * @param data The data
     * @return The layer
     */
    public static Layer fromData(byte[] data) {
        return new Layer(
                Const.DEFAULT_BLOB_MEDIA_TYPE,
                DigestUtils.sha256(data),
                data.length,
                Base64.getEncoder().encodeToString(data),
                Map.of());
    }

    /**
     * Get the content-type file or the default content type
     * @param path The path
     * @return The content type
     */
    private static String getContentType(Path path) {
        try {
            return Objects.requireNonNullElse(Files.probeContentType(path), Const.DEFAULT_DESCRIPTOR_MEDIA_TYPE);
        } catch (Exception e) {
            return Const.DEFAULT_DESCRIPTOR_MEDIA_TYPE;
        }
    }

    /**
     * Create a layer from a digest
     * @param digest The digest
     * @param size The size
     * @return The layer
     */
    public static Layer fromDigest(String digest, long size) {
        return new Layer(Const.DEFAULT_EMPTY_MEDIA_TYPE, digest, size, (String) null, Map.of());
    }

    /**
     * An empty config
     * @return The empty config
     */
    public static Layer empty() {
        return new Layer(
                Const.DEFAULT_EMPTY_MEDIA_TYPE,
                "sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a",
                2,
                "e30=",
                Map.of());
    }
}
