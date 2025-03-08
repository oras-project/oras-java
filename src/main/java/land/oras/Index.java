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

import java.nio.file.Path;
import java.util.List;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;

/**
 * Index from an OCI layout
 */
public class Index {

    private final int schemaVersion;
    private final String mediaType;
    private final String artifactType;
    private final List<ManifestDescriptor> manifests;

    /**
     * Original json
     */
    private transient String json;

    /**
     * The manifest descriptor
     */
    private final transient ManifestDescriptor descriptor;

    private Index(
            int schemaVersion,
            String mediaType,
            String artifactType,
            List<ManifestDescriptor> manifests,
            ManifestDescriptor descriptor,
            String json) {
        this.schemaVersion = schemaVersion;
        this.mediaType = mediaType;
        this.descriptor = descriptor;
        this.artifactType = artifactType;
        this.manifests = manifests;
        this.json = json;
    }

    /**
     * Get the media type
     * @return The media type
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * Get the schema version
     * @return The schema version
     */
    public int getSchemaVersion() {
        return schemaVersion;
    }

    /**
     * Get the artifact type
     * @return The artifact type
     */
    public String getArtifactType() {
        return artifactType;
    }

    /**
     * Get the list of manifests
     * @return The list of manifests
     */
    public List<ManifestDescriptor> getManifests() {
        return manifests;
    }

    /**
     * Get the descriptor
     * @return The descriptor
     */
    public ManifestDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Return a new index with the given descriptor
     * @param descriptor The descriptor
     * @return The manifest
     */
    public Index withDescriptor(ManifestDescriptor descriptor) {
        return new Index(schemaVersion, mediaType, artifactType, manifests, descriptor, json);
    }

    /**
     * Return same instance but with original JSON
     * @param json The original JSON
     * @return The index
     */
    private Index withJson(String json) {
        this.json = json;
        return this;
    }

    /**
     * Return the JSON representation of the index
     * @return The JSON string
     */
    public String toJson() {
        return JsonUtils.toJson(this);
    }

    /**
     * Return the original JSON
     * @return The original JSON
     */
    public String getJson() {
        return json;
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
        return new Index(2, Const.DEFAULT_INDEX_MEDIA_TYPE, null, descriptors, null, null);
    }
}
