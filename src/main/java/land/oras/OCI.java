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

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for OCI operation on remote registry or layout
 * Commons methods for OCI operations
 * @param <T> The reference type
 */
public abstract sealed class OCI<T extends Ref> permits Registry, OCILayout {

    /**
     * The logger
     */
    protected static final Logger LOG = LoggerFactory.getLogger(OCI.class);

    /**
     * Default constructor
     */
    protected OCI() {}

    /**
     * Push an artifact
     * @param ref The ref
     * @param paths The paths
     * @return The manifest
     */
    public Manifest pushArtifact(T ref, LocalPath... paths) {
        return pushArtifact(ref, ArtifactType.unknown(), Annotations.empty(), Config.empty(), paths);
    }

    /**
     * Push an artifact
     * @param ref The ref
     * @param artifactType The artifact type
     * @param paths The paths
     * @return The manifest
     */
    public Manifest pushArtifact(T ref, ArtifactType artifactType, LocalPath... paths) {
        return pushArtifact(ref, artifactType, Annotations.empty(), Config.empty(), paths);
    }

    /**
     * Upload an ORAS artifact
     * @param ref The ref
     * @param artifactType The artifact type
     * @param annotations The annotations
     * @param paths The paths
     * @return The manifest
     */
    public Manifest pushArtifact(T ref, ArtifactType artifactType, Annotations annotations, LocalPath... paths) {
        return pushArtifact(ref, artifactType, annotations, Config.empty(), paths);
    }

    /**
     * Push a blob from file
     * @param ref The ref
     * @param blob The blob
     * @return The layer
     */
    public Layer pushBlob(T ref, Path blob) {
        return pushBlob(ref, blob, Map.of());
    }

    /**
     * Push config
     * @param ref The ref
     * @param config The config
     * @return The config
     */
    public Config pushConfig(T ref, Config config) {
        Layer layer = pushBlob(ref, config.getDataBytes());
        LOG.debug("Config pushed: {}", layer.getDigest());
        return config;
    }

    /**
     * Push an artifact
     * @param ref The container
     * @param artifactType The artifact type. Can be null
     * @param annotations The annotations
     * @param config The config
     * @param paths The paths
     * @return The manifest
     */
    public abstract Manifest pushArtifact(
            T ref, ArtifactType artifactType, Annotations annotations, @Nullable Config config, LocalPath... paths);

    /**
     * Pull an artifact
     * @param ref The reference of the artifact
     * @param path The path to save the artifact
     * @param overwrite Overwrite the artifact if it exists
     */
    public abstract void pullArtifact(T ref, Path path, boolean overwrite);

    /**
     * Get the blob for the given digest. Not be suitable for large blobs
     * @param ref The ref
     * @return The blob as bytes
     */
    public abstract byte[] getBlob(T ref);

    /**
     * Fetch blob and save it to file
     * @param ref The ref
     * @param path The path to save the blob
     */
    public abstract void fetchBlob(T ref, Path path);

    /**
     * Fetch blob and return it as input stream
     * @param ref The ref
     * @return The input stream
     */
    public abstract InputStream fetchBlob(T ref);

    /**
     * Push a blob from file
     * @param ref The container
     * @param blob The blob
     * @param annotations The annotations
     * @return The layer
     */
    public abstract Layer pushBlob(T ref, Path blob, Map<String, String> annotations);

    /**
     * Push the blob for the given layer
     * @param ref The container ref
     * @param data The data
     * @return The layer
     */
    public abstract Layer pushBlob(T ref, byte[] data);
}
