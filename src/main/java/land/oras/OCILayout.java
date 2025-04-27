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
import java.util.*;
import java.util.stream.Stream;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import land.oras.utils.SupportedAlgorithm;
import org.jspecify.annotations.Nullable;

/**
 * Index from an OCI layout
 */
public final class OCILayout extends OCI<LayoutRef> {

    @SuppressWarnings("all")
    private final String imageLayoutVersion = "1.0.0";

    /**
     * Path on the file system of the OCI Layout
     */
    private transient Path path;

    /**
     * Private constructor
     */
    private OCILayout() {}

    /**
     * Return a new builder for this oci layout
     * @return The builder
     */
    public static OCILayout.Builder builder() {
        return OCILayout.Builder.builder();
    }

    @Override
    public Manifest pushArtifact(
            LayoutRef ref,
            ArtifactType artifactType,
            Annotations annotations,
            @Nullable Config config,
            LocalPath... paths) {

        Manifest manifest = Manifest.empty().withArtifactType(artifactType);
        Map<String, String> manifestAnnotations = new HashMap<>(annotations.manifestAnnotations());
        if (!manifestAnnotations.containsKey(Const.ANNOTATION_CREATED)) {
            manifestAnnotations.put(Const.ANNOTATION_CREATED, Const.currentTimestamp());
        }
        manifest = manifest.withAnnotations(manifestAnnotations);
        if (config != null) {
            config = config.withAnnotations(annotations);
            manifest = manifest.withConfig(config);
        }

        // Push layers
        List<Layer> layers = pushLayers(ref, true, paths);

        // Push the config like any other blob
        Config configToPush = config != null ? config : Config.empty();
        Config pushedConfig = pushConfig(ref.withTag(configToPush.getDigest()), configToPush);

        // Add layer and config
        manifest = manifest.withLayers(layers).withConfig(pushedConfig);

        // Push the manifest
        manifest = pushManifest(ref, manifest);
        LOG.debug("Manifest pushed to: {}", ref.withTag(manifest.getDescriptor().getDigest()));
        return manifest;
    }

    @Override
    public void pullArtifact(LayoutRef ref, Path path, boolean overwrite) {
        if (ref.getTag() == null) {
            throw new OrasException("Tag is required to pull artifact from layout");
        }

        // Find manifest
        Manifest manifest = getManifest(ref);

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
    public Manifest pushManifest(LayoutRef layoutRef, Manifest manifest) {

        // For portability each layer should have at least one entry
        if (manifest.getLayers().isEmpty()) {
            Config config = manifest.getConfig();
            Layer configLayer = Layer.fromJson(config.toJson());
            manifest = manifest.withLayers(List.of(configLayer));
        }

        byte[] manifestData = getDescriptorData(manifest);
        String manifestDigest = digest(layoutRef, manifest);

        ManifestDescriptor manifestDescriptor = ManifestDescriptor.of(
                        Const.DEFAULT_MANIFEST_MEDIA_TYPE, manifestDigest, manifestData.length)
                .withAnnotations(manifest.getAnnotations().isEmpty() ? null : manifest.getAnnotations())
                .withArtifactType(manifest.getArtifactType().getMediaType());
        if (layoutRef.getTag() != null && !layoutRef.isValidDigest()) {
            Map<String, String> newAnnotations = new HashMap<>();
            if (manifestDescriptor.getAnnotations() != null) {
                newAnnotations.putAll(manifestDescriptor.getAnnotations());
            }
            newAnnotations.put(Const.ANNOTATION_REF, layoutRef.getTag());
            manifestDescriptor = manifestDescriptor.withAnnotations(newAnnotations);
        }
        manifest = manifest.withDescriptor(manifestDescriptor);

        Index index = Index.fromPath(getIndexPath()).withNewManifests(manifestDescriptor);

        // Write blobs
        try {
            writeManifest(manifest);
            writeOCIIndex(index);
        } catch (IOException e) {
            throw new OrasException("Failed to write manifest", e);
        }
        return manifest;
    }

    @Override
    public Index pushIndex(LayoutRef layoutRef, Index index) {

        byte[] indexData = getDescriptorData(index);
        String indexDigest = digest(layoutRef, index);

        ManifestDescriptor indexDescriptor = ManifestDescriptor.of(
                        Const.DEFAULT_INDEX_MEDIA_TYPE, indexDigest, indexData.length)
                .withAnnotations(
                        index.getAnnotations() == null || index.getAnnotations().isEmpty()
                                ? null
                                : index.getAnnotations())
                .withArtifactType(index.getMediaType());
        if (layoutRef.getTag() != null && !layoutRef.isValidDigest()) {
            Map<String, String> newAnnotations = new HashMap<>();
            if (index.getAnnotations() != null) {
                newAnnotations.putAll(index.getAnnotations());
            }
            newAnnotations.put(Const.ANNOTATION_REF, layoutRef.getTag());
            indexDescriptor = indexDescriptor.withAnnotations(newAnnotations);
        }
        index = index.withDescriptor(indexDescriptor);

        Index ociIndex = Index.fromPath(getIndexPath()).withNewManifests(indexDescriptor);

        // Write blobs
        try {
            writeIndex(index);
            writeOCIIndex(ociIndex);
        } catch (IOException e) {
            throw new OrasException("Failed to write manifest", e);
        }
        return index;
    }

    @Override
    public Index getIndex(LayoutRef ref) {
        Path path = getIndexPath();
        return Index.fromPath(path);
    }

    @Override
    public Manifest getManifest(LayoutRef ref) {
        String tag = ref.getTag();
        if (tag == null) {
            throw new OrasException("Tag or digest is required to find manifest");
        }
        ManifestDescriptor descriptor = findManifestDescriptor(ref);
        Path manifestPath = getBlobPath(descriptor);
        if (!Files.exists(manifestPath)) {
            throw new OrasException("Blob not found: %s".formatted(manifestPath));
        }

        return Manifest.fromPath(manifestPath).withDescriptor(descriptor);
    }

    @Override
    public byte[] getBlob(LayoutRef layoutRef) {
        try (InputStream is = fetchBlob(layoutRef)) {
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
            LOG.info("Downloaded: {}", ref.getTag());
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
    public Descriptor fetchBlobDescriptor(LayoutRef ref) {
        if (ref.getTag() == null) {
            throw new OrasException("Tag or digest is required to get blob from layout");
        }
        if (SupportedAlgorithm.isSupported(ref.getTag())) {
            return Descriptor.of(ref.getTag(), size(getBlobPath(ref)));
        }
        // A manifest
        else {
            return findManifestDescriptor(ref).toDescriptor();
        }
    }

    @Override
    public Layer pushBlob(LayoutRef ref, Path blob, Map<String, String> annotations) {
        ensureDigest(ref, blob);
        Path blobPath = getBlobPath(ref);
        String digest = ref.getAlgorithm().digest(blob);
        ensureAlgorithmPath(digest);
        LOG.debug("Digest: {}", digest);
        try {
            if (Files.exists(blobPath)) {
                LOG.info("Blob already exists: {}", digest);
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
        try {
            Path path = Files.createTempFile("oras", "blob");
            Files.write(path, data);
            ensureDigest(ref, path);
            String digest = ref.getAlgorithm().digest(data);
            ensureAlgorithmPath(digest);
            return pushBlob(ref, path, Map.of());
        } catch (IOException e) {
            throw new OrasException("Failed to push blob to OCI layout", e);
        }
    }

    @Override
    public Tags getTags(LayoutRef ref) {
        Index index = Index.fromPath(getIndexPath());
        String name = ref.getFolder().getFileName().toString();
        List<String> tags = index.getManifests().stream()
                .filter(m -> m.getAnnotations() != null && m.getAnnotations().containsKey(Const.ANNOTATION_REF))
                .map(m -> m.getAnnotations().get(Const.ANNOTATION_REF))
                .toList();
        return new Tags(name, tags);
    }

    @Override
    public Referrers getReferrers(LayoutRef ref, @Nullable ArtifactType artifactType) {
        Index index = Index.fromPath(getIndexPath());
        ManifestDescriptor currentDescriptor = findManifestDescriptor(ref);
        String currentDescriptorDigest = currentDescriptor.getDigest();
        LOG.info("Looking for referrers of manifest: {}", currentDescriptorDigest);
        List<ManifestDescriptor> manifestDescriptors = new LinkedList<>();
        for (ManifestDescriptor manifestDescriptor : index.getManifests()) {
            String digest = manifestDescriptor.getDigest();
            Descriptor descriptor = probeDescriptor(ref.withDigest(digest));
            Describable describable = isIndexMediaType(descriptor.getMediaType())
                    ? getIndex(ref.withDigest(digest))
                    : getManifest(ref.withDigest(digest));
            if (describable.getSubject() != null) {
                Subject subject = describable.getSubject();
                String subjectDigest = subject.getDigest();
                if (subjectDigest.equals(currentDescriptorDigest)) {
                    LOG.info("Subject with digest {} found for manifest: {}", subjectDigest, digest);
                    manifestDescriptors.add(manifestDescriptor);
                }
            }
        }
        return Referrers.from(manifestDescriptors);
    }

    private void setPath(Path path) {
        this.path = path;
    }
    /**
     * Clean up unreferenced blobs in the OCI layout.
     *
     * @return the number of blobs deleted
     * @throws IOException if an I/O error occurs
     */
    public int cleanupUnreferencedBlobs() throws IOException {
        LOG.info("Starting cleanup of unreferenced blobs in OCI layout: {}", path);

        // Collect all referenced digests
        Set<String> referencedDigests = new HashSet<>();
        Path indexPath = path.resolve("index.json");
        if (!Files.exists(indexPath)) {
            LOG.warn("No index.json found in layout: {}", path);
            return 0;
        }

        String indexContent = Files.readString(indexPath);
        Map<String, Object> index = JsonUtils.fromJson(indexContent, Map.class);
        if (index.containsKey("manifests")) {
            List<Map<String, Object>> manifests = (List<Map<String, Object>>) index.get("manifests");
            for (Map<String, Object> manifest : manifests) {
                String digest = (String) manifest.get("digest");
                if (digest != null) {
                    referencedDigests.add(digest);
                    referencedDigests.addAll(collectReferencedDigests(this, digest, new HashSet<>()));
                }
            }
        } else {
            LOG.warn("No manifests found in index.json: {}", indexPath);
        }

        // List all blobs in blobs/sha256
        Path blobsDir = path.resolve("blobs").resolve("sha256");
        if (!Files.exists(blobsDir)) {
            LOG.warn("No blobs directory found in layout: {}", path);
            return 0;
        }

        Set<String> allBlobs = new HashSet<>();
        try (Stream<Path> blobPaths = Files.list(blobsDir)) {
            blobPaths.forEach(blobPath -> {
                String fileName = blobPath.getFileName().toString();
                allBlobs.add("sha256:" + fileName);
            });
        }

        // Identify and delete unreferenced blobs
        int deletedCount = 0;
        for (String blobDigest : allBlobs) {
            if (!referencedDigests.contains(blobDigest)) {
                Path blobPath = blobsDir.resolve(blobDigest.substring("sha256:".length()));
                try {
                    Files.delete(blobPath);
                    LOG.info("Deleted unreferenced blob: {}", blobDigest);
                    deletedCount++;
                } catch (IOException e) {
                    LOG.error("Failed to delete blob {}: {}", blobDigest, e.getMessage());
                }
            }
        }

        LOG.info("Cleanup completed. Deleted {} unreferenced blobs.", deletedCount);
        return deletedCount;
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

    @Override
    public Descriptor getDescriptor(LayoutRef ref) {
        String tag = ref.getTag();
        if (tag == null) {
            throw new OrasException("Tag or digest is required to find manifest");
        }
        ManifestDescriptor manifestDescriptor = findManifestDescriptor(ref);
        return manifestDescriptor.toDescriptor();
    }

    @Override
    public Descriptor probeDescriptor(LayoutRef ref) {
        // We should probably optimize to avoid reading the descriptor and only get its attributes (JSON path?)
        return getDescriptor(ref).withJson(null);
    }

    private byte[] getDescriptorData(Descriptor descriptor) {
        if (descriptor.getJson() != null) {
            return descriptor.getJson().getBytes();
        }
        return descriptor.toJson().getBytes();
    }

    private String digest(LayoutRef layoutRef, Descriptor descriptor) {
        return layoutRef
                .getAlgorithm()
                .digest(
                        descriptor.getJson() != null
                                ? descriptor.getJson().getBytes()
                                : descriptor.toJson().getBytes());
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
        boolean isDigest = SupportedAlgorithm.matchPattern(ref.getTag());
        if (isDigest) {
            SupportedAlgorithm algorithm = SupportedAlgorithm.fromDigest(ref.getTag());
            return getBlobPath().resolve(algorithm.getPrefix()).resolve(SupportedAlgorithm.getDigest(ref.getTag()));
        }

        Manifest manifest = getManifest(ref);

        return getBlobPath(manifest.getDescriptor());
    }

    private long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new OrasException("Failed to get size", e);
        }
    }

    private ManifestDescriptor findManifestDescriptor(LayoutRef ref) {
        String tag = ref.getTag();
        if (tag == null) {
            throw new OrasException("Tag or digest is required to find manifest");
        }
        Index index = Index.fromPath(getIndexPath());
        return index.getManifests().stream()
                .filter(m -> (m.getAnnotations() != null
                                && tag.equals(m.getAnnotations().get(Const.ANNOTATION_REF))
                        || tag.equals(m.getDigest())))
                .findFirst()
                .orElseThrow(() -> new OrasException("Tag or digest not found: %s".formatted(tag)));
    }

    private Path getBlobPath(ManifestDescriptor manifestDescriptor) {
        String digest = manifestDescriptor.getDigest();
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
        ManifestDescriptor descriptor = index.getDescriptor();
        if (descriptor == null)
            throw new OrasException("Index descriptor is required when writing index blob with existing JSON");
        String digest = descriptor.getDigest();
        return getBlobAlgorithmPath(digest).resolve(SupportedAlgorithm.getDigest(digest));
    }

    private Path getBlobAlgorithmPath(String digest) {
        SupportedAlgorithm algorithm = SupportedAlgorithm.fromDigest(digest);
        return getBlobPath().resolve(algorithm.getPrefix());
    }

    private void writeOCIIndex(Index index) throws IOException {
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

    private void writeIndex(Index index) throws IOException {
        ManifestDescriptor descriptor = index.getDescriptor();
        Path manifestFile = getBlobPath(descriptor);
        Path manifestPrefixDirectory =
                getBlobAlgorithmPath(index.getDescriptor().getDigest());

        if (!Files.exists(manifestPrefixDirectory)) {
            Files.createDirectory(manifestPrefixDirectory);
        }
        // Skip if already exists
        if (Files.exists(manifestFile)) {
            LOG.debug("Manifest already exists: {}", manifestFile);
            return;
        }
        if (index.getJson() == null) {
            LOG.debug("Writing new manifest: {}", manifestFile);
            Files.writeString(manifestFile, index.toJson());
        } else {
            LOG.debug("Writing existing manifest: {}", manifestFile);
            Files.writeString(manifestFile, index.getJson());
        }
    }

    private void ensureDigest(LayoutRef ref, Path path) {
        if (ref.getTag() == null) {
            throw new OrasException("Missing ref");
        }
        if (!SupportedAlgorithm.matchPattern(ref.getTag())) {
            throw new OrasException("Unsupported digest: %s".formatted(ref.getTag()));
        }
        SupportedAlgorithm algorithm = SupportedAlgorithm.fromDigest(ref.getTag());
        String pathDigest = algorithm.digest(path);
        if (!ref.getTag().equals(pathDigest)) {
            throw new OrasException("Digest mismatch: %s != %s".formatted(ref.getTag(), pathDigest));
        }
    }

    /**
     * Return the OCI layout from the index.json file
     * @param layoutPath The path to the layout containing the index.json file
     * @return The OCI layout
     */
    public static OCILayout fromLayoutIndex(Path layoutPath) {
        OCILayout layout = JsonUtils.fromJson(layoutPath.resolve(Const.OCI_LAYOUT_INDEX), OCILayout.class);
        layout.path = layoutPath;
        return layout;
    }

    /**
     * Return the path to the OCI layout
     * @return The path to the OCI layout
     */
    public Path getPath() {
        return path;
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
