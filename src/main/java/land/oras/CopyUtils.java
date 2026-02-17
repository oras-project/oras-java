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

import java.io.IOException;
import java.io.InputStream;
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
     * Copy a container from source to target.
     * @param source The source OCI
     * @param sourceRef The source reference
     * @param target The target OCI
     * @param targetRef The target reference
     * @param recursive Whether to copy referrers recursively
     * @param <SourceRefType> The source reference type
     * @param <TargetRefType> The target reference type
     */
    public static <SourceRefType extends Ref<@NonNull SourceRefType>, TargetRefType extends Ref<@NonNull TargetRefType>>
            void copy(
                    OCI<SourceRefType> source,
                    SourceRefType sourceRef,
                    OCI<TargetRefType> target,
                    TargetRefType targetRef,
                    boolean recursive) {

        try {

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

            // Write all layer
            for (Layer layer : source.collectLayers(sourceRef, contentType, true)) {
                Objects.requireNonNull(layer.getDigest(), "Layer digest is required for streaming copy");
                Objects.requireNonNull(layer.getSize(), "Layer size is required for streaming copy");
                LOG.debug("Copying layer {}", layer.getDigest());
                target.pushBlob(
                        targetRef.forTarget(effectiveTargetRegistry).withDigest(layer.getDigest()),
                        layer.getSize(),
                        () -> source.fetchBlob(
                                sourceRef.forTarget(resolveSourceRegistry).withDigest(layer.getDigest())),
                        layer.getAnnotations());
                LOG.debug("Copied layer {}", layer.getDigest());
            }

            // Single manifest
            if (source.isManifestMediaType(contentType)) {

                // Write manifest as any blob
                Manifest manifest = source.getManifest(sourceRef);
                String tag = sourceRef.getTag();

                Objects.requireNonNull(manifest.getDigest(), "Manifest digest is required for streaming copy");

                // Write config as any blob
                Config config = manifest.getConfig();
                Objects.requireNonNull(config.getDigest(), "Config digest is required for streaming copy");
                Objects.requireNonNull(config.getSize(), "Config size is required for streaming copy");
                target.pushBlob(
                        targetRef
                                .forTarget(effectiveTargetRegistry)
                                .withDigest(manifest.getConfig().getDigest()),
                        config.getSize(),
                        () -> source.pullConfig(sourceRef.forTarget(resolveSourceRegistry), manifest.getConfig()),
                        config.getAnnotations());

                // Push the manifest
                LOG.debug("Copying manifest {}", manifestDigest);
                target.pushManifest(targetRef.withDigest(tag), manifest);
                LOG.debug("Copied manifest {}", manifestDigest);

                if (recursive) {
                    LOG.debug("Recursively copy referrers");
                    Referrers referrers = source.getReferrers(sourceRef.withDigest(manifestDigest), null);
                    for (ManifestDescriptor referer : referrers.getManifests()) {
                        LOG.info("Copy reference {}", referer.getDigest());
                        copy(source, sourceRef.withDigest(referer.getDigest()), target, targetRef, recursive);
                    }
                }

            }
            // Index
            else if (source.isIndexMediaType(contentType)) {

                Index index = source.getIndex(sourceRef);
                String tag = sourceRef.getTag();

                // Write all manifests and their config
                for (ManifestDescriptor manifestDescriptor : index.getManifests()) {
                    Manifest manifest = source.getManifest(sourceRef.withDigest(manifestDescriptor.getDigest()));

                    // Write config as any blob
                    try (InputStream is = source.pullConfig(sourceRef, manifest.getConfig())) {
                        LOG.debug("Copying config blob {}", manifest.getConfig().getDigest());
                        target.pushBlob(
                                targetRef.withDigest(manifest.getConfig().getDigest()), is);
                        LOG.debug("Copied config blob {}", manifest.getConfig().getDigest());
                    }

                    // Push the manifest
                    target.pushManifest(
                            targetRef.withDigest(manifest.getDigest()), manifest.withDescriptor(manifestDescriptor));
                }

                LOG.debug("Copying index {}", manifestDigest);
                target.pushIndex(targetRef.withDigest(tag), index);
                LOG.debug("Copied index {}", manifestDigest);

            } else {
                throw new OrasException("Unsupported content type: %s".formatted(contentType));
            }

        } catch (IOException e) {
            throw new OrasException("Failed to copy container", e);
        }
    }
}
