package land.oras;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import org.jspecify.annotations.NullUnmarked;

/**
 * Class for manifest
 */
@NullUnmarked
public final class Manifest {

    private final int schemaVersion;
    private final String mediaType;
    private final String artifactType;
    private final Config config;
    private final List<Layer> layers;
    private final Map<String, String> annotations;

    /**
     * Constructor
     * @param schemaVersion The schema version
     * @param mediaType The media type
     * @param config The config
     * @param layers The layers
     * @param annotations The annotations
     */
    private Manifest(
            int schemaVersion,
            String mediaType,
            String artifactType,
            Config config,
            List<Layer> layers,
            Annotations annotations) {
        this.schemaVersion = schemaVersion;
        this.mediaType = mediaType;
        this.artifactType = artifactType;
        this.config = config;
        this.layers = layers;
        this.annotations = Map.copyOf(annotations.manifestAnnotations());
    }

    /**
     * Get the schema version
     * @return The schema version
     */
    public int getSchemaVersion() {
        return schemaVersion;
    }

    /**
     * Get the media type
     * @return The media type
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * Get the artifact type
     * @return The artifact type
     */
    public String getArtifactType() {
        return artifactType;
    }

    /**
     * Get the config
     * @return The config
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Get the layers
     * @return The layers
     */
    public List<Layer> getLayers() {
        return Collections.unmodifiableList(layers);
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
     * Return a new manifest with the given artifact type
     * @param artifactType The artifact type
     * @return The manifest
     */
    public Manifest withArtifactType(String artifactType) {
        return new Manifest(
                schemaVersion, mediaType, artifactType, config, layers, Annotations.ofManifest(annotations));
    }

    /**
     * Return a new manifest with the given layers
     * @param layers The layers
     * @return The manifest
     */
    public Manifest withLayers(List<Layer> layers) {
        return new Manifest(
                schemaVersion, mediaType, artifactType, config, layers, Annotations.ofManifest(annotations));
    }

    /**
     * Return a new manifest with the given config
     * @param config The config
     * @return The manifest
     */
    public Manifest withConfig(Config config) {
        return new Manifest(
                schemaVersion, mediaType, artifactType, config, layers, Annotations.ofManifest(annotations));
    }

    /**
     * Return a new manifest with the given annotations
     * @param annotations The annotations
     * @return The manifest
     */
    public Manifest withAnnotations(Map<String, String> annotations) {
        return new Manifest(
                schemaVersion, mediaType, artifactType, config, layers, Annotations.ofManifest(annotations));
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
    public static Manifest fromJson(String json) {
        return JsonUtils.fromJson(json, Manifest.class);
    }

    /**
     * Return a copy of an empty manifest
     * @return The empty manifest
     */
    public static Manifest empty() {
        return new Manifest(2, Const.DEFAULT_MANIFEST_MEDIA_TYPE, null, Config.empty(), List.of(), Annotations.empty());
    }
}
