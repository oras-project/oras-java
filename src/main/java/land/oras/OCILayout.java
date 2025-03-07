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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import land.oras.utils.SupportedAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Index from an OCI layout
 */
public final class OCILayout extends OCI {

    /**
     * The logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(OCILayout.class);

    private final String imageLayoutVersion = "1.0.0";

    /**
     * Path on the file system of the OCI Layout
     */
    private transient Path path;

    /**
     * Private constructor
     */
    private OCILayout() {}

    private void setPath(Path path) {
        this.path = path;
    }

    /**
     * Return the JSON representation of the referrers
     * @return The JSON string
     */
    public String toJson() {
        return JsonUtils.toJson(this);
    }

    /**
     * Create the OCI layout file from a JSON string
     * @param json The JSON string
     * @return The OCI layout
     */
    public static OCILayout fromJson(String json) {
        return JsonUtils.fromJson(json, OCILayout.class);
    }

    /**
     * Return the image layout version
     * @return The image layout version
     */
    public String getImageLayoutVersion() {
        return imageLayoutVersion;
    }

    /**
     * Copy the container ref from registry into oci-layout
     * @param registry The registry
     * @param containerRef The container
     */
    public void copy(Registry registry, ContainerRef containerRef) {

        try {

            // Create blobs directory if needed
            Files.createDirectories(getBlobPath());

            // Write oci layout JSON
            Files.writeString(getOciLayoutPath(), toJson());

            Map<String, String> headers = registry.getHeaders(containerRef);
            String contentType = headers.get(Const.CONTENT_TYPE_HEADER.toLowerCase());
            if (contentType == null) {
                throw new OrasException("Content type not found in headers");
            }
            String manifestDigest = headers.get(Const.DOCKER_CONTENT_DIGEST_HEADER.toLowerCase());
            if (manifestDigest == null) {
                throw new OrasException("Manifest digest not found in headers");
            }

            LOG.debug("Content type: {}", contentType);
            LOG.debug("Manifest digest: {}", manifestDigest);

            // Single manifest
            if (registry.isManifestMediaType(contentType)) {

                // Write manifest as any blob
                Manifest manifest = registry.getManifest(containerRef);
                writeManifest(manifest);

                // Write the index.json containing this manifest
                Index index = Index.fromManifests(List.of(manifest.getDescriptor()));
                writeIndex(index);

                // Write config as any blob
                writeConfig(registry, containerRef, manifest.getConfig());
            }
            // Index
            else if (registry.isIndexMediaType(contentType)) {

                Index index = registry.getIndex(containerRef);

                // Write all manifests and their config
                for (ManifestDescriptor descriptor : index.getManifests()) {
                    Manifest manifest = registry.getManifest(containerRef.withDigest(descriptor.getDigest()));
                    writeManifest(manifest.withDescriptor(descriptor));
                    writeConfig(registry, containerRef, manifest.getConfig());
                }

                // Write the index
                writeIndex(index);

            } else {
                throw new OrasException("Unsupported content type: %s".formatted(contentType));
            }

            // Write all layer
            for (Layer layer : registry.collectLayers(containerRef, contentType, true)) {
                try (InputStream is = registry.fetchBlob(containerRef.withDigest(layer.getDigest()))) {

                    Path prefixDirectory = getBlobAlgorithmPath(layer.getDigest());
                    if (!Files.exists(prefixDirectory)) {
                        Files.createDirectory(prefixDirectory);
                    }

                    Path blobFile = getBlobPath(layer);

                    // Skip if already exists
                    if (Files.exists(blobFile)) {
                        LOG.debug("Blob already exists: {}", blobFile);
                        continue;
                    }
                    Files.copy(is, blobFile);
                    LOG.debug("Copied blob to {}", blobFile);
                }
            }
        } catch (IOException e) {
            throw new OrasException("Failed to copy container", e);
        }
    }

    private Path getOciLayoutPath() {
        return path.resolve(Const.OCI_LAYOUT_FILE);
    }

    private Path getBlobPath() {
        return path.resolve(Const.OCI_LAYOUT_BLOBS);
    }

    private Path getBlobPath(ManifestDescriptor manifestDescriptor) {
        String digest = manifestDescriptor.getDigest();
        SupportedAlgorithm algorithm = SupportedAlgorithm.fromDigest(digest);
        return getBlobPath().resolve(algorithm.getPrefix()).resolve(SupportedAlgorithm.getDigest(digest));
    }

    private Path getBlobPath(Config config) {
        String digest = config.getDigest();
        SupportedAlgorithm algorithm = SupportedAlgorithm.fromDigest(digest);
        return getBlobPath().resolve(algorithm.getPrefix()).resolve(SupportedAlgorithm.getDigest(digest));
    }

    private Path getBlobPath(Layer layer) {
        String digest = layer.getDigest();
        SupportedAlgorithm algorithm = SupportedAlgorithm.fromDigest(digest);
        return getBlobPath().resolve(algorithm.getPrefix()).resolve(SupportedAlgorithm.getDigest(digest));
    }

    private Path getIndexPath() {
        return path.resolve(Const.OCI_LAYOUT_INDEX);
    }

    private Path getIndexBlobPath(Index index) {
        String digest = index.getDescriptor().getDigest();
        return getBlobAlgorithmPath(digest).resolve(SupportedAlgorithm.getDigest(digest));
    }

    private Path getBlobAlgorithmPath(String digest) {
        SupportedAlgorithm algorithm = SupportedAlgorithm.fromDigest(digest);
        return getBlobPath().resolve(algorithm.getPrefix());
    }

    private void writeIndex(Index index) throws IOException {
        Path indexFile = getIndexPath();
        Files.writeString(indexFile, index.getJson() != null ? index.getJson() : index.toJson());
        if (index.getJson() != null) {
            Files.writeString(getIndexBlobPath(index), index.getJson());
        }
    }

    private void writeManifest(Manifest manifest) throws IOException {
        ManifestDescriptor descriptor = manifest.getDescriptor();
        Path manifestFile = getBlobPath(descriptor);
        Path manifestPrefixDirectory =
                getBlobAlgorithmPath(manifest.getDescriptor().getDigest());

        if (!Files.exists(manifestPrefixDirectory)) {
            Files.createDirectory(manifestPrefixDirectory);
        }
        // Skip if already exists
        if (Files.exists(manifestFile)) {
            LOG.debug("Manifest already exists: {}", manifestFile);
            return;
        }
        if (manifest.getJson() == null) {
            LOG.debug("Writing new manifest: {}", manifestFile);
            Files.writeString(manifestFile, manifest.toJson());
        } else {
            LOG.debug("Writing existing manifest: {}", manifestFile);
            Files.writeString(manifestFile, manifest.getJson());
        }
    }

    private void writeConfig(Registry registry, ContainerRef containerRef, Config config) throws IOException {
        String configDigest = config.getDigest();
        Path configFile = getBlobPath(config);

        Path configPrefixDirectory = getBlobAlgorithmPath(configDigest);
        if (!Files.exists(configPrefixDirectory)) {
            Files.createDirectory(configPrefixDirectory);
        }
        // Skip if already exists
        if (Files.exists(configFile)) {
            LOG.debug("Config already exists: {}", configFile);
            return;
        }
        // Write the data from data or fetch the blob
        if (config.getData() != null) {
            Files.write(configFile, config.getDataBytes());
        } else {
            try (InputStream is = registry.fetchBlob(containerRef.withDigest(configDigest))) {
                Files.copy(is, configFile);
            }
        }
    }

    /**
     * Builder for the registry
     */
    public static class Builder {

        private final OCILayout layout = new OCILayout();

        /**
         * Hidden constructor
         */
        private Builder() {
            // Hide constructor
        }

        /**
         * Return a new builder with default path
         * @param path The path
         * @return The builder
         */
        public OCILayout.Builder defaults(Path path) {
            layout.setPath(path);
            return this;
        }

        /**
         * Return a new builder
         * @return The builder
         */
        public static OCILayout.Builder builder() {
            return new OCILayout.Builder();
        }

        /**
         * Build the registry
         * @return The registry
         */
        public OCILayout build() {
            if (!Files.isDirectory(layout.path)) {
                throw new OrasException("Folder does not exist: %s".formatted(layout.path));
            }
            return layout;
        }
    }
}
