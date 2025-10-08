/*-
 * =LICENSE=
 * ORAS Java SDK
 * ===
 * Copyright (C) 2024 - 2025 ORAS
 * ===
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =LICENSEEND=
 */

package land.oras;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import org.jspecify.annotations.Nullable;

/**
 * Index from an OCI layout
 */
@OrasModel
@JsonPropertyOrder({
    Const.JSON_PROPERTY_SCHEMA_VERSION,
    Const.JSON_PROPERTY_MEDIA_TYPE,
    Const.JSON_PROPERTY_ARTIFACT_TYPE,
    Const.JSON_PROPERTY_DIGEST,
    Const.JSON_PROPERTY_SIZE,
    Const.JSON_PROPERTY_CONFIG,
    Const.JSON_PROPERTY_SUBJECT,
    Const.JSON_PROPERTY_ANNOTATIONS,
    Const.JSON_PROPERTY_MANIFESTS,
})
@JsonInclude(JsonInclude.Include.NON_NULL) // We need to serialize empty list of manifests
public final class Index extends Descriptor implements Describable {

    private final int schemaVersion;
    private final List<ManifestDescriptor> manifests;
    private final Subject subject;

    /**
     * The manifest descriptor
     */
    private final ManifestDescriptor descriptor;

    @JsonCreator
    @SuppressWarnings("unused")
    private Index(
            @JsonProperty(Const.JSON_PROPERTY_SCHEMA_VERSION) int schemaVersion,
            @JsonProperty(Const.JSON_PROPERTY_MEDIA_TYPE) String mediaType,
            @JsonProperty(Const.JSON_PROPERTY_ARTIFACT_TYPE) String artifactType,
            @JsonProperty(Const.JSON_PROPERTY_MANIFESTS) List<ManifestDescriptor> manifests,
            @JsonProperty(Const.JSON_PROPERTY_ANNOTATIONS) Map<String, String> annotations,
            @JsonProperty(Const.JSON_PROPERTY_SUBJECT) Subject subject) {
        this(schemaVersion, mediaType, artifactType, manifests, annotations, subject, null, null);
    }

    private Index(
            int schemaVersion,
            String mediaType,
            String artifactType,
            List<ManifestDescriptor> manifests,
            Map<String, String> annotations,
            Subject subject,
            ManifestDescriptor descriptor,
            String json) {
        super(null, null, mediaType, annotations, artifactType, json);
        this.schemaVersion = schemaVersion;
        this.descriptor = descriptor;
        this.subject = subject;
        this.manifests = manifests;
    }

    /**
     * Get the schema version
     * @return The schema version
     */
    public int getSchemaVersion() {
        return schemaVersion;
    }

    /**
     * Get the list of manifests
     * @return The list of manifests
     */
    public List<ManifestDescriptor> getManifests() {
        return manifests;
    }

    /**
     * Get the artifact type as string for JSON serialization
     * @return The artifact type as string
     */
    @JsonProperty(Const.JSON_PROPERTY_ARTIFACT_TYPE)
    @SuppressWarnings("unused")
    public String getArtifactTypeAsString() {
        return artifactType;
    }

    /**
     * Return a new index with new manifest added to index
     * @param manifest The manifest
     * @return The index
     */
    public Index withNewManifests(ManifestDescriptor manifest) {
        List<ManifestDescriptor> newManifests = new LinkedList<>();
        for (ManifestDescriptor descriptor : manifests) {

            // Ignore same digest
            if (descriptor.getDigest().equals(manifest.getDigest())) {
                continue;
            }

            // Move previous ref
            if (descriptor.getAnnotations() != null
                    && descriptor.getAnnotations().containsKey(Const.ANNOTATION_REF)
                    && manifest.getAnnotations() != null
                    && manifest.getAnnotations().containsKey(Const.ANNOTATION_REF)
                    && descriptor
                            .getAnnotations()
                            .get(Const.ANNOTATION_REF)
                            .equals(manifest.getAnnotations().get(Const.ANNOTATION_REF))) {
                Map<String, String> newAnnotations = new LinkedHashMap<>(descriptor.getAnnotations());
                newAnnotations.remove(Const.ANNOTATION_REF);
                if (newAnnotations.isEmpty()) {
                    newAnnotations = null;
                }
                newManifests.add(ManifestDescriptor.fromJson(
                        descriptor.withAnnotations(newAnnotations).toJson()));
                continue;
            }
            newManifests.add(ManifestDescriptor.fromJson(descriptor.toJson()));
        }
        newManifests.add(manifest);
        return new Index(schemaVersion, mediaType, artifactType, newManifests, annotations, subject, descriptor, json);
    }

    @Override
    public ManifestDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Get the annotations
     * @return The annotations
     */
    @Override
    public @Nullable Map<String, String> getAnnotations() {
        if (annotations != null && !annotations.isEmpty()) {
            return annotations;
        }
        return null;
    }

    /**
     * Return a new index with the given descriptor
     * @param descriptor The descriptor
     * @return The manifest
     */
    public Index withDescriptor(ManifestDescriptor descriptor) {
        return new Index(schemaVersion, mediaType, artifactType, manifests, annotations, subject, descriptor, json);
    }

    /**
     * Return same instance but with original JSON
     * @param json The original JSON
     * @return The index
     */
    protected Index withJson(String json) {
        this.json = json;
        return this;
    }

    @Override
    public Subject getSubject() {
        return subject;
    }

    /**
     * Return a new index with the given subject
     * @param subject The subject
     * @return The index
     */
    public Index withSubject(Subject subject) {
        return new Index(schemaVersion, mediaType, artifactType, manifests, annotations, subject, descriptor, json);
    }

    /**
     * Create an index from a JSON string
     * @param json The JSON string
     * @return The index
     */
    public static Index fromJson(String json) {
        return JsonUtils.fromJson(json, Index.class).withJson(json);
    }

    /**
     * Create an index from a path
     * @param path The path
     * @return The index
     */
    public static Index fromPath(Path path) {
        return JsonUtils.fromJson(path, Index.class);
    }

    /**
     * Create an index from a list of manifests
     * @param descriptors The list of manifests
     * @return The index
     */
    public static Index fromManifests(List<ManifestDescriptor> descriptors) {
        return new Index(2, Const.DEFAULT_INDEX_MEDIA_TYPE, null, descriptors, null, null, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Index index = (Index) o;
        return Objects.equals(toJson(), index.toJson());
    }

    @Override
    public int hashCode() {
        return Objects.hash(toJson());
    }

    @Override
    public String toString() {
        return toJson();
    }
}
