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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Map;
import java.util.Objects;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import land.oras.utils.SupportedAlgorithm;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Manifest descriptor
 */
@NullMarked
@OrasModel
@JsonPropertyOrder({
    Const.JSON_PROPERTY_MEDIA_TYPE,
    Const.JSON_PROPERTY_ARTIFACT_TYPE,
    Const.JSON_PROPERTY_DIGEST,
    Const.JSON_PROPERTY_SIZE,
    Const.JSON_PROPERTY_PLATFORM,
    Const.JSON_PROPERTY_ANNOTATIONS,
})
public final class ManifestDescriptor {

    private final @Nullable String artifactType;
    private final String mediaType;
    private final String digest;
    private final long size;

    @Nullable
    private final Platform platform;

    @Nullable
    private final Map<String, String> annotations;

    @JsonCreator
    private ManifestDescriptor(
            @JsonProperty(Const.JSON_PROPERTY_ARTIFACT_TYPE) @Nullable String artifactType,
            @JsonProperty(Const.JSON_PROPERTY_MEDIA_TYPE) String mediaType,
            @JsonProperty(Const.JSON_PROPERTY_DIGEST) String digest,
            @JsonProperty(Const.JSON_PROPERTY_SIZE) long size,
            @JsonProperty(Const.JSON_PROPERTY_PLATFORM) @Nullable Platform platform,
            @JsonProperty(Const.JSON_PROPERTY_ANNOTATIONS) @Nullable Map<String, String> annotations) {
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
     * Return the platform as a Platform object
     * @return The platform
     */
    @JsonIgnore
    public Platform getPlatform() {
        return platform != null ? platform : Platform.empty();
    }

    /**
     * Return the platform or null if the platform is not set
     * Only use for serialization purposes, as the platform field is nullable in the JSON representation of the manifest descriptor
     * @return The platform or null
     */
    @JsonProperty(Const.JSON_PROPERTY_PLATFORM)
    public @Nullable Platform getPlatformOrNull() {
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
     * Create a manifest descriptor with the given platform
     * @param platform The platform
     * @return The subject
     */
    public ManifestDescriptor withPlatform(Platform platform) {
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
        return of(descriptor, descriptor.getDigest());
    }

    /**
     * Utility method. Useful when assembly manifest to be added to an Index using no platform, empty annotations and default supported algorithm
     * @param manifest The manifest
     * @return The manifest descriptor
     */
    public static ManifestDescriptor of(Manifest manifest) {
        return of(manifest, Platform.empty(), Annotations.empty(), SupportedAlgorithm.getDefault());
    }

    /**
     * Utility method. Useful when assembly manifest to be added to an Index
     * @param manifest The manifest
     * @param platform The platform
     * @param annotations The annotations
     * @param supportedAlgorithm The supported algorithm to calculate the digest of the manifest
     * @return The manifest descriptor
     */
    public static ManifestDescriptor of(
            Manifest manifest, Platform platform, Annotations annotations, SupportedAlgorithm supportedAlgorithm) {
        String json = manifest.toJson();
        String digest = supportedAlgorithm.digest(json.getBytes());
        long size = json.length();
        return ManifestDescriptor.of(manifest.getMediaType(), digest, size)
                .withAnnotations(annotations.manifestAnnotations())
                .withPlatform(platform)
                .withArtifactType(manifest.getArtifactTypeAsString());
    }

    /**
     * Create a manifest descriptor with the given digest
     * @param descriptor The descriptor
     * @param digest The digest
     * @return The subject
     */
    public static ManifestDescriptor of(Descriptor descriptor, @Nullable String digest) {
        Objects.requireNonNull(descriptor.getSize());
        Objects.requireNonNull(digest);
        return new ManifestDescriptor(
                descriptor.getArtifactType() != null
                        ? descriptor.getArtifactType().getMediaType()
                        : null,
                descriptor.getMediaType(),
                digest,
                descriptor.getSize(),
                null,
                descriptor.getAnnotations());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ManifestDescriptor that = (ManifestDescriptor) o;
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
