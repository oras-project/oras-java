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

import java.util.Map;
import java.util.Objects;
import land.oras.utils.JsonUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Manifest descriptor
 */
@NullMarked
@OrasModel
public final class ManifestDescriptor {

    private final @Nullable String artifactType;
    private final String mediaType;
    private final String digest;
    private final long size;

    @Nullable
    private final Map<String, String> platform;

    @Nullable
    private final Map<String, String> annotations;

    /**
     * Constructor
     * @param artifactType The optional artifact type
     * @param mediaType The media type
     * @param digest The digest
     * @param size The size
     */
    private ManifestDescriptor(
            @Nullable String artifactType,
            String mediaType,
            String digest,
            long size,
            @Nullable Map<String, String> platform,
            @Nullable Map<String, String> annotations) {
        this.artifactType = artifactType;
        this.mediaType = mediaType;
        this.digest = digest;
        this.size = size;
        this.platform = platform;
        this.annotations = annotations;
    }

    /**
     * Get the artifact type
     * @return The artifact type
     */
    public @Nullable String getArtifactType() {
        return artifactType;
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
     * Get the platform
     * @return The platform
     */
    public Map<String, String> getPlatform() {
        return platform;
    }

    /**
     * Get the annotations
     * @return The annotations
     */
    public @Nullable Map<String, String> getAnnotations() {
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
     * Create a manifest descriptor from a JSON string
     * @param json The JSON string
     * @return The manifest
     */
    public static ManifestDescriptor fromJson(String json) {
        return JsonUtils.fromJson(json, ManifestDescriptor.class);
    }

    /**
     * Return this manifest descriptor as a descriptor
     * @return The descriptor
     */
    public Descriptor toDescriptor() {
        return Descriptor.of(digest, size, mediaType, annotations, artifactType);
    }

    /**
     * Return this manifest descriptor as a subject
     * @return The subject
     */
    public Subject toSubject() {
        return Subject.of(mediaType, digest, size);
    }

    /**
     * Create a manifest descriptor with the given annotations
     * @param annotations The annotations
     * @return The subject
     */
    public ManifestDescriptor withAnnotations(@Nullable Map<String, String> annotations) {
        return new ManifestDescriptor(artifactType, mediaType, digest, size, platform, annotations);
    }

    /**
     * Create a manifest descriptor with the given artifact type
     * @param artifactType The artifact type
     * @return The subject
     */
    public ManifestDescriptor withArtifactType(@Nullable String artifactType) {
        return new ManifestDescriptor(artifactType, mediaType, digest, size, platform, annotations);
    }

    /**
     * Create a manifest descriptor
     * @param mediaType The media type
     * @param digest The digest
     * @param size The size
     * @return The subject
     */
    public static ManifestDescriptor of(String mediaType, String digest, long size) {
        return new ManifestDescriptor(null, mediaType, digest, size, null, null);
    }

    /**
     * Create a manifest descriptor
     * @param descriptor The descriptor
     * @return The subject
     */
    public static ManifestDescriptor of(Descriptor descriptor) {
        Objects.requireNonNull(descriptor.getDigest());
        Objects.requireNonNull(descriptor.getSize());
        return new ManifestDescriptor(
                descriptor.getArtifactType() != null
                        ? descriptor.getArtifactType().getMediaType()
                        : null,
                descriptor.getMediaType(),
                descriptor.getDigest(),
                descriptor.getSize(),
                null,
                descriptor.getAnnotations());
    }
}
