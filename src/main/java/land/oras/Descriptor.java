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

import java.util.Collections;
import java.util.Map;
import land.oras.utils.JsonUtils;
import org.jspecify.annotations.Nullable;

/**
 * Abstract class for descriptor
 */
public abstract sealed class Descriptor permits Config, Manifest, Layer, Index {

    /**
     * The media type of the layer
     */
    protected final String mediaType;

    /**
     * Annotations for the layer
     */
    protected final @Nullable Map<String, String> annotations;

    protected final @Nullable String digest;
    protected final @Nullable Long size;
    protected final @Nullable String artifactType;

    protected Descriptor(
            String digest, Long size, String mediaType, Map<String, String> annotations, String artifactType) {
        this.digest = digest;
        this.size = size;
        this.mediaType = mediaType;
        this.annotations = annotations;
        this.artifactType = artifactType;
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
}
