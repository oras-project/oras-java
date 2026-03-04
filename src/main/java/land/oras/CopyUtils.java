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

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
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
     * Private constructor
     */
    private CopyUtils() {
        // Utils class
    }

    /**
     * Options for copy.
     * @param includeReferrers Whether to include referrers in the copy
     * @param targetPlatforms List of platforms to filter by. If null or empty, all platforms are included.
     * @param includeUnspecifiedPlatforms Whether to include manifests without platform information when platform filtering is active
     */
    public record CopyOptions(
            boolean includeReferrers, @Nullable List<Platform> targetPlatforms, boolean includeUnspecifiedPlatforms) {

        /**
         * The default copy options with includeReferrers to false
         * @return The default copy options
         */
        public static CopyOptions shallow() {
            return new CopyOptions(false, null, true);
        }

        /**
         * The copy options with includeReferrers and recursive set to true.
         * @return The copy options with includeReferrers and recursive set to true
         */
        public static CopyOptions deep() {
            return new CopyOptions(true, null, true);
        }

        /**
         * Copy options with platform filtering
         * <p>
         * Example usage:
         * <pre>{@code
         * List<Platform> platforms = List.of(Platform.linuxAmd64(), Platform.windowsAmd64());
         * CopyOptions options = CopyOptions.withPlatforms(false, platforms);
         * CopyUtils.copy(source, sourceRef, target, targetRef, options);
         * }</pre>
         *
         * @param includeReferrers Whether to include referrers in the copy
         * @param targetPlatforms List of platforms to filter by
         * @return The copy options with platform filtering
         */
        public static CopyOptions withPlatforms(boolean includeReferrers, List<Platform> targetPlatforms) {
            return new CopyOptions(includeReferrers, targetPlatforms, false);
        }

        /**
         * Copy options with platform filtering including unspecified platforms
         * @param includeReferrers Whether to include referrers in the copy
         * @param targetPlatforms List of platforms to filter by
         * @param includeUnspecifiedPlatforms Whether to include manifests without platform information
         * @return The copy options with platform filtering
         */
        public static CopyOptions withPlatforms(
                boolean includeReferrers, List<Platform> targetPlatforms, boolean includeUnspecifiedPlatforms) {
            return new CopyOptions(includeReferrers, targetPlatforms, includeUnspecifiedPlatforms);
        }

        /**
         * Check if platform filtering is enabled
         * @return true if platform filtering is enabled
         */
        public boolean isPlatformFilteringEnabled() {
            return targetPlatforms != null && !targetPlatforms.isEmpty();
        }

        /**
         * Check if a manifest should be included based on its platform
         * @param manifestDescriptor The manifest descriptor to check
         * @return true if the manifest should be included
         */
        public boolean shouldIncludeManifest(ManifestDescriptor manifestDescriptor) {
            if (!isPlatformFilteringEnabled()) {
                return true;
            }

            Platform manifestPlatform = manifestDescriptor.getPlatform();

            // Handle unspecified platforms
            if (Platform.unspecified(manifestPlatform)) {
                return includeUnspecifiedPlatforms;
            }

            // Check if manifest platform matches any target platform
            return targetPlatforms.stream()
                    .anyMatch(targetPlatform -> Platform.matches(manifestPlatform, targetPlatform));
        }
    }

    /**
     * Copy a container from source to target.
     * @deprecated Use {@link #copy(OCI, Ref, OCI, Ref, CopyOptions)} instead. This method will be removed in a future release.
     * @param source The source OCI
     * @param sourceRef The source reference
     * @param target The target OCI
     * @param targetRef The target reference
     * @param recursive Copy refferers
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
    private static <
                    SourceRefType extends Ref<@NonNull SourceRefType>,
                    TargetRefType extends Ref<@NonNull TargetRefType>>
            void copyLayers(
                    OCI<SourceRefType> source,
                    SourceRefType sourceRef,
                    OCI<TargetRefType> target,
                    TargetRefType targetRef,
                    String contentType) {
        for (Layer layer : source.collectLayers(sourceRef, contentType, true)) {
            Objects.requireNonNull(layer.getDigest(), "Layer digest is required for streaming copy");
            Objects.requireNonNull(layer.getSize(), "Layer size is required for streaming copy");
            LOG.debug("Copying layer {}", layer.getDigest());
            target.pushBlob(
                    targetRef.withDigest(layer.getDigest()),
                    layer.getSize(),
                    () -> source.fetchBlob(sourceRef.withDigest(layer.getDigest())),
                    layer.getAnnotations());
            LOG.debug("Copied layer {}", layer.getDigest());
        }
    }

    /**
     * Copy a container from source to target.
     * <p>
     * This method supports platform-specific copying when configured in the options.
     * When platform filtering is enabled, only manifests matching the target platforms
     * will be copied, which saves bandwidth and storage space.
     *
     * @param source The source OCI
     * @param sourceRef The source reference
     * @param target The target OCI
     * @param targetRef The target reference
     * @param options The copy options, including platform filtering configuration
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

        boolean includeReferrers = options.includeReferrers();

        Descriptor descriptor = source.probeDescriptor(sourceRef);

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

        // Single manifest
        if (source.isManifestMediaType(contentType)) {

            // Write all layers
            copyLayers(source, effectiveSourceRef, target, effectiveTargetRef, contentType);

            // Write manifest as any blob
            Manifest manifest = source.getManifest(effectiveSourceRef);
            String tag = effectiveSourceRef.getTag();

            Objects.requireNonNull(manifest.getDigest(), "Manifest digest is required for streaming copy");

            // Push config
            copyConfig(manifest, source, effectiveSourceRef, target, effectiveTargetRef);

            // Push the manifest
            LOG.debug("Copying manifest {}", manifestDigest);
            target.pushManifest(effectiveTargetRef.withDigest(tag), manifest);
            LOG.debug("Copied manifest {}", manifestDigest);

            if (includeReferrers) {
                LOG.debug("Including referrers on copy of manifest {}", manifestDigest);
                Referrers referrers = source.getReferrers(effectiveSourceRef.withDigest(manifestDigest), null);
                for (ManifestDescriptor referer : referrers.getManifests()) {
                    LOG.debug("Copy reference from referrers {}", referer.getDigest());
                    copy(
                            source,
                            effectiveSourceRef.withDigest(referer.getDigest()),
                            target,
                            effectiveTargetRef,
                            options);
                }
            } else {
                LOG.debug("Not including referrers on copy of manifest {}", manifestDigest);
            }

        }
        // Index
        else if (source.isIndexMediaType(contentType)) {

            Index index = source.getIndex(effectiveSourceRef);
            String tag = effectiveSourceRef.getTag();

            List<ManifestDescriptor> copiedManifests = new LinkedList<>();

            // Write all manifests and their config
            for (ManifestDescriptor manifestDescriptor : index.getManifests()) {

                // Skip manifest if platform filtering is enabled and manifest doesn't match
                if (!options.shouldIncludeManifest(manifestDescriptor)) {
                    LOG.debug(
                            "Skipping manifest {} due to platform filter. Manifest platform: {}, Target platforms: {}",
                            manifestDescriptor.getDigest(),
                            manifestDescriptor.getPlatform(),
                            options.targetPlatforms());
                    continue;
                }

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

                    // Add to copied manifests
                    copiedManifests.add(manifestDescriptor);

                } else if (source.isIndexMediaType(manifestDescriptor.getMediaType())) {
                    // Copy index of index
                    LOG.debug("Copying nested index {}", manifestDescriptor.getDigest());
                    copy(
                            source,
                            effectiveSourceRef.withDigest(manifestDescriptor.getDigest()),
                            target,
                            effectiveTargetRef.withDigest(manifestDescriptor.getDigest()),
                            options);
                    LOG.debug("Copied nested index {}", manifestDescriptor.getDigest());

                    // Add to copied manifests
                    copiedManifests.add(manifestDescriptor);
                }
            }

            LOG.debug("Copying index {}", manifestDigest);
            // Create filtered index with only copied manifests if platform filtering was applied
            Index indexToPush = index;
            if (options.isPlatformFilteringEnabled()
                    && copiedManifests.size() != index.getManifests().size()) {
                LOG.debug(
                        "Platform filtering applied: copied {} of {} manifests",
                        copiedManifests.size(),
                        index.getManifests().size());

                // Create filtered index by removing non-matching manifests
                indexToPush = index;
                for (ManifestDescriptor manifestDescriptor : index.getManifests()) {
                    if (!options.shouldIncludeManifest(manifestDescriptor)) {
                        indexToPush = indexToPush.withRemovedDescriptor(manifestDescriptor);
                    }
                }
            }
            Index pushedIndex = target.pushIndex(effectiveTargetRef.withDigest(tag), indexToPush);
            LOG.debug("Copied index {} with tag {}", pushedIndex, tag);

        } else {
            throw new OrasException("Unsupported content type: %s".formatted(contentType));
        }
    }

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
        target.pushBlob(
                targetRef.forTarget(target).withDigest(manifest.getConfig().getDigest()),
                config.getSize(),
                () -> source.pullConfig(sourceRef, manifest.getConfig()),
                config.getAnnotations());
        LOG.debug("Copied config {}", manifest.getConfig().getDigest());
    }
}
