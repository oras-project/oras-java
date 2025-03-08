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
import org.jspecify.annotations.Nullable;

/**
 * Index from an OCI layout
 */
public final class OCILayout extends OCI<LayoutRef> {

    private final String imageLayoutVersion = "1.0.0";

    /**
     * Path on the file system of the OCI Layout
     */
    private transient Path path;

    /**
     * Private constructor
     */
    private OCILayout() {}

    @Override
    public Manifest pushArtifact(
            LayoutRef ref,
            ArtifactType artifactType,
            Annotations annotations,
            @Nullable Config config,
            LocalPath... paths) {
        throw new OrasException("Not implemented");
    }

    @Override
    public void pullArtifact(LayoutRef ref, Path path, boolean overwrite) {
        if (ref.getTag() == null) {
            throw new OrasException("Tag is required to pull artifact from layout");
        }

        // Find manifest
        Manifest manifest = findManifestByTag(ref);

        // Find the layer with title annotation
        Layer layer = manifest.getLayers().stream()
                .filter(l -> l.getAnnotations().containsKey(Const.ANNOTATION_TITLE))
                .findFirst()
                .orElseThrow(() -> new OrasException("Layer not found with title annotation"));

        Path blobPath = getBlobPath(layer);

        // Copy the blob to the target path
        try {
            Files.copy(blobPath, path.resolve(layer.getAnnotations().get(Const.ANNOTATION_TITLE)));
        } catch (IOException e) {
            throw new OrasException("Failed to copy blob", e);
        }
    }

    @Override
    public byte[] getBlob(LayoutRef containerRef) {
        try (InputStream is = fetchBlob(containerRef)) {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new OrasException("Failed to get blob", e);
        }
    }

    @Override
    public void fetchBlob(LayoutRef ref, Path path) {
        InputStream is = fetchBlob(ref);
        try {
            Files.copy(is, path);
        } catch (IOException e) {
            throw new OrasException("Failed to fetch blob", e);
        }
    }

    @Override
    public InputStream fetchBlob(LayoutRef ref) {
        Path blobPath = getBlobPath(ref);
        try {
            return Files.newInputStream(blobPath);
        } catch (IOException e) {
            throw new OrasException("Failed to fetch blob", e);
        }
    }

    @Override
    public Layer pushBlob(LayoutRef ref, Path blob, Map<String, String> annotations) {
        ensureDigest(ref);
        Path blobPath = getBlobPath(ref);
        String digest = ref.getAlgorithm().digest(blob);
        ensureAlgorithmPath(digest);
        LOG.debug("Digest: {}", digest);
        try {
            if (Files.exists(blobPath)) {
                LOG.debug("Blob already exists: {}", blobPath);
                return Layer.fromFile(blobPath, ref.getAlgorithm()).withAnnotations(annotations);
            }
            Files.copy(blob, blobPath);
            return Layer.fromFile(blobPath, ref.getAlgorithm()).withAnnotations(annotations);
        } catch (IOException e) {
            throw new OrasException("Failed to push blob", e);
        }
    }

    @Override
    public Layer pushBlob(LayoutRef ref, byte[] data) {
        ensureDigest(ref);
        String digest = ref.getAlgorithm().digest(data);
        ensureAlgorithmPath(digest);
        try {
            Path path = Files.createTempFile("oras", "blob");
            Files.write(path, data);
            return pushBlob(ref, path, Map.of());
        } catch (IOException e) {
            throw new OrasException("Failed to push blob to OCI layout", e);
        }
    }

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
     * Copy the direct container ref from registry into oci-layout
     * @param registry The registry
     * @param containerRef The container
     */
    public void copy(Registry registry, ContainerRef containerRef) {
        copy(registry, containerRef, false);
    }

    private void ensureMinimalLayout() {
        try {
            Files.createDirectories(getBlobPath());
            if (!Files.exists(getOciLayoutPath())) {
                Files.writeString(getOciLayoutPath(), toJson());
            }
            if (!Files.exists(getIndexPath())) {
                Files.writeString(getIndexPath(), Index.fromManifests(List.of()).toJson());
            }
        } catch (IOException e) {
            throw new OrasException("Failed to create layout", e);
        }
    }

    private void ensureAlgorithmPath(String digest) {
        Path prefixDirectory = getBlobAlgorithmPath(digest);
        try {
            if (!Files.exists(prefixDirectory)) {
                Files.createDirectory(prefixDirectory);
            }
        } catch (IOException e) {
            throw new OrasException("Failed to create algorithm path", e);
        }
    }

    /**
     * Copy the container ref from registry into oci-layout
     * @param registry The registry
     * @param containerRef The container
     * @param recursive True if references should be copied
     */
    public void copy(Registry registry, ContainerRef containerRef, boolean recursive) {

        try {

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

                if (recursive) {
                    LOG.debug("Recursively copy referrers");
                    Referrers referrers = registry.getReferrers(containerRef.withDigest(manifestDigest), null);
                    for (ManifestDescriptor referer : referrers.getManifests()) {
                        LOG.info("Copy reference {}", referer.getDigest());
                        copy(registry, containerRef.withDigest(referer.getDigest()), recursive);
                    }
                }

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

                    ensureAlgorithmPath(layer.getDigest());

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

    private void ensureDigest(LayoutRef ref) {
        if (ref.getTag() == null) {
            throw new OrasException("Missing ref");
        }
        if (!SupportedAlgorithm.isSupported(ref.getTag())) {
            throw new OrasException("Unsupported digest: %s".formatted(ref.getTag()));
        }
    }

    private Path getOciLayoutPath() {
        return path.resolve(Const.OCI_LAYOUT_FILE);
    }

    private Path getBlobPath() {
        return path.resolve(Const.OCI_LAYOUT_BLOBS);
    }

    private Path getBlobPath(LayoutRef ref) {
        if (ref.getTag() == null) {
            throw new OrasException("Tag is required to get blob from layout");
        }
        boolean isDigest = SupportedAlgorithm.isSupported(ref.getTag());
        if (isDigest) {
            SupportedAlgorithm algorithm = SupportedAlgorithm.fromDigest(ref.getTag());
            return getBlobPath().resolve(algorithm.getPrefix()).resolve(SupportedAlgorithm.getDigest(ref.getTag()));
        }

        Manifest manifest = findManifestByTag(ref);

        return getBlobPath(manifest.getDescriptor());
    }

    private Manifest findManifestByTag(LayoutRef ref) {
        String tag = ref.getTag();
        Index index = Index.fromPath(getIndexPath());
        ManifestDescriptor descriptor = index.getManifests().stream()
                .filter(m -> tag != null && tag.equals(m.getAnnotations().get(Const.ANNOTATION_REF)))
                .findFirst()
                .orElseThrow(() -> new OrasException("Tag not found: %s".formatted(tag)));

        Path manifestPath = getBlobPath(descriptor);
        if (!Files.exists(manifestPath)) {
            throw new OrasException("Blob not found: %s".formatted(manifestPath));
        }

        // Read the manifest
        return Manifest.fromPath(manifestPath).withDescriptor(descriptor);
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
                try {
                    Files.createDirectory(layout.path);
                } catch (IOException e) {
                    throw new OrasException("Failed to create OCI layout directory", e);
                }
            }
            layout.ensureMinimalLayout();
            return layout;
        }
    }
}
