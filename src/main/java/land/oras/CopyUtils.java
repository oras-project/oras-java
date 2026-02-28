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

import java.util.Objects;
import land.oras.exception.OrasException;
import org.jspecify.annotations.NonNull;
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
     * @param recursive Recursive copy that include index of index. If false will copy manifests media type when encountered inside index
     */
    public record CopyOptions(boolean includeReferrers, boolean recursive) {

        /**
         * The default copy options with includeReferrers and recursive set to false.
         * @return The default copy options
         */
        public static CopyOptions shallow() {
            return new CopyOptions(false, false);
        }

        /**
         * The copy options with includeReferrers and recursive set to true.
         * @return The copy options with includeReferrers and recursive set to true
         */
        public static CopyOptions deep() {
            return new CopyOptions(true, true);
        }

        /**
         * Create a new copy options with default values.
         * @param includeReferrers Whether to include referrers in the copy
         * @return The copy options
         */
        public CopyOptions withIncludeReferrers(boolean includeReferrers) {
            return new CopyOptions(includeReferrers, this.recursive);
        }

        /**
         * Create a new copy options with default values.
         * @param recursive Recursive copy that include index of index. If false will copy manifests media type when encountered inside index
         * @return The copy options
         */
        public CopyOptions withRecursive(boolean recursive) {
            return new CopyOptions(this.includeReferrers, recursive);
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
        copy(source, sourceRef, target, targetRef, CopyOptions.shallow().withRecursive(recursive));
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

        boolean recursive = options.recursive();
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

        // Write all layer
        for (Layer layer : source.collectLayers(effectiveSourceRef, contentType, true, false)) {
            Objects.requireNonNull(layer.getDigest(), "Layer digest is required for streaming copy");
            Objects.requireNonNull(layer.getSize(), "Layer size is required for streaming copy");
            LOG.debug("Copying layer {}", layer.getDigest());
            target.pushBlob(
                    effectiveTargetRef.withDigest(layer.getDigest()),
                    layer.getSize(),
                    () -> source.fetchBlob(effectiveSourceRef.withDigest(layer.getDigest())),
                    layer.getAnnotations());
            LOG.debug("Copied layer {}", layer.getDigest());
        }

        // Single manifest
        if (source.isManifestMediaType(contentType)) {

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

            // Write all manifests and their config
            for (ManifestDescriptor manifestDescriptor : index.getManifests()) {

                if (!recursive && source.isIndexMediaType(manifestDescriptor.getMediaType())) {
                    LOG.debug(
                            "Encountered index media type {} in index {}, skipping manifest {} due to non-recursive copy",
                            manifestDescriptor.getMediaType(),
                            manifestDigest,
                            manifestDescriptor.getDigest());
                    index = index.removeDescriptor(manifestDescriptor);
                    continue;
                }

                // Copy manifest
                if (source.isManifestMediaType(manifestDescriptor.getMediaType())) {
                    Manifest manifest =
                            source.getManifest(effectiveSourceRef.withDigest(manifestDescriptor.getDigest()));

                    // Push config
                    copyConfig(manifest, source, effectiveSourceRef, target, effectiveTargetRef);

                    // Push the manifest
                    LOG.debug("Copying nested manifest {}", manifestDigest);
                    target.pushManifest(
                            effectiveTargetRef.withDigest(manifest.getDigest()),
                            manifest.withDescriptor(manifestDescriptor));
                    LOG.debug("Copied nested manifest {}", manifestDigest);
                } else if (source.isIndexMediaType(manifestDescriptor.getMediaType())) {
                    // Copy index of index
                    LOG.debug("Copying nested index {}", manifestDigest);
                    copy(
                            source,
                            effectiveSourceRef.withDigest(manifestDescriptor.getDigest()),
                            target,
                            effectiveTargetRef.withDigest(manifestDescriptor.getDigest()),
                            options);
                    LOG.debug("Copied nested index {}", manifestDigest);
                }
            }

            LOG.debug("Copying index {}", manifestDigest);
            index = target.pushIndex(effectiveTargetRef.withDigest(tag), index);
            LOG.debug("Copied index {}", manifestDigest);

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
