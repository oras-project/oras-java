package land.oras;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import land.oras.utils.JsonUtils;
import org.jspecify.annotations.NullMarked;

/**
 * Record for annotations
 *
 * @param manifestAnnotations Annotations for the manifest
 * @param configAnnotations   Annotations for the config
 * @param filesAnnotations    Annotations for the layers/files
 */
@NullMarked
public record Annotations(
        Map<String, String> configAnnotations,
        Map<String, String> manifestAnnotations,
        Map<String, Map<String, String>> filesAnnotations) {

    /**
     * Create a new annotations record with only manifest annotations
     * @param manifestAnnotations The manifest annotations
     * @return The annotations
     */
    public static Annotations ofManifest(Map<String, String> manifestAnnotations) {
        return new Annotations(new HashMap<>(), manifestAnnotations, new HashMap<>());
    }

    /**
     * Create a new annotations record with only config annotations
     * @param configAnnotations The config annotations
     * @return The annotations
     */
    public static Annotations ofConfig(Map<String, String> configAnnotations) {
        return new Annotations(configAnnotations, new HashMap<>(), new HashMap<>());
    }

    /**
     * Empty annotations
     * @return The empty annotations
     */
    public static Annotations empty() {
        return new Annotations(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    /**
     * Get the manifest annotations for a file
     * @param key The key
     * @return The annotations
     */
    public Map<String, String> getFileAnnotations(String key) {
        return this.filesAnnotations().getOrDefault(key, new HashMap<>());
    }

    /**
     * Annotations file format
     */
    private static class AnnotationFile extends HashMap<String, Map<String, String>> {
        /**
         * Get the manifest annotations
         *
         * @return The manifest annotations
         */
        public Map<String, String> getManifestAnnotations() {
            return this.getOrDefault("$manifest", new HashMap<>());
        }

        /**
         * Get the config annotations
         *
         * @return The config annotations
         */
        public Map<String, String> getConfigAnnotations() {
            return this.getOrDefault("$config", new HashMap<>());
        }

        /**
         * Get the files annotations without the manifest and config annotations
         *
         * @return The files annotations
         */
        public Map<String, Map<String, String>> getFilesAnnotations() {
            return this.entrySet().stream()
                    .filter(entry -> !"$manifest".equals(entry.getKey())
                            && !"$config".equals(entry.getKey())) // Filter out $manifest and $config
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        /**
         * Get the annotations for a file
         * @param key The key
         * @return The annotations
         */
        public Map<String, String> getFileAnnotations(String key) {
            return this.getOrDefault(key, new HashMap<>());
        }
    }

    /**
     * Convert the annotations from a JSON string
     *
     * @param json The JSON string
     * @return The annotations
     */
    public static Annotations fromJson(String json) {
        AnnotationFile file = JsonUtils.fromJson(json, AnnotationFile.class);
        return new Annotations(file.getConfigAnnotations(), file.getManifestAnnotations(), file.getFilesAnnotations());
    }

    /**
     * Convert the annotations to a JSON string
     *
     * @return The JSON string
     */
    public String toJson() {
        AnnotationFile file = new AnnotationFile();
        file.put("$manifest", manifestAnnotations());
        file.put("$config", configAnnotations());
        file.putAll(filesAnnotations());
        return JsonUtils.toJson(file);
    }
}
