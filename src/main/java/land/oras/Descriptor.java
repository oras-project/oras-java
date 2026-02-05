/*-
 * =LICENSE=
 * ORAS Java SDK
 * ===
 * Copyright (C) 2024 - 2026 ORAS
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import org.jspecify.annotations.Nullable;

/**
 * Main class for descriptor
 */
@OrasModel
@JsonPropertyOrder({Const.JSON_PROPERTY_MEDIA_TYPE, Const.JSON_PROPERTY_DIGEST, Const.JSON_PROPERTY_SIZE})
public sealed class Descriptor permits Config, Manifest, Layer, Index {

    /**
     * The media type of the layer
     */
    protected final String mediaType;

    /**
     * Annotations for the layer
     */
    protected final @Nullable Map<String, String> annotations;

    /**
     * The digest of the layer
     */
    protected final @Nullable String digest;

    /**
     * The size of the layer
     */
    protected final @Nullable Long size;

    /**
     * The artifact type
     */
    protected final @Nullable String artifactType;

    /**
     * Original json
     */
    protected String json;

    /**
     * Constructor
     * @param digest The digest
     * @param size The size
     * @param mediaType The media type
     * @param annotations The annotations
     * @param artifactType The artifact type
     * @param json The original JSON
     */
    protected Descriptor(
            String digest,
            Long size,
            String mediaType,
            Map<String, String> annotations,
            String artifactType,
            String json) {
        this.digest = digest;
        this.size = size;
        this.mediaType = mediaType;
        this.annotations = annotations;
        this.artifactType = artifactType;
        this.json = json;
    }

    /**
     * Return the original JSON
     * @return The original JSON
     */
    @JsonIgnore
    public String getJson() {
        return json;
    }

    /**
     * Get the annotations
     * @return The annotations
     */
    @JsonIgnore
    public Map<String, String> getAnnotations() {
        if (annotations == null) {
            return Map.of();
        }
        return Collections.unmodifiableMap(annotations);
    }

    /**
     * We never serialize empty annotations
     * @return The annotations or null
     */
    @JsonProperty(Const.JSON_PROPERTY_ANNOTATIONS)
    @SuppressWarnings("unused")
    public @Nullable Map<String, String> getAnnotationsForJson() {
        if (annotations == null || annotations.isEmpty()) {
            return null;
        }
        return annotations;
    }

    /**
     * Get the media type
     * @return The media type
     */
    public final String getMediaType() {
        return mediaType;
    }

    /**
     * Get the digest
     * @return The digest
     */
    public @Nullable String getDigest() {
        return digest;
    }

    /**
     * Get the size
     * @return The size
     */
    public @Nullable Long getSize() {
        return size;
    }

    /**
     * Get the artifact type
     * @return The artifact type
     */
    public @Nullable ArtifactType getArtifactType() {
        if (artifactType != null) {
            return ArtifactType.from(artifactType);
        }
        return null;
    }

    /**
     * Return the JSON representation of this descriptor
     * @return The JSON string
     */
    public final String toJson() {
        return JsonUtils.toJson(this);
    }

    /**
     * Return same instance but with original JSON
     * @param json The original JSON
     * @return The index
     */
    protected Descriptor withJson(String json) {
        this.json = json;
        return this;
    }

    /**
     * Return this manifest descriptor as a subject
     * @return The subject
     */
    public Subject toSubject() {
        Objects.requireNonNull(digest);
        Objects.requireNonNull(size);
        return Subject.of(mediaType, digest, size);
    }

    /**
     * Create a new descriptor
     * @param digest The digest
     * @param size The size
     * @param mediaType The media type
     * @param annotations The annotations
     * @param artifactType The artifact type
     * @return The descriptor
     */
    public static Descriptor of(
            String digest, Long size, String mediaType, Map<String, String> annotations, String artifactType) {
        return new Descriptor(digest, size, mediaType, annotations, artifactType, null);
    }

    /**
     * Create a new descriptor
     * @param digest The digest
     * @param size The size
     * @param mediaType The media type
     * @return The descriptor
     */
    public static Descriptor of(String digest, Long size, String mediaType) {
        return new Descriptor(digest, size, mediaType, null, null, null);
    }

    /**
     * Create a new descriptor
     * @param digest The digest
     * @param size The size
     * @return The descriptor
     */
    public static Descriptor of(String digest, Long size) {
        return new Descriptor(digest, size, Const.DEFAULT_DESCRIPTOR_MEDIA_TYPE, null, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Descriptor that = (Descriptor) o;
        return Objects.equals(toJson(), that.toJson());
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
