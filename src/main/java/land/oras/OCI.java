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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import land.oras.exception.OrasException;
import land.oras.utils.ArchiveUtils;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import land.oras.utils.SupportedAlgorithm;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for OCI operation on remote registry or layout
 * Commons methods for OCI operations
 * @param <T> The reference type
 */
public abstract sealed class OCI<T extends Ref<@NonNull T>> permits Registry, OCILayout {

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
     * Push a blob from file
     * @param ref The ref
     * @param input The input stream
     * @return The layer
     */
    public Layer pushBlob(T ref, InputStream input) {
        try {
            Path tempFile = Files.createTempFile("oras", "layer");
            Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return pushBlob(ref, tempFile);
        } catch (IOException e) {
            throw new OrasException("Failed to push blob", e);
        }
    }

    /**
     * Collect layers from the ref
     * @param ref The ref
     * @param contentType The content type
     * @param includeAll Include all layers or only the ones with title annotation
     * @return The layers
     */
    protected List<Layer> collectLayers(T ref, String contentType, boolean includeAll) {
        List<Layer> layers = new LinkedList<>();
        if (isManifestMediaType(contentType)) {
            return getManifest(ref).getLayers();
        }
        Index index = getIndex(ref);
        for (ManifestDescriptor manifestDescriptor : index.getManifests()) {
            List<Layer> manifestLayers =
                    getManifest(ref.withDigest(manifestDescriptor.getDigest())).getLayers();
            for (Layer manifestLayer : manifestLayers) {
                if (manifestLayer.getAnnotations().isEmpty()
                        || !manifestLayer.getAnnotations().containsKey(Const.ANNOTATION_TITLE)) {
                    if (includeAll) {
                        LOG.debug("Including layer without title annotation: {}", manifestLayer.getDigest());
                        layers.add(manifestLayer);
                    }
                    LOG.debug("Skipping layer without title annotation: {}", manifestLayer.getDigest());
                    continue;
                }
                layers.add(manifestLayer);
            }
        }
        return layers;
    }

    /**
     * Push layers to the target
     * @param ref The ref
     * @param withDigest Push with digest
     * @param paths The paths to the files
     * @return The layers
     */
    protected final List<Layer> pushLayers(T ref, boolean withDigest, LocalPath... paths) {
        List<Layer> layers = new ArrayList<>();
        for (LocalPath path : paths) {
            try {
                // Create tar.gz archive for directory
                if (Files.isDirectory(path.getPath())) {
                    LocalPath tempTar = ArchiveUtils.tar(path);
                    LocalPath tempArchive = ArchiveUtils.compress(tempTar, path.getMediaType());
                    if (withDigest) {
                        ref = ref.withDigest(ref.getAlgorithm().digest(tempArchive.getPath()));
                    }
                    try (InputStream is = Files.newInputStream(tempArchive.getPath())) {
                        Layer layer = pushBlob(ref, is)
                                .withMediaType(path.getMediaType())
                                .withAnnotations(Map.of(
                                        Const.ANNOTATION_TITLE,
                                        path.getPath().getFileName().toString(),
                                        Const.ANNOTATION_ORAS_CONTENT_DIGEST,
                                        ref.getAlgorithm().digest(tempTar.getPath()),
                                        Const.ANNOTATION_ORAS_UNPACK,
                                        "true"));
                        layers.add(layer);
                        LOG.info("Uploaded directory: {}", layer.getDigest());
                    }
                    Files.delete(tempArchive.getPath());
                } else {
                    try (InputStream is = Files.newInputStream(path.getPath())) {
                        if (withDigest) {
                            ref = ref.withDigest(ref.getAlgorithm().digest(path.getPath()));
                        }
                        Layer layer = pushBlob(ref, is)
                                .withMediaType(path.getMediaType())
                                .withAnnotations(Map.of(
                                        Const.ANNOTATION_TITLE,
                                        path.getPath().getFileName().toString()));
                        layers.add(layer);
                        LOG.info("Uploaded: {}", layer.getDigest());
                    }
                }
            } catch (IOException e) {
                throw new OrasException("Failed to push artifact", e);
            }
        }
        return layers;
    }

    /**
     * Return if a media type is an index media type
     * @param mediaType The media type
     * @return True if it is a index media type
     */
    protected boolean isIndexMediaType(String mediaType) {
        return mediaType.equals(Const.DEFAULT_INDEX_MEDIA_TYPE) || mediaType.equals(Const.DOCKER_INDEX_MEDIA_TYPE);
    }

    /**
     * Return if a media type is a manifest media type
     * @param mediaType The media type
     * @return True if it is a manifest media type
     */
    protected boolean isManifestMediaType(String mediaType) {
        return mediaType.equals(Const.DEFAULT_MANIFEST_MEDIA_TYPE)
                || mediaType.equals(Const.DOCKER_MANIFEST_MEDIA_TYPE);
    }

    /**
     * Push config
     * @param ref The ref
     * @param config The config
     * @return The config
     */
    public final Config pushConfig(T ref, Config config) {
        Layer layer = pushBlob(ref, config.getDataBytes());
        LOG.debug("Config pushed: {}", layer.getDigest());
        return config;
    }

    /**
     * Pull config data or just return the data from config if set inline
     * @param ref The ref
     * @param config The config
     * @return The input stream
     */
    public final InputStream pullConfig(T ref, Config config) {
        if (config.getData() != null) {
            return new ByteArrayInputStream(config.getDataBytes());
        }
        String digest = config.getDigest();
        return fetchBlob(ref.withDigest(digest));
    }

    /**
     * Attach an artifact
     * @param ref The ref
     * @param artifactType The artifact type
     * @param paths The paths
     * @return The manifest
     */
    public final Manifest attachArtifact(T ref, ArtifactType artifactType, LocalPath... paths) {
        return attachArtifact(ref, artifactType, Annotations.empty(), paths);
    }

    /**
     * Get the tags for a ref
     * @param ref The ref
     * @return The tags
     */
    public abstract Tags getTags(T ref);

    /**
     * Get the tags for a ref
     * @return The repositories
     */
    public abstract Repositories getRepositories();

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
     * Push a manifest
     * @param ref The ref
     * @param manifest The manifest
     * @return The location
     */
    public abstract Manifest pushManifest(T ref, Manifest manifest);

    /**
     * Push an index
     * @param ref The ref
     * @param index The index
     * @return The index
     */
    public abstract Index pushIndex(T ref, Index index);

    /**
     * Retrieve an index
     * @param ref The ref
     * @return The index
     */
    public abstract Index getIndex(T ref);

    /**
     * Retrieve a manifest
     * @param ref The ref
     * @return The manifest
     */
    public abstract Manifest getManifest(T ref);

    /**
     * Retrieve a descriptor
     * @param ref The ref
     * @return The descriptor
     */
    public abstract Descriptor getDescriptor(T ref);

    /**
     * Probe a descriptor. Typically used to get digest, size and media type without the content
     * @param ref The ref
     * @return The descriptor
     */
    public abstract Descriptor probeDescriptor(T ref);

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
     * Fetch blob and return it's descriptor
     * @param ref The ref
     * @return The descriptor
     */
    public abstract Descriptor fetchBlobDescriptor(T ref);

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

    /**
     * Get the referrers of a container
     * @param ref The ref
     * @param artifactType The optional artifact type
     * @return The referrers
     */
    public abstract Referrers getReferrers(T ref, @Nullable ArtifactType artifactType);

    /**
     * Attach file to an existing manifest
     * @param ref The ref
     * @param artifactType The artifact type
     * @param annotations The annotations
     * @param paths The paths
     * @return The manifest of the new artifact
     */
    public Manifest attachArtifact(T ref, ArtifactType artifactType, Annotations annotations, LocalPath... paths) {

        // Push layers
        List<Layer> layers = pushLayers(ref, true, paths);

        // Get the subject from the descriptor
        Descriptor descriptor = getDescriptor(ref);
        Subject subject = descriptor.toSubject();

        // Add created annotation if not present since we push with digest
        Map<String, String> manifestAnnotations = annotations.manifestAnnotations();
        if (!manifestAnnotations.containsKey(Const.ANNOTATION_CREATED)) {
            manifestAnnotations.put(Const.ANNOTATION_CREATED, Const.currentTimestamp());
        }

        // assemble manifest
        Manifest manifest = Manifest.empty()
                .withArtifactType(artifactType)
                .withAnnotations(manifestAnnotations)
                .withLayers(layers)
                .withSubject(subject);
        return pushManifest(
                ref.withDigest(
                        SupportedAlgorithm.getDefault().digest(manifest.toJson().getBytes(StandardCharsets.UTF_8))),
                manifest);
    }
    /**
     * Collect all referenced digests from a manifest or index recursively.
     *
     * @param layout the OCI layout instance
     * @param digest the digest of the manifest or index to process
     * @param visited set of visited digests to avoid cycles
     * @return set of referenced digests
     * @throws IOException if an I/O error occurs
     */
    public Set<String> collectReferencedDigests(OCILayout layout, String digest, Set<String> visited)
            throws IOException {
        Set<String> digests = new HashSet<>();
        if (visited.contains(digest) || digest == null) {
            return digests; // Avoid infinite recursion or null digests
        }
        visited.add(digest);

        // Read the manifest or index
        Path blobPath = layout.getBlobPath(digest);
        String content = Files.readString(blobPath);
        Map<String, Object> node = JsonUtils.fromJson(content, Map.class);

        // Handle index (has "manifests")
        if (node.containsKey("manifests")) {
            List<Map<String, Object>> manifests = (List<Map<String, Object>>) node.get("manifests");
            for (Map<String, Object> manifest : manifests) {
                String manifestDigest = (String) manifest.get("digest");
                if (manifestDigest != null) {
                    digests.add(manifestDigest);
                    digests.addAll(collectReferencedDigests(layout, manifestDigest, visited));
                }
            }
        } else {
            // Handle manifest
            // Collect config digest
            if (node.containsKey("config")) {
                Map<String, Object> config = (Map<String, Object>) node.get("config");
                String configDigest = (String) config.get("digest");
                if (configDigest != null) {
                    digests.add(configDigest);
                }
            }
            // Collect layer digests
            if (node.containsKey("layers")) {
                List<Map<String, Object>> layers = (List<Map<String, Object>>) node.get("layers");
                for (Map<String, Object> layer : layers) {
                    String layerDigest = (String) layer.get("digest");
                    if (layerDigest != null) {
                        digests.add(layerDigest);
                    }
                }
            }
            // Handle subject (for reference artifacts)
            if (node.containsKey("subject")) {
                Map<String, Object> subject = (Map<String, Object>) node.get("subject");
                String subjectDigest = (String) subject.get("digest");
                if (subjectDigest != null) {
                    digests.add(subjectDigest);
                    digests.addAll(collectReferencedDigests(layout, subjectDigest, visited));
                }
            }
        }

        return digests;
    }

    /**
     * Get the blob path for a given digest.
     *
     * @param digest the digest (e.g., sha256:abc123)
     * @return the file path to the blob
     */
    public Path getBlobPath(String digest) {
        String[] parts = digest.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid digest format: " + digest);
        }
        String algorithm = parts[0];
        String hash = parts[1];
        if (this instanceof OCILayout) {
            OCILayout layout = (OCILayout) this;
            return layout.getPath().resolve("blobs").resolve(algorithm).resolve(hash);
        }
        throw new UnsupportedOperationException("getBlobPath not supported in this context");
    }

    /**
     * Get the layout path.
     *
     * @return the path
     */
    protected Path getPath() {
        throw new UnsupportedOperationException("getPath not supported in this context");
    }
}
