package land.oras;

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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import land.oras.exception.OrasException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copy utility class.
 */
public final class CopyUtils {

    /**
     * The logger
     */
    protected static final Logger LOG = LoggerFactory.getLogger(CopyUtils.class);

    /**
     * Maximum recursion depth when copying nested indexes and referrer graphs.
     * Avoid malicious source that serves an unbounded chain, protecting the copier from a StackOverflow.
     */
    private static final int MAX_COPY_DEPTH = 32;

    /**
     * Private constructor
     */
    private CopyUtils() {
        // Utils class
    }

    /**
     * Options for copy.
     */
    @OrasModel
    public static final class CopyOptions {

        private final boolean includeReferrers;
        private final @Nullable Set<Platform> platformFilter;

        private CopyOptions(boolean includeReferrers, @Nullable Set<Platform> platformFilter) {
            this.includeReferrers = includeReferrers;
            this.platformFilter = platformFilter;
        }

        /**
         * The default copy options with includeReferrers to false
         * @return The default copy options
         */
        public static CopyOptions shallow() {
            return new CopyOptions(false, null);
        }

        /**
         * The copy options with includeReferrers and recursive set to true.
         * @return The copy options with includeReferrers and recursive set to true
         */
        public static CopyOptions deep() {
            return new CopyOptions(true, null);
        }

        /**
         * Return a new CopyOptions with the given platform filter.
         * When copying an index, only manifests matching one of the given platforms will be copied.
         * The resulting index will contain only the filtered manifests and will have a different digest.
         * @param platforms The platforms to filter by
         * @return New CopyOptions with the platform filter set
         */
        public CopyOptions withPlatformFilter(Set<Platform> platforms) {
            return new CopyOptions(includeReferrers, platforms);
        }

        /**
         * Return whether referrers should be included in the copy.
         * @return {@code true} if referrers should be included
         */
        public boolean includeReferrers() {
            return includeReferrers;
        }

        /**
         * Return the optional platform filter.
         * @return The platform filter, or {@code null} if not set
         */
        public @Nullable Set<Platform> platformFilter() {
            return platformFilter;
        }
    }

    /**
     * Copy a container from source to target.
     * @deprecated Use {@link #copy(OCI, Ref, OCI, Ref, CopyOptions)} instead. This method will be removed in a future release.
     * @param source The source OCI
     * @param sourceRef The source reference
     * @param target The target OCI
     * @param targetRef The target reference
     * @param recursive Copy referers
     * @param <SourceRefType> The source reference type
     * @param <TargetRefType> The target reference type
     */
    @Deprecated(forRemoval = true)
    public static <SourceRefType extends Ref<@NonNull SourceRefType>, TargetRefType extends Ref<@NonNull TargetRefType>>
            void copy(
                    OCI<SourceRefType> source,
                    SourceRefType sourceRef,
                    OCI<TargetRefType> target,
                    TargetRefType targetRef,
                    boolean recursive) {
        copy(source, sourceRef, target, targetRef, recursive ? CopyOptions.deep() : CopyOptions.shallow());
    }

    /**
     * Copy all layers for a given reference and content type from source to target.
     * @param source The source OCI
     * @param sourceRef The source reference
     * @param target The target OCI
     * @param targetRef The target reference
     * @param contentType The content type (manifest or index media type)
     * @param <SourceRefType> The source reference type
     * @param <TargetRefType> The target reference type
     */
    @SuppressWarnings("unchecked")
    private static <
                    SourceRefType extends Ref<@NonNull SourceRefType>,
                    TargetRefType extends Ref<@NonNull TargetRefType>>
            void copyLayers(
                    OCI<SourceRefType> source,
                    SourceRefType sourceRef,
                    OCI<TargetRefType> target,
                    TargetRefType targetRef,
                    String contentType) {
        CompletableFuture.allOf(source.collectLayers(sourceRef, contentType, true).stream()
                        .map(layer -> {
                            Objects.requireNonNull(layer.getDigest(), "Layer digest is required for streaming copy");
                            Objects.requireNonNull(layer.getSize(), "Layer size is required for streaming copy");
                            return CompletableFuture.runAsync(
                                    () -> {
                                        if (canMount(source, sourceRef, target, targetRef)) {
                                            boolean result = target.mountBlob(
                                                    (TargetRefType) sourceRef.withDigest(layer.getDigest()),
                                                    targetRef.withDigest(layer.getDigest()));
                                            if (result) {
                                                LOG.debug(
                                                        "Copied layer (mounted from {}) {}",
                                                        sourceRef.getRepository(),
                                                        layer.getDigest());
                                                return;
                                            }
                                        }
                                        target.pushBlob(
                                                targetRef.withDigest(layer.getDigest()),
                                                layer.getSize(),
                                                () -> source.fetchBlob(sourceRef.withDigest(layer.getDigest())),
                                                layer.getAnnotations());
                                    },
                                    source.getExecutorService());
                        })
                        .toArray(CompletableFuture[]::new))
                .join();
    }

    /**
     * Copy a container from source to target.
     * @param source The source OCI
     * @param sourceRef The source reference
     * @param target The target OCI
     * @param targetRef The target reference
     * @param options The copy option
     * @param <SourceRefType> The source reference type
     * @param <TargetRefType> The target reference type
     */
    public static <SourceRefType extends Ref<@NonNull SourceRefType>, TargetRefType extends Ref<@NonNull TargetRefType>>
            void copy(
                    OCI<SourceRefType> source,
                    SourceRefType sourceRef,
                    OCI<TargetRefType> target,
                    TargetRefType targetRef,
                    CopyOptions options) {
        copy(source, sourceRef, target, targetRef, options, new HashSet<>(), 0);
    }

    /**
     * Copy a container from source to target, tracking visited digests and recursion depth to guard
     * against a malicious source that serves a cyclic or unbounded-depth index/referrer graph.
     * @param visited The digests already copied in this operation (cycle and diamond guard)
     * @param depth The current recursion depth
     */
    private static <
                    SourceRefType extends Ref<@NonNull SourceRefType>,
                    TargetRefType extends Ref<@NonNull TargetRefType>>
            void copy(
                    OCI<SourceRefType> source,
                    SourceRefType sourceRef,
                    OCI<TargetRefType> target,
                    TargetRefType targetRef,
                    CopyOptions options,
                    Set<String> visited,
                    int depth) {

        if (depth > MAX_COPY_DEPTH) {
            LOG.error("Depth exceeded for copy of {}", sourceRef.getRepository());
            throw new OrasException(
                    "Maximum copy recursion depth (%d) exceeded; the source may serve an unbounded index or referrer graph"
                            .formatted(MAX_COPY_DEPTH));
        }

        boolean includeReferrers = options.includeReferrers();

        Descriptor descriptor = source.probeDescriptor(sourceRef);

        // Guard against cycles from malicious source
        String probedDigest = descriptor.getDigest();
        if (probedDigest != null && !visited.add(probedDigest)) {
            LOG.warn("Skipping already-copied content {} (cycle or shared reference)", probedDigest);
            return;
        }

        // Get the resolved source registry
        String resolveSourceRegistry = descriptor.getRegistry();
        Objects.requireNonNull(resolveSourceRegistry, "Registry is required for streaming copy");

        // Get the resolve target registry
        String effectiveTargetRegistry = targetRef.getTarget(target);
        Objects.requireNonNull(effectiveTargetRegistry, "Target registry is required for streaming copy");

        String contentType = descriptor.getMediaType();
        String manifestDigest = descriptor.getDigest();
        LOG.debug("Content type: {}", contentType);
        LOG.debug("Manifest digest: {}", manifestDigest);

        SourceRefType effectiveSourceRef = sourceRef.forTarget(source).forTarget(resolveSourceRegistry);
        TargetRefType effectiveTargetRef = targetRef.forTarget(target).forTarget(effectiveTargetRegistry);

        LOG.info("Copying from {} to {}", effectiveSourceRef.getRepository(), effectiveTargetRef.getRepository());

        // Single manifest
        if (source.isManifestMediaType(contentType)) {

            // Write all layers
            copyLayers(source, effectiveSourceRef, target, effectiveTargetRef, contentType);

            // Write manifest as any blob
            Manifest manifest = source.getManifest(effectiveSourceRef);
            String targetTag = effectiveTargetRef.getTag();

            Objects.requireNonNull(manifest.getDigest(), "Manifest digest is required for streaming copy");

            // Push config
            copyConfig(manifest, source, effectiveSourceRef, target, effectiveTargetRef);

            // Push the manifest
            LOG.debug("Copying manifest {}", manifestDigest);
            target.pushManifest(effectiveTargetRef.withDigest(targetTag), manifest);
            LOG.debug("Copied manifest {} with tag {}", manifestDigest, targetTag);

            if (includeReferrers) {
                LOG.debug("Including referrers on copy of manifest {}", manifestDigest);
                Referrers referrers = source.getReferrers(effectiveSourceRef.withDigest(manifestDigest), null);
                for (ManifestDescriptor referer : referrers.getManifests()) {
                    LOG.debug("Copy reference from referrers {}", referer.getDigest());
                    copy(
                            source,
                            effectiveSourceRef.withDigest(referer.getDigest()),
                            target,
                            effectiveTargetRef.withDigest(referer.getDigest()),
                            options,
                            visited,
                            depth + 1);
                }
            } else {
                LOG.debug("Not including referrers on copy of manifest {}", manifestDigest);
            }

        }
        // Index
        else if (source.isIndexMediaType(contentType)) {

            Index index = source.getIndex(effectiveSourceRef);
            String targetTag = effectiveTargetRef.getTag();

            // Apply platform filter if set — partial copy produces a new index with fewer manifests
            List<ManifestDescriptor> manifestsToCopy;
            Index indexToPush;
            if (options.platformFilter() != null) {
                List<ManifestDescriptor> filtered = index.getManifests().stream()
                        .filter(d ->
                                options.platformFilter().stream().anyMatch(p -> Platform.matches(d.getPlatform(), p)))
                        .toList();
                if (filtered.isEmpty()) {
                    throw new OrasException(
                            "No manifests found in index matching platform filter: " + options.platformFilter());
                }
                manifestsToCopy = filtered;
                indexToPush = index.withManifests(filtered);
                LOG.debug(
                        "Platform filter {} matched {}/{} manifests",
                        options.platformFilter(),
                        filtered.size(),
                        index.getManifests().size());
            } else {
                manifestsToCopy = index.getManifests();
                indexToPush = index;
            }

            // Write all manifests and their config
            for (ManifestDescriptor manifestDescriptor : manifestsToCopy) {

                // Copy manifest
                if (source.isManifestMediaType(manifestDescriptor.getMediaType())) {
                    Manifest manifest =
                            source.getManifest(effectiveSourceRef.withDigest(manifestDescriptor.getDigest()));

                    // Copy all layers for this manifest
                    copyLayers(
                            source,
                            effectiveSourceRef.withDigest(manifestDescriptor.getDigest()),
                            target,
                            effectiveTargetRef,
                            manifestDescriptor.getMediaType());

                    // Push config
                    copyConfig(manifest, source, effectiveSourceRef, target, effectiveTargetRef);

                    // Push the manifest
                    LOG.debug("Copying nested manifest {}", manifestDescriptor.getDigest());
                    Manifest pushedManifest = target.pushManifest(
                            effectiveTargetRef.withDigest(manifest.getDigest()),
                            manifest.withDescriptor(manifestDescriptor));
                    LOG.debug("Copied nested manifest {}", manifestDescriptor.getDigest());

                } else if (source.isIndexMediaType(manifestDescriptor.getMediaType())) {
                    // Copy index of index
                    LOG.debug("Copying nested index {}", manifestDescriptor.getDigest());
                    copy(
                            source,
                            effectiveSourceRef.withDigest(manifestDescriptor.getDigest()),
                            target,
                            effectiveTargetRef.withDigest(manifestDescriptor.getDigest()),
                            options,
                            visited,
                            depth + 1);
                    LOG.debug("Copied nested index {}", manifestDescriptor.getDigest());
                }
            }

            LOG.debug("Copying index {}", manifestDigest);
            Index pushedIndex = target.pushIndex(effectiveTargetRef.withDigest(targetTag), indexToPush);
            LOG.debug("Copied index {} with tag {}", pushedIndex, targetTag);

        } else {
            throw new OrasException("Unsupported content type: %s".formatted(contentType));
        }
    }

    @SuppressWarnings("unchecked")
    private static <
                    SourceRefType extends Ref<@NonNull SourceRefType>,
                    TargetRefType extends Ref<@NonNull TargetRefType>>
            boolean canMount(
                    OCI<SourceRefType> source,
                    SourceRefType sourceRef,
                    OCI<TargetRefType> target,
                    TargetRefType targetRef) {
        if (!source.getClass().equals(target.getClass())) {
            return false;
        }
        // Safe due to class comparison before
        return source.canMount(target, sourceRef, (SourceRefType) targetRef);
    }

    @SuppressWarnings("unchecked")
    private static <
                    SourceRefType extends Ref<@NonNull SourceRefType>,
                    TargetRefType extends Ref<@NonNull TargetRefType>>
            void copyConfig(
                    Manifest manifest,
                    OCI<SourceRefType> source,
                    SourceRefType sourceRef,
                    OCI<TargetRefType> target,
                    TargetRefType targetRef) {
        // Write config as any blob
        LOG.debug("Copying config {}", manifest.getConfig().getDigest());
        Config config = manifest.getConfig();
        Objects.requireNonNull(config.getDigest(), "Config digest is required for streaming copy");
        Objects.requireNonNull(config.getSize(), "Config size is required for streaming copy");
        TargetRefType configTargetRef =
                targetRef.forTarget(target).withDigest(manifest.getConfig().getDigest());
        if (canMount(source, sourceRef, target, targetRef)) {
            target.mountBlob((TargetRefType) sourceRef.withDigest(config.getDigest()), configTargetRef);
            LOG.debug(
                    "Copied config (mounted from {}) {}",
                    sourceRef.getRepository(),
                    manifest.getConfig().getDigest());
            return;
        }
        target.pushBlob(
                configTargetRef,
                config.getSize(),
                () -> source.pullConfig(sourceRef, manifest.getConfig()),
                config.getAnnotations());
        LOG.debug("Copied config {}", manifest.getConfig().getDigest());
    }
}
