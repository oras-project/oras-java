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

package land.oras;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import land.oras.utils.SupportedAlgorithm;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A referer of a container on a {@link Registry}.
 */
@NullMarked
@OrasModel
public final class ContainerRef extends Ref<ContainerRef> {

    /**
     * The regex pattern to parse the container name including the registry, namespace, repository, tag and digest.
     */
    private static final Pattern NAME_REGEX = Pattern.compile(
            "(?:([^/@]+[.:][^/@]*)/)?" // registry
                    + "((?:[^:@/]+/)+)?" // namespace
                    + "([^:@/]+)" // repository
                    + "(?::([^:@]+))?" // tag
                    + "(?:@(.+))?" // digest
                    + "$");

    /**
     * The registry where the container is stored.
     */
    private final String registry;

    /**
     * The repository where the container is stored.
     */
    private final String repository;

    /**
     * The namespace of the container.
     */
    private final @Nullable String namespace;

    /**
     * The digest of the container.
     */
    private final @Nullable String digest;

    /**
     * Whether the container reference is unqualified without registry
     */
    private final boolean unqualified;

    /**
     * Private constructor
     * @param registry The registry where the container is stored.
     * @param unqualified Whether the container reference is unqualified without registry
     * @param namespace The namespace of the container.
     * @param repository The repository where the container is stored
     * @param tag The tag of the container.
     * @param digest The digest of the container.
     */
    private ContainerRef(
            String registry,
            boolean unqualified,
            @Nullable String namespace,
            String repository,
            String tag,
            @Nullable String digest) {
        super(tag);
        this.unqualified = unqualified;
        this.registry = registry;
        this.namespace = namespace;
        this.repository = repository;
        this.digest = digest;
    }

    /**
     * Create a new container reference
     * @return The new container reference
     */
    public String getRegistry() {
        return registry;
    }

    /**
     * Get the effective registry based on given target
     * @param target The target registry
     * @return The effective registry
     */
    public String getEffectiveRegistry(Registry target) {
        if (isUnqualified()) {
            if (target.getRegistry() != null) {
                return target.getRegistry();
            }
        }
        return registry;
    }

    /**
     * Whether the container reference is unqualified without registry
     * @return True if unqualified
     */
    public boolean isUnqualified() {
        return unqualified;
    }

    /**
     * Get the full repository name including the namespace if any
     * @param registry The registry
     * @return The full repository name
     */
    public String getFullRepository(@Nullable Registry registry) {
        String namespace = getNamespace(registry);
        if (namespace != null) {
            return "%s/%s".formatted(namespace, repository);
        }
        return repository;
    }

    /**
     * Get the full repository name including the namespace if any
     * @return The full repository name
     */
    public String getFullRepository() {
        return getFullRepository(null);
    }

    /**
     * Get the API registry
     * @param target The target registry
     * @return The API registry
     */
    public String getApiRegistry(@Nullable Registry target) {
        String registry = target != null && target.getRegistry() != null ? target.getRegistry() : getRegistry();
        if (registry.equals("docker.io")) {
            return "registry-1.docker.io";
        }
        return registry;
    }

    /**
     * Get the API registry
     * @return The API registry
     */
    public String getApiRegistry() {
        return getApiRegistry(null);
    }

    /**
     * Get the namespace
     * @return The namespace
     */
    public @Nullable String getNamespace() {
        String registry = getRegistry();
        if (namespace == null && registry.equals("docker.io")) {
            return "library";
        }
        return namespace;
    }

    /**
     * Get the effective namespace based on given registry traget
     * @param target The target registry
     * @return The effective namespace
     */
    public @Nullable String getNamespace(@Nullable Registry target) {
        if (target == null || target.getRegistry() == null) {
            return getNamespace();
        }
        if (namespace == null && target.getRegistry().equals("docker.io")) {
            return "library";
        }
        return namespace;
    }

    /**
     * Get the repository
     * @return The repository
     */
    public String getRepository() {
        return repository;
    }

    /**
     * Get the digest
     * @return The digest
     */
    public @Nullable String getDigest() {
        return digest;
    }

    @Override
    public ContainerRef withDigest(String digest) {
        return new ContainerRef(registry, unqualified, namespace, repository, tag, digest);
    }

    @Override
    public SupportedAlgorithm getAlgorithm() {
        // Default if not set
        if (digest == null) {
            return SupportedAlgorithm.getDefault();
        }
        // See https://github.com/opencontainers/image-spec/blob/main/descriptor.md#digests
        return SupportedAlgorithm.fromDigest(digest);
    }

    /**
     * Get the API prefix
     * @param target The target registry
     * @return The API prefix
     */
    private String getApiPrefix(@Nullable Registry target) {
        String namespace = getNamespace(target);
        if (namespace != null) {
            return "%s/v2/%s/%s".formatted(getApiRegistry(target), namespace, repository);
        }
        return "%s/v2/%s".formatted(getApiRegistry(target), repository);
    }

    /**
     * Return the catalog repositories URL
     * @param target The target registry
     * @return The tag URL
     */
    public String getRepositoriesPath(@Nullable Registry target) {
        return "%s/v2/_catalog".formatted(getApiRegistry(target));
    }

    /**
     * Return the catalog repositories URL
     * @return The tag URL
     */
    public String getRepositoriesPath() {
        return getRepositoriesPath(null);
    }

    /**
     * Return the tag URL
     * @param target The target registry
     * @return The tag URL
     */
    public String getTagsPath(@Nullable Registry target) {
        return "%s/tags/list".formatted(getApiPrefix(target));
    }

    /**
     * Return the tag URL
     * @return The tag URL
     */
    public String getTagsPath() {
        return getTagsPath(null);
    }

    /**
     * Return the referrers URL for this container referrer
     * @param artifactType The optional artifact type
     * @param registry The optional registry
     * @return The referrers URL
     */
    public String getReferrersPath(@Nullable Registry registry, @Nullable ArtifactType artifactType) {
        if (artifactType == null) {
            return "%s/referrers/%s".formatted(getApiPrefix(registry), digest);
        }
        return "%s/referrers/%s?artifactType=%s"
                .formatted(
                        getApiPrefix(registry),
                        digest,
                        URLEncoder.encode(artifactType.toString(), StandardCharsets.UTF_8));
    }

    /**
     * Return the referrers URL for this container referrer
     * @param artifactType The optional artifact type
     * @return The referrers URL
     */
    public String getReferrersPath(@Nullable ArtifactType artifactType) {
        return getReferrersPath(null, artifactType);
    }

    /**
     * Return the manifests URL
     * @param registry The registry
     * @return The manifests URL
     */
    public String getManifestsPath(@Nullable Registry registry) {
        return "%s/manifests/%s".formatted(getApiPrefix(registry), digest == null ? tag : digest);
    }

    /**
     * Return the manifests URL
     * @return The manifests URL
     */
    public String getManifestsPath() {
        return getManifestsPath(null);
    }

    /**
     * Return the blobs upload URL with the digest for single POST upload
     * @param registry The registry
     * @return The blobs upload URL
     */
    public String getBlobsUploadDigestPath(Registry registry) {
        if (digest == null) {
            throw new OrasException("You are required to include a digest");
        }
        return "%s/blobs/uploads/?digest=%s".formatted(getApiPrefix(registry), digest);
    }

    /**
     * Return the blobs URL
     * @param registry The registry
     * @return The blobs URL
     */
    public String getBlobsPath(@Nullable Registry registry) {
        if (digest == null) {
            throw new OrasException("You are required to include a digest");
        }
        return "%s/blobs/%s".formatted(getApiPrefix(registry), digest);
    }

    /**
     * Parse the container name into registry, repository and tag.
     * @param name The full name of the container to parse with any components.
     * @return The container object with the registry, repository and tag.
     */
    public static ContainerRef parse(String name) {

        // Strip prefix http:// or https:// or oci://
        name = name.replaceAll("^(http://|https://|oci://)", "");

        Matcher matcher = NAME_REGEX.matcher(name);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid container name format");
        }

        // Extract the parts of the container name
        String registry = matcher.group(1);
        String namespace = matcher.group(2);
        String repository = matcher.group(3);
        String tag = matcher.group(4);
        String digest = matcher.group(5);
        boolean unqualified = false;
        if (repository == null) {
            throw new IllegalArgumentException("You are minimally required to include a <namespace>/<repository>");
        }
        if (registry == null) {
            registry = Const.DEFAULT_REGISTRY;
            unqualified = true;
        }
        if (tag == null) {
            tag = Const.DEFAULT_TAG;
        }
        // Strip the trailing slash from the namespace
        if (namespace != null) {
            namespace = namespace.substring(0, namespace.length() - 1);
        }

        // Validate digest algorithm
        if (digest != null) {
            SupportedAlgorithm.fromDigest(digest);
        }

        return new ContainerRef(registry, unqualified, namespace, repository, tag, digest);
    }

    /**
     * Return a copy of reference for a registry other registry
     * @param registry The registry
     * @return The container reference
     */
    public ContainerRef forRegistry(String registry) {
        return new ContainerRef(registry, false, namespace, repository, tag, digest);
    }

    /**
     * Return a copy of reference for a registry other registry
     * @param registry The registry
     * @return The container reference
     */
    public ContainerRef forRegistry(Registry registry) {
        return new ContainerRef(
                registry.getRegistry() != null ? registry.getRegistry() : this.registry,
                false, // not unqualified if registry is set
                namespace,
                repository,
                tag,
                digest);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ContainerRef that = (ContainerRef) o;
        return Objects.equals(getRegistry(), that.getRegistry())
                && Objects.equals(getRepository(), that.getRepository())
                && Objects.equals(getNamespace(), that.getNamespace())
                && Objects.equals(getDigest(), that.getDigest())
                && Objects.equals(getTag(), that.getTag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRegistry(), getRepository(), getNamespace(), getDigest(), getTag());
    }

    @Override
    public String toString() {
        if (namespace != null && !namespace.isEmpty()) {
            return "%s/%s/%s:%s%s".formatted(registry, namespace, repository, tag, digest != null ? "@" + digest : "");
        }
        return "%s/%s:%s%s".formatted(registry, repository, tag, digest != null ? "@" + digest : "");
    }
}
