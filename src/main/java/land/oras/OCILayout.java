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

    private String imageLayoutVersion;

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
     * Get the image layout version
     * @return The image layout version
     */
    public String getImageLayoutVersion() {
        return imageLayoutVersion;
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
     * Copy the container ref from registry into oci-layout
     * @param registry The registry
     * @param containerRef The container
     * @param folder The folder
     */
    public void copy(Registry registry, ContainerRef containerRef, Path folder) {
        if (!Files.isDirectory(folder)) {
            throw new OrasException("Folder does not exist: %s".formatted(folder));
        }

        try {

            // Create blobs directory if needed
            Path blobs = folder.resolve(Const.OCI_LAYOUT_BLOBS);
            Files.createDirectories(blobs);
            OCILayout ociLayout = OCILayout.fromJson("{\"imageLayoutVersion\":\"1.0.0\"}");

            // Write oci layout
            Files.writeString(folder.resolve(Const.OCI_LAYOUT_FOLDER), ociLayout.toJson());

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
                writeManifest(manifest, folder);

                // Write the index.json containing this manifest
                Index index = Index.fromManifests(List.of(manifest.getDescriptor()));
                writeIndex(index, folder);

                // Write config as any blob
                writeConfig(registry, containerRef, manifest.getConfig(), folder);
            }
            // Index
            else if (registry.isIndexMediaType(contentType)) {

                Index index = registry.getIndex(containerRef);

                // Write all manifests and their config
                for (ManifestDescriptor descriptor : index.getManifests()) {
                    Manifest manifest = registry.getManifest(containerRef.withDigest(descriptor.getDigest()));
                    writeManifest(manifest.withDescriptor(descriptor), folder);
                    writeConfig(registry, containerRef, manifest.getConfig(), folder);
                }

                // Write the index
                writeIndex(index, folder);

            } else {
                throw new OrasException("Unsupported content type: %s".formatted(contentType));
            }

            // Write all layer
            for (Layer layer : registry.collectLayers(containerRef, contentType, true)) {
                try (InputStream is = registry.fetchBlob(containerRef.withDigest(layer.getDigest()))) {

                    // Algorithm
                    SupportedAlgorithm algorithm = SupportedAlgorithm.fromDigest(layer.getDigest());

                    Path prefixDirectory = blobs.resolve(algorithm.getPrefix());
                    if (!Files.exists(prefixDirectory)) {
                        Files.createDirectory(prefixDirectory);
                    }
                    Path blobFile = prefixDirectory.resolve(SupportedAlgorithm.getDigest(layer.getDigest()));
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

    private void writeIndex(Index index, Path folder) throws IOException {
        Path indexFile = folder.resolve(Const.OCI_LAYOUT_INDEX);
        Files.writeString(indexFile, index.getJson() != null ? index.getJson() : index.toJson());
        if (index.getJson() != null) {
            Path blobs = folder.resolve(Const.OCI_LAYOUT_BLOBS);
            String indexDigest = index.getDescriptor().getDigest();
            SupportedAlgorithm manifestAlgorithm = SupportedAlgorithm.fromDigest(indexDigest);
            Files.writeString(
                    blobs.resolve(manifestAlgorithm.getPrefix()).resolve(SupportedAlgorithm.getDigest(indexDigest)),
                    index.getJson());
        }
    }

    private void writeManifest(Manifest manifest, Path folder) throws IOException {
        Path blobs = folder.resolve(Const.OCI_LAYOUT_BLOBS);
        ManifestDescriptor descriptor = manifest.getDescriptor();
        String manifestDigest = descriptor.getDigest();
        SupportedAlgorithm manifestAlgorithm = SupportedAlgorithm.fromDigest(manifestDigest);
        Path manifestFile = blobs.resolve(manifestAlgorithm.getPrefix())
                .resolve(SupportedAlgorithm.getDigest(descriptor.getDigest()));
        Path manifestPrefixDirectory = blobs.resolve(manifestAlgorithm.getPrefix());
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

    private void writeConfig(Registry registry, ContainerRef containerRef, Config config, Path folder)
            throws IOException {
        Path blobs = folder.resolve(Const.OCI_LAYOUT_BLOBS);
        String configDigest = config.getDigest();
        SupportedAlgorithm configAlgorithm = SupportedAlgorithm.fromDigest(configDigest);
        Path configFile =
                blobs.resolve(configAlgorithm.getPrefix()).resolve(SupportedAlgorithm.getDigest(config.getDigest()));
        Path configPrefixDirectory = blobs.resolve(configAlgorithm.getPrefix());
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

            return layout;
        }
    }
}
