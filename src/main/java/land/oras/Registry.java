/*-
 * =LICENSE=
 * ORAS Java SDK
 * ===
 * Copyright (C) 2024 - 2025 ORAS
 * ===
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import land.oras.auth.AuthProvider;
import land.oras.auth.AuthStoreAuthenticationProvider;
import land.oras.auth.BearerTokenProvider;
import land.oras.auth.NoAuthProvider;
import land.oras.auth.UsernamePasswordProvider;
import land.oras.exception.OrasException;
import land.oras.utils.ArchiveUtils;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import land.oras.utils.OrasHttpClient;
import land.oras.utils.SupportedAlgorithm;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A registry is the main entry point for interacting with a container registry
 */
@NullMarked
public final class Registry extends OCI<ContainerRef> {

    /**
     * The chunk size for uploading blobs (5MB)
     * This is a standard chunk size commonly used in cloud storage systems to balance
     * network performance with memory usage. The actual chunk size used may be larger
     * if the registry specifies a minimum chunk size via the OCI-Chunk-Min-Length header.
     */
    private static final int CHUNK_SIZE = 5 * 1024 * 1024;

    /**
     * The digest calculation limit (16MB)
     * For files smaller than this size, we compute the digest before starting the upload
     * to check if the blob already exists in the registry, potentially avoiding unnecessary uploads.
     */
    private static final int DIGEST_CALCULATION_LIMIT = 16 * 1024 * 1024;

    /**
     * The HTTP client
     */
    private OrasHttpClient client;

    /**
     * The auth provider
     */
    private AuthProvider authProvider;

    /**
     * Insecure. Use HTTP instead of HTTPS
     */
    private boolean insecure;

    /**
     * Skip TLS verification
     */
    private boolean skipTlsVerify;

    /**
     * Constructor
     */
    private Registry() {
        this.authProvider = new NoAuthProvider();
        this.client = OrasHttpClient.Builder.builder().build();
    }

    /**
     * Return this registry with insecure flag
     * @param insecure Insecure
     */
    private void setInsecure(boolean insecure) {
        this.insecure = insecure;
    }

    /**
     * Return this registry with skip TLS verification
     * @param skipTlsVerify Skip TLS verification
     */
    private void setSkipTlsVerify(boolean skipTlsVerify) {
        this.skipTlsVerify = skipTlsVerify;
    }

    /**
     * Return this registry with auth provider
     * @param authProvider The auth provider
     */
    private void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
        client.updateAuthentication(authProvider);
    }

    /**
     * Build the provider
     * @return The provider
     */
    private Registry build() {
        client = OrasHttpClient.Builder.builder()
                .withAuthentication(authProvider)
                .withSkipTlsVerify(skipTlsVerify)
                .build();
        return this;
    }

    /**
     * Get the HTTP scheme depending on the insecure flag
     * @return The scheme
     */
    public String getScheme() {
        return insecure ? "http" : "https";
    }

    /**
     * Get the tags of a container
     * @param containerRef The container
     * @return The tags
     */
    public List<String> getTags(ContainerRef containerRef) {
        URI uri = URI.create("%s://%s".formatted(getScheme(), containerRef.getTagsPath()));
        OrasHttpClient.ResponseWrapper<String> response =
                client.get(uri, Map.of(Const.ACCEPT_HEADER, Const.DEFAULT_JSON_MEDIA_TYPE));
        if (switchTokenAuth(containerRef, response)) {
            response = client.get(uri, Map.of(Const.ACCEPT_HEADER, Const.DEFAULT_JSON_MEDIA_TYPE));
        }
        handleError(response);
        return JsonUtils.fromJson(response.response(), Tags.class).tags();
    }

    /**
     * Get the referrers of a container
     * @param containerRef The container
     * @param artifactType The optional artifact type
     * @return The referrers
     */
    public Referrers getReferrers(ContainerRef containerRef, @Nullable ArtifactType artifactType) {
        if (containerRef.getDigest() == null) {
            throw new OrasException("Digest is required to get referrers");
        }
        URI uri = URI.create("%s://%s".formatted(getScheme(), containerRef.getReferrersPath(artifactType)));
        OrasHttpClient.ResponseWrapper<String> response =
                client.get(uri, Map.of(Const.ACCEPT_HEADER, Const.DEFAULT_INDEX_MEDIA_TYPE));
        if (switchTokenAuth(containerRef, response)) {
            response = client.get(uri, Map.of(Const.ACCEPT_HEADER, Const.DEFAULT_INDEX_MEDIA_TYPE));
        }
        handleError(response);
        return JsonUtils.fromJson(response.response(), Referrers.class);
    }

    /**
     * Delete a manifest
     * @param containerRef The artifact
     */
    public void deleteManifest(ContainerRef containerRef) {
        URI uri = URI.create("%s://%s".formatted(getScheme(), containerRef.getManifestsPath()));
        OrasHttpClient.ResponseWrapper<String> response = client.delete(uri, Map.of());
        logResponse(response);
        if (switchTokenAuth(containerRef, response)) {
            response = client.delete(uri, Map.of());
            logResponse(response);
        }
        handleError(response);
    }

    @Override
    public Manifest pushManifest(ContainerRef containerRef, Manifest manifest) {

        Map<String, String> annotations = manifest.getAnnotations();
        if (!annotations.containsKey(Const.ANNOTATION_CREATED) && containerRef.getDigest() == null) {
            Map<String, String> manifestAnnotations = new HashMap<>(annotations);
            manifestAnnotations.put(Const.ANNOTATION_CREATED, Const.currentTimestamp());
            manifest = manifest.withAnnotations(manifestAnnotations);
        }
        URI uri = URI.create("%s://%s".formatted(getScheme(), containerRef.getManifestsPath()));
        byte[] manifestData = manifest.getJson() != null
                ? manifest.getJson().getBytes()
                : manifest.toJson().getBytes();
        OrasHttpClient.ResponseWrapper<String> response =
                client.put(uri, manifestData, Map.of(Const.CONTENT_TYPE_HEADER, Const.DEFAULT_MANIFEST_MEDIA_TYPE));
        if (switchTokenAuth(containerRef, response)) {
            response =
                    client.put(uri, manifestData, Map.of(Const.CONTENT_TYPE_HEADER, Const.DEFAULT_MANIFEST_MEDIA_TYPE));
        }
        logResponse(response);
        handleError(response);
        if (manifest.getSubject() != null) {
            // https://github.com/opencontainers/distribution-spec/blob/main/spec.md#pushing-manifests-with-subject
            if (!response.headers().containsKey(Const.OCI_SUBJECT_HEADER.toLowerCase())) {
                throw new OrasException(
                        "Subject was set on manifest but not OCI subject header was returned. Legecy flow not implemented");
            }
        }
        return getManifest(containerRef);
    }

    /**
     * Push a manifest
     * @param containerRef The container
     * @param index The index
     * @return The location
     */
    public Index pushIndex(ContainerRef containerRef, Index index) {
        URI uri = URI.create("%s://%s".formatted(getScheme(), containerRef.getManifestsPath()));
        OrasHttpClient.ResponseWrapper<String> response = client.put(
                uri,
                JsonUtils.toJson(index).getBytes(),
                Map.of(Const.CONTENT_TYPE_HEADER, Const.DEFAULT_INDEX_MEDIA_TYPE));
        if (switchTokenAuth(containerRef, response)) {
            response = client.put(
                    uri,
                    JsonUtils.toJson(index).getBytes(),
                    Map.of(Const.CONTENT_TYPE_HEADER, Const.DEFAULT_INDEX_MEDIA_TYPE));
        }
        logResponse(response);
        handleError(response);
        return getIndex(containerRef);
    }

    /**
     * Delete a blob
     * @param containerRef The container
     */
    public void deleteBlob(ContainerRef containerRef) {
        URI uri = URI.create("%s://%s".formatted(getScheme(), containerRef.getBlobsPath()));
        OrasHttpClient.ResponseWrapper<String> response = client.delete(uri, Map.of());
        logResponse(response);
        // Switch to bearer auth if needed and retry first request
        if (switchTokenAuth(containerRef, response)) {
            response = client.delete(uri, Map.of());
            logResponse(response);
        }
        handleError(response);
    }

    @Override
    public void pullArtifact(ContainerRef containerRef, Path path, boolean overwrite) {

        // Only collect layer that are files
        String contentType = getContentType(containerRef);
        List<Layer> layers = collectLayers(containerRef, contentType, false);
        if (layers.isEmpty()) {
            LOG.info("Skipped pulling layers without file name in '{}'", Const.ANNOTATION_TITLE);
            return;
        }
        for (Layer layer : layers) {
            try (InputStream is = fetchBlob(containerRef.withDigest(layer.getDigest()))) {
                // Unpack or just copy blob
                if (Boolean.parseBoolean(layer.getAnnotations().getOrDefault(Const.ANNOTATION_ORAS_UNPACK, "false"))) {
                    LOG.debug("Extracting blob to: {}", path);

                    // Uncompress the tar.gz archive and verify digest if present
                    LocalPath tempArchive = ArchiveUtils.uncompress(is, layer.getMediaType());
                    String expectedDigest = layer.getAnnotations().get(Const.ANNOTATION_ORAS_CONTENT_DIGEST);
                    if (expectedDigest != null) {
                        LOG.trace("Expected digest: {}", expectedDigest);
                        String actualDigest = containerRef.getAlgorithm().digest(tempArchive.getPath());
                        LOG.trace("Actual digest: {}", actualDigest);
                        if (!expectedDigest.equals(actualDigest)) {
                            throw new OrasException(
                                    "Digest mismatch: expected %s but got %s".formatted(expectedDigest, actualDigest));
                        }
                    }

                    // Extract the tar
                    ArchiveUtils.untar(Files.newInputStream(tempArchive.getPath()), path);

                } else {
                    Path targetPath = path.resolve(
                            layer.getAnnotations().getOrDefault(Const.ANNOTATION_TITLE, layer.getDigest()));
                    LOG.debug("Copying blob to: {}", targetPath);
                    Files.copy(
                            is,
                            targetPath,
                            overwrite ? StandardCopyOption.REPLACE_EXISTING : StandardCopyOption.ATOMIC_MOVE);
                }
            } catch (IOException e) {
                throw new OrasException("Failed to pull artifact", e);
            }
        }
    }

    @Override
    public Manifest pushArtifact(
            ContainerRef containerRef,
            ArtifactType artifactType,
            Annotations annotations,
            @Nullable Config config,
            LocalPath... paths) {
        Manifest manifest = Manifest.empty().withArtifactType(artifactType);
        Map<String, String> manifestAnnotations = new HashMap<>(annotations.manifestAnnotations());
        if (!manifestAnnotations.containsKey(Const.ANNOTATION_CREATED) && containerRef.getDigest() == null) {
            manifestAnnotations.put(Const.ANNOTATION_CREATED, Const.currentTimestamp());
        }
        manifest = manifest.withAnnotations(manifestAnnotations);
        if (config != null) {
            config = config.withAnnotations(annotations);
            manifest = manifest.withConfig(config);
        }

        // Push layers
        List<Layer> layers = pushLayers(containerRef, false, paths);

        // Push the config like any other blob
        Config pushedConfig = pushConfig(containerRef, config != null ? config : Config.empty());

        // Add layer and config
        manifest = manifest.withLayers(layers).withConfig(pushedConfig);

        // Push the manifest
        manifest = pushManifest(containerRef, manifest);
        LOG.debug(
                "Manifest pushed to: {}",
                containerRef.withDigest(manifest.getDescriptor().getDigest()));
        return manifest;
    }

    /**
     * Copy an artifact from one container to another
     * @param targetRegistry The target registry
     * @param sourceContainer The source container
     * @param targetContainer The target container
     */
    public void copy(Registry targetRegistry, ContainerRef sourceContainer, ContainerRef targetContainer) {
        throw new OrasException("Not implemented");
    }

    /**
     * Attach file to an existing manifest
     * @param containerRef The container
     * @param artifactType The artifact type
     * @param paths The paths
     * @return The manifest of the new artifact
     */
    public Manifest attachArtifact(ContainerRef containerRef, ArtifactType artifactType, LocalPath... paths) {
        return attachArtifact(containerRef, artifactType, Annotations.empty(), paths);
    }

    /**
     * Attach file to an existing manifest
     * @param containerRef The container
     * @param artifactType The artifact type
     * @param annotations The annotations
     * @param paths The paths
     * @return The manifest of the new artifact
     */
    public Manifest attachArtifact(
            ContainerRef containerRef, ArtifactType artifactType, Annotations annotations, LocalPath... paths) {

        // Push layers
        List<Layer> layers = pushLayers(containerRef, false, paths);

        // Get the subject from the manifest
        Subject subject = getManifest(containerRef).getDescriptor().toSubject();

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
                containerRef.withDigest(
                        SupportedAlgorithm.SHA256.digest(manifest.toJson().getBytes(StandardCharsets.UTF_8))),
                manifest);
    }

    @Override
    public Layer pushBlob(ContainerRef containerRef, Path blob, Map<String, String> annotations) {
        String digest = containerRef.getAlgorithm().digest(blob);
        LOG.debug("Digest: {}", digest);
        if (hasBlob(containerRef.withDigest(digest))) {
            LOG.info("Blob already exists: {}", digest);
            return Layer.fromFile(blob, containerRef.getAlgorithm()).withAnnotations(annotations);
        }
        URI uri = URI.create(
                "%s://%s".formatted(getScheme(), containerRef.withDigest(digest).getBlobsUploadDigestPath()));
        OrasHttpClient.ResponseWrapper<String> response = client.upload(
                "POST", uri, Map.of(Const.CONTENT_TYPE_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE), blob);
        logResponse(response);

        // Switch to bearer auth if needed and retry first request
        if (switchTokenAuth(containerRef, response)) {
            response = client.upload(
                    "POST", uri, Map.of(Const.CONTENT_TYPE_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE), blob);
            logResponse(response);
        }

        // Accepted single POST push
        if (response.statusCode() == 201) {
            return Layer.fromFile(blob, containerRef.getAlgorithm()).withAnnotations(annotations);
        }

        // We need to push via PUT
        if (response.statusCode() == 202) {
            String location = response.headers().get(Const.LOCATION_HEADER.toLowerCase());
            // Ensure location is absolute URI
            if (!location.startsWith("http") && !location.startsWith("https")) {
                location = "%s://%s/%s"
                        .formatted(getScheme(), containerRef.getApiRegistry(), location.replaceFirst("^/", ""));
            }
            LOG.debug("Location header: {}", location);
            response = client.upload(
                    "PUT",
                    URI.create("%s&digest=%s".formatted(location, digest)),
                    Map.of(Const.CONTENT_TYPE_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE),
                    blob);
            if (response.statusCode() == 201) {
                LOG.debug("Successful push: {}", response.response());
            } else {
                throw new OrasException("Failed to push layer: %s".formatted(response.response()));
            }
        }

        handleError(response);
        return Layer.fromFile(blob, containerRef.getAlgorithm()).withAnnotations(annotations);
    }

    @Override
    public Layer pushBlob(ContainerRef containerRef, byte[] data) {
        String digest = containerRef.getAlgorithm().digest(data);
        if (containerRef.getDigest() != null) {
            ensureDigest(containerRef, data);
        }
        if (hasBlob(containerRef.withDigest(digest))) {
            LOG.info("Blob already exists: {}", digest);
            return Layer.fromData(containerRef, data);
        }
        URI uri = URI.create(
                "%s://%s".formatted(getScheme(), containerRef.withDigest(digest).getBlobsUploadDigestPath()));
        OrasHttpClient.ResponseWrapper<String> response =
                client.post(uri, data, Map.of(Const.CONTENT_TYPE_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE));
        logResponse(response);

        // Switch to bearer auth if needed and retry first request
        if (switchTokenAuth(containerRef, response)) {
            response = client.post(
                    uri, data, Map.of(Const.CONTENT_TYPE_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE));
            logResponse(response);
        }

        // Accepted single POST push
        if (response.statusCode() == 201) {
            return Layer.fromData(containerRef, data);
        }

        // We need to push via PUT
        if (response.statusCode() == 202) {
            String location = response.headers().get(Const.LOCATION_HEADER.toLowerCase());
            // Ensure location is absolute URI
            if (!location.startsWith("http") && !location.startsWith("https")) {
                location = "%s://%s/%s"
                        .formatted(getScheme(), containerRef.getApiRegistry(), location.replaceFirst("^/", ""));
            }
            LOG.debug("Location header: {}", location);
            response = client.put(
                    URI.create("%s&digest=%s".formatted(location, digest)),
                    data,
                    Map.of(Const.CONTENT_TYPE_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE));
            if (response.statusCode() == 201) {
                LOG.debug("Successful push: {}", response.response());
            } else {
                throw new OrasException("Failed to push layer: %s".formatted(response.response()));
            }
        }

        handleError(response);
        return Layer.fromData(containerRef, data);
    }

    /**
     * Return if the registry contains already the blob
     * @param containerRef The container
     * @return True if the blob exists
     */
    private boolean hasBlob(ContainerRef containerRef) {
        OrasHttpClient.ResponseWrapper<String> response = headBlob(containerRef);
        return response.statusCode() == 200;
    }

    private OrasHttpClient.ResponseWrapper<String> headBlob(ContainerRef containerRef) {
        URI uri = URI.create("%s://%s".formatted(getScheme(), containerRef.getBlobsPath()));
        OrasHttpClient.ResponseWrapper<String> response =
                client.head(uri, Map.of(Const.ACCEPT_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE));
        logResponse(response);

        // Switch to bearer auth if needed and retry first request
        if (switchTokenAuth(containerRef, response)) {
            response = client.head(uri, Map.of(Const.ACCEPT_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE));
            logResponse(response);
        }
        return response;
    }

    /**
     * Get the blob for the given digest. Not be suitable for large blobs
     * @param containerRef The container
     * @return The blob as bytes
     */
    @Override
    public byte[] getBlob(ContainerRef containerRef) {
        try (InputStream is = fetchBlob(containerRef)) {
            return ensureDigest(containerRef, is.readAllBytes());
        } catch (IOException e) {
            throw new OrasException("Failed to get blob", e);
        }
    }

    @Override
    public void fetchBlob(ContainerRef containerRef, Path path) {
        if (!hasBlob(containerRef)) {
            throw new OrasException(new OrasHttpClient.ResponseWrapper<>("", 404, Map.of()));
        }
        URI uri = URI.create("%s://%s".formatted(getScheme(), containerRef.getBlobsPath()));
        OrasHttpClient.ResponseWrapper<Path> response =
                client.download(uri, Map.of(Const.ACCEPT_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE), path);
        logResponse(response);
        handleError(response);
    }

    @Override
    public InputStream fetchBlob(ContainerRef containerRef) {
        if (!hasBlob(containerRef)) {
            throw new OrasException(new OrasHttpClient.ResponseWrapper<>("", 404, Map.of()));
        }
        URI uri = URI.create("%s://%s".formatted(getScheme(), containerRef.getBlobsPath()));
        OrasHttpClient.ResponseWrapper<InputStream> response =
                client.download(uri, Map.of(Const.ACCEPT_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE));
        logResponse(response);
        handleError(response);
        return response.response();
    }

    @Override
    public Descriptor fetchBlobDescriptor(ContainerRef containerRef) {
        OrasHttpClient.ResponseWrapper<String> response = headBlob(containerRef);
        handleError(response);
        String size = response.headers().get(Const.CONTENT_LENGTH_HEADER.toLowerCase());
        String digest = response.headers().get(Const.DOCKER_CONTENT_DIGEST_HEADER.toLowerCase());
        return Descriptor.of(digest, Long.parseLong(size), Const.DEFAULT_DESCRIPTOR_MEDIA_TYPE);
    }

    /**
     * Get the manifest of a container
     * @param containerRef The container
     * @return The manifest and it's associated descriptor
     */
    public Manifest getManifest(ContainerRef containerRef) {
        OrasHttpClient.ResponseWrapper<String> response = getManifestResponse(containerRef);
        logResponse(response);
        handleError(response);
        String contentType = response.headers().get(Const.CONTENT_TYPE_HEADER.toLowerCase());
        if (!isManifestMediaType(contentType)) {
            throw new OrasException(
                    "Expected manifest but got index. Probably a multi-platform image instead of artifact");
        }
        String size = response.headers().get(Const.CONTENT_LENGTH_HEADER.toLowerCase());
        String digest = response.headers().get(Const.DOCKER_CONTENT_DIGEST_HEADER.toLowerCase());
        ManifestDescriptor descriptor =
                ManifestDescriptor.of(contentType, digest, size == null ? 0 : Long.parseLong(size));
        return Manifest.fromJson(response.response()).withDescriptor(descriptor);
    }

    /**
     * Get the index of a container
     * @param containerRef The container
     * @return The index and it's associated descriptor
     */
    public Index getIndex(ContainerRef containerRef) {
        OrasHttpClient.ResponseWrapper<String> response = getManifestResponse(containerRef);
        logResponse(response);
        handleError(response);
        String contentType = response.headers().get(Const.CONTENT_TYPE_HEADER.toLowerCase());
        if (!isIndexMediaType(contentType)) {
            throw new OrasException("Expected index but got %s".formatted(contentType));
        }
        String size = response.headers().get(Const.CONTENT_LENGTH_HEADER.toLowerCase());
        String digest = response.headers().get(Const.DOCKER_CONTENT_DIGEST_HEADER.toLowerCase());
        ManifestDescriptor descriptor =
                ManifestDescriptor.of(contentType, digest, size == null ? 0 : Long.parseLong(size));
        return Index.fromJson(response.response()).withDescriptor(descriptor);
    }

    /**
     * Get a manifest response
     * @param containerRef The container
     * @return The response
     */
    private OrasHttpClient.ResponseWrapper<String> getManifestResponse(ContainerRef containerRef) {
        URI uri = URI.create("%s://%s".formatted(getScheme(), containerRef.getManifestsPath()));
        OrasHttpClient.ResponseWrapper<String> response =
                client.head(uri, Map.of(Const.ACCEPT_HEADER, Const.MANIFEST_ACCEPT_TYPE));
        logResponse(response);

        // Switch to bearer auth if needed and retry first request
        if (switchTokenAuth(containerRef, response)) {
            response = client.head(uri, Map.of(Const.ACCEPT_HEADER, Const.MANIFEST_ACCEPT_TYPE));
            logResponse(response);
        }
        handleError(response);
        return client.get(uri, Map.of("Accept", Const.MANIFEST_ACCEPT_TYPE));
    }

    private byte[] ensureDigest(ContainerRef ref, byte[] data) {
        if (ref.getDigest() == null) {
            throw new OrasException("Missing digest");
        }
        SupportedAlgorithm algorithm = SupportedAlgorithm.fromDigest(ref.getDigest());
        String dataDigest = algorithm.digest(data);
        if (!ref.getDigest().equals(dataDigest)) {
            throw new OrasException("Digest mismatch: %s != %s".formatted(ref.getTag(), dataDigest));
        }
        return data;
    }

    /**
     * Switch the current authentication to token auth
     * @param response The response
     */
    private boolean switchTokenAuth(ContainerRef containerRef, OrasHttpClient.ResponseWrapper<String> response) {
        if (response.statusCode() == 401 && !(authProvider instanceof BearerTokenProvider)) {
            LOG.debug("Requesting token with token flow");
            setAuthProvider(new BearerTokenProvider(authProvider).refreshToken(containerRef, client, response));
            return true;
        }
        // Need token refresh (expired or wrong scope)
        if ((response.statusCode() == 401 || response.statusCode() == 403)
                && authProvider instanceof BearerTokenProvider) {
            LOG.debug("Requesting new token with username password flow");
            setAuthProvider(((BearerTokenProvider) authProvider).refreshToken(containerRef, client, response));
            return true;
        }
        return false;
    }

    /**
     * Handle an error response
     * @param responseWrapper The response
     */
    @SuppressWarnings("unchecked")
    private void handleError(OrasHttpClient.ResponseWrapper<?> responseWrapper) {
        if (responseWrapper.statusCode() >= 400) {
            if (responseWrapper.response() instanceof String) {
                LOG.debug("Response: {}", responseWrapper.response());
                throw new OrasException((OrasHttpClient.ResponseWrapper<String>) responseWrapper);
            }
            throw new OrasException(new OrasHttpClient.ResponseWrapper<>("", responseWrapper.statusCode(), Map.of()));
        }
    }

    /**
     * Log the response
     * @param response The response
     */
    private void logResponse(OrasHttpClient.ResponseWrapper<?> response) {
        LOG.debug("Status Code: {}", response.statusCode());
        LOG.debug("Headers: {}", response.headers());
        // Only log non-binary responses
        if (response.response() instanceof String) {
            LOG.debug("Response: {}", response.response());
        }
    }

    /**
     * Push a blob using input stream in chunks to avoid loading the whole blob in memory.
     * This method is recommended for large files to prevent excessive memory usage.
     * For smaller blobs, consider using {@link #pushBlob(ContainerRef, Path)} which may be more efficient
     * as it uses fewer HTTP requests.
     *
     * This method complies with the OCI Distribution Specification for chunked uploads and will
     * respect the minimum chunk size requirements specified by the registry.
     *
     * @param containerRef the container ref
     * @param input the input stream
     * @param size the size of the blob
     * @return The Layer containing the uploaded blob information
     * @throws OrasException if upload fails or digest calculation fails
     * @see <a href="https://github.com/opencontainers/distribution-spec/blob/main/spec.md#pushing-a-blob-in-chunks">OCI Distribution Spec: Pushing a blob in chunks</a>
     */
    public Layer pushChunks(ContainerRef containerRef, InputStream input, long size) {
        // INITIALIZATION PHASE

        // Initialize the Message Digest
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(getMessageDigestAlgorithm(containerRef.getAlgorithm()));
        } catch (NoSuchAlgorithmException e) {
            throw new OrasException("Failed to get message digest", e);
        }

        byte[] buffer = new byte[CHUNK_SIZE];
        ByteArrayOutputStream firstChunk = new ByteArrayOutputStream();
        int bytesRead;
        long totalBytesRead = 0;
        String contentDigest = null;

        try {
            // FIRST CHUNK PROCESSING

            // Read first chunk to buffer for initial PATCH request
            while ((bytesRead = input.read(buffer)) != -1 && totalBytesRead < CHUNK_SIZE) {
                digest.update(buffer, 0, bytesRead);
                firstChunk.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            // Handle small blobs that fit in one chunk
            if (bytesRead == -1) {
                contentDigest = createDigestString(containerRef, digest.digest());

                // Check if blob already exists, return early if it does
                if (hasBlob(containerRef.withDigest(contentDigest))) {
                    LOG.info("Blob already exists: {}", contentDigest);
                    return Layer.fromDigest(contentDigest, totalBytesRead);
                }
            }

            // UPLOAD SESSION INITIALIZATION

            URI uploadUri = URI.create("%s://%s".formatted(getScheme(), containerRef.getBlobsUploadPath()));
            OrasHttpClient.ResponseWrapper<String> response = client.post(uploadUri, new byte[0], Map.of());

            // Handle authentication if needed
            if (switchTokenAuth(containerRef, response)) {
                response = client.post(uploadUri, new byte[0], Map.of());
            }

            handleError(response);
            if (response.statusCode() != 202) {
                throw new OrasException("Failed to initiate blob upload: " + response.statusCode());
            }

            // Get upload location URL
            String location = response.headers().get(Const.LOCATION_HEADER.toLowerCase());
            if (location == null) {
                throw new OrasException("No location header in response");
            }

            // Handle minimum chunk size requirements from registry
            int chunkSize = adjustChunkSizeIfNeeded(response, buffer.length);
            if (buffer.length < chunkSize) {
                buffer = new byte[chunkSize];
            }

            // Ensure location is an absolute URL
            location = ensureAbsoluteUri(location, containerRef);
            LOG.debug("Initial location URL: {}", location);

            // UPLOAD FIRST CHUNK

            long startRange = 0;
            long endRange = totalBytesRead - 1;

            if (totalBytesRead > 0) {
                // Prepare headers for first chunk
                Map<String, String> firstChunkHeaders = new HashMap<>();
                firstChunkHeaders.put(Const.CONTENT_TYPE_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE);
                firstChunkHeaders.put(Const.CONTENT_RANGE_HEADER, startRange + "-" + endRange);

                // Upload first chunk
                response = client.patch(URI.create(location), firstChunk.toByteArray(), firstChunkHeaders);
                handleError(response);

                if (response.statusCode() != 202) {
                    throw new OrasException("Failed to upload first chunk: " + response.statusCode());
                }

                // Update location for next request
                location = getLocationHeader(response);
                location = ensureAbsoluteUri(location, containerRef);
                LOG.debug("Location after first chunk: {}", location);

                // Update range information for next chunk
                endRange = getEndRangeFromHeader(response, endRange);
                startRange = endRange + 1;

                // PROCESS TRANSITION BYTES

                // Handle bytes read during the last iteration of first chunk loop
                if (bytesRead > 0) {
                    LOG.debug("Processing transition bytes: {} bytes", bytesRead);
                    digest.update(buffer, 0, bytesRead);

                    // Prepare headers for transition bytes
                    Map<String, String> transitionHeaders = new HashMap<>();
                    long transitionEndRange = startRange + bytesRead - 1;
                    transitionHeaders.put(Const.CONTENT_TYPE_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE);
                    transitionHeaders.put(Const.CONTENT_RANGE_HEADER, startRange + "-" + transitionEndRange);

                    // Upload transition bytes
                    response = client.patch(URI.create(location), Arrays.copyOf(buffer, bytesRead), transitionHeaders);
                    handleError(response);

                    if (response.statusCode() != 202) {
                        throw new OrasException("Failed to upload transition bytes: " + response.statusCode());
                    }

                    // Update location for next chunk
                    location = getLocationHeader(response);
                    location = ensureAbsoluteUri(location, containerRef);
                    LOG.debug("Location after transition chunk: {}", location);

                    // Update range information for next chunk
                    endRange = getEndRangeFromHeader(response, transitionEndRange);
                    startRange = endRange + 1;
                    totalBytesRead += bytesRead;
                }
            }

            // UPLOAD REMAINING CHUNKS

            while ((bytesRead = input.read(buffer)) != -1) {
                // Update digest with current chunk
                digest.update(buffer, 0, bytesRead);

                // Prepare headers for chunk
                Map<String, String> chunkHeaders = new HashMap<>();
                endRange = startRange + bytesRead - 1;
                chunkHeaders.put(Const.CONTENT_TYPE_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE);
                chunkHeaders.put(Const.CONTENT_RANGE_HEADER, startRange + "-" + endRange);

                // Upload chunk
                response = client.patch(URI.create(location), Arrays.copyOf(buffer, bytesRead), chunkHeaders);
                handleError(response);

                if (response.statusCode() != 202) {
                    throw new OrasException("Failed to upload chunk: " + response.statusCode());
                }

                // Update location for next chunk
                location = getLocationHeader(response);
                location = ensureAbsoluteUri(location, containerRef);

                // Update range information for next chunk
                endRange = getEndRangeFromHeader(response, endRange);
                startRange = endRange + 1;
                totalBytesRead += bytesRead;
            }

            // FINALIZE UPLOAD

            // Calculate final digest if not already done
            if (contentDigest == null) {
                contentDigest = createDigestString(containerRef, digest.digest());
                LOG.debug("Calculated content digest: {}", contentDigest);
            }

            // Prepare final upload URI
            URI finalizeUri = constructFinalizeUri(location, contentDigest, containerRef);

            // Complete the upload with final PUT
            Map<String, String> finalHeaders = new HashMap<>();
            finalHeaders.put(Const.CONTENT_TYPE_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE);

            // Log finalization details for debugging
            LOG.debug("Final PUT URL: {}", finalizeUri);
            LOG.debug("Content Digest: {}", contentDigest);

            response = client.put(finalizeUri, new byte[0], finalHeaders);
            logFinalResponse(response);
            handleError(response);

            if (response.statusCode() != 201) {
                throw new OrasException("Failed to complete blob upload: " + response.statusCode());
            }

            return Layer.fromDigest(contentDigest, totalBytesRead);

        } catch (IOException e) {
            throw new OrasException("Failed to push blob", e);
        }
    }

    private List<Layer> pushLayers(ContainerRef containerRef, LocalPath... paths) {
        List<Layer> layers = new ArrayList<>();
        for (LocalPath path : paths) {
            try {
                // Create tar.gz archive for directory
                if (Files.isDirectory(path.getPath())) {
                    LocalPath tempTar = ArchiveUtils.tar(path);
                    LocalPath tempArchive = ArchiveUtils.compress(tempTar, path.getMediaType());
                    try (InputStream is = Files.newInputStream(tempArchive.getPath())) {
                        long size = Files.size(tempArchive.getPath());
                        Layer layer = pushChunks(containerRef, is, size)
                                .withMediaType(path.getMediaType())
                                .withAnnotations(Map.of(
                                        Const.ANNOTATION_TITLE,
                                        path.getPath().getFileName().toString(),
                                        Const.ANNOTATION_ORAS_CONTENT_DIGEST,
                                        containerRef.getAlgorithm().digest(tempTar.getPath()),
                                        Const.ANNOTATION_ORAS_UNPACK,
                                        "true"));
                        layers.add(layer);
                        LOG.info("Uploaded directory: {}", layer.getDigest());
                    }
                    Files.delete(tempArchive.getPath());
                } else {
                    try (InputStream is = Files.newInputStream(path.getPath())) {
                        long size = Files.size(path.getPath());
                        Layer layer = pushChunks(containerRef, is, size)
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
     * Get blob as stream to avoid loading into memory
     * @param containerRef The container ref
     * @return The input stream
     */
    public InputStream getBlobStream(ContainerRef containerRef) {
        // Similar to fetchBlob()
        return fetchBlob(containerRef);
    }

    /**
     * Convert SupportedAlgorithm to MessageDigest algorithm string
     * @param algorithm The supported algorithm
     * @return The algorithm string for MessageDigest
     */
    private String getMessageDigestAlgorithm(SupportedAlgorithm algorithm) {
        switch (algorithm) {
            case SHA256:
                return "SHA-256";
            case SHA512:
                return "SHA-512";
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
    }

    // Helper method to convert bytes to hex
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Creates a properly formatted digest string.
     */
    private String createDigestString(ContainerRef containerRef, byte[] digestBytes) {
        return containerRef.getAlgorithm().getPrefix() + ":" + bytesToHex(digestBytes);
    }

    /**
     * Gets and validates the location header.
     */
    private String getLocationHeader(OrasHttpClient.ResponseWrapper<String> response) {
        String location = response.headers().get(Const.LOCATION_HEADER.toLowerCase());
        if (location == null) {
            throw new OrasException("No location header in response");
        }
        return location;
    }

    /**
     * Makes sure the location URI has a scheme.
     */
    private String ensureAbsoluteUri(String location, ContainerRef containerRef) {
        if (!location.startsWith("http:") && !location.startsWith("https:")) {
            return "%s://%s/%s".formatted(getScheme(), containerRef.getRegistry(), location.replaceFirst("^/", ""));
        }
        return location;
    }

    /**
     * Extracts the end range value from response headers.
     */
    private long getEndRangeFromHeader(OrasHttpClient.ResponseWrapper<String> response, long defaultEndRange) {
        String rangeHeader = response.headers().get(Const.RANGE_HEADER.toLowerCase());
        if (rangeHeader != null) {
            String[] parts = rangeHeader.split("-");
            if (parts.length == 2) {
                return Long.parseLong(parts[1]);
            }
        }
        return defaultEndRange;
    }

    /**
     * Adjusts chunk size based on registry requirements.
     */
    private int adjustChunkSizeIfNeeded(OrasHttpClient.ResponseWrapper<String> response, int currentChunkSize) {
        String minChunkSizeHeader = response.headers().get("OCI-Chunk-Min-Length".toLowerCase());
        if (minChunkSizeHeader == null) {
            return currentChunkSize;
        }

        try {
            int registryMinChunkSize = Integer.parseInt(minChunkSizeHeader);
            if (registryMinChunkSize > currentChunkSize) {
                LOG.debug(
                        "Registry requires minimum chunk size of {} bytes, adjusting from default {} bytes",
                        registryMinChunkSize,
                        currentChunkSize);
                return registryMinChunkSize;
            }
        } catch (NumberFormatException e) {
            LOG.warn("Invalid OCI-Chunk-Min-Length header value: {}", minChunkSizeHeader);
        }

        return currentChunkSize;
    }

    /**
     * Constructs the URI for finalizing the upload.
     */
    private URI constructFinalizeUri(String location, String contentDigest, ContainerRef containerRef) {
        location = ensureAbsoluteUri(location, containerRef);
        LOG.debug("Final location before finalize: {}", location);

        try {
            if (location.contains("?")) {
                return URI.create(location + "&digest=" + contentDigest);
            } else {
                return URI.create(location + "?digest=" + contentDigest);
            }
        } catch (Exception e) {
            throw new OrasException("Failed to construct URI for completing upload", e);
        }
    }

    /**
     * Logs final response details.
     */
    private void logFinalResponse(OrasHttpClient.ResponseWrapper<String> response) {
        LOG.debug("Response status: {}", response.statusCode());
        LOG.debug("Response headers: {}", response.headers());
        if (response.response() instanceof String) {
            LOG.debug("Response body: {}", response.response());
        }
     * Return if a media type is an index media type
     * @param mediaType The media type
     * @return True if it is a index media type
     */
    boolean isIndexMediaType(String mediaType) {
        return mediaType.equals(Const.DEFAULT_INDEX_MEDIA_TYPE) || mediaType.equals(Const.DOCKER_INDEX_MEDIA_TYPE);
    }

    /**
     * Return if a media type is a manifest media type
     * @param mediaType The media type
     * @return True if it is a manifest media type
     */
    boolean isManifestMediaType(String mediaType) {
        return mediaType.equals(Const.DEFAULT_MANIFEST_MEDIA_TYPE)
                || mediaType.equals(Const.DOCKER_MANIFEST_MEDIA_TYPE);
    }

    /**
     * Get the content type of the container
     * @param containerRef The container
     * @return The content type
     */
    String getContentType(ContainerRef containerRef) {
        return getHeaders(containerRef).get(Const.CONTENT_TYPE_HEADER.toLowerCase());
    }

    /**
     * Execute a head request on the manifest URL and return the headers
     * @param containerRef The container
     * @return The headers
     */
    Map<String, String> getHeaders(ContainerRef containerRef) {
        URI uri = URI.create("%s://%s".formatted(getScheme(), containerRef.getManifestsPath()));
        OrasHttpClient.ResponseWrapper<String> response =
                client.head(uri, Map.of(Const.ACCEPT_HEADER, Const.MANIFEST_ACCEPT_TYPE));
        logResponse(response);

        // Switch to bearer auth if needed and retry first request
        if (switchTokenAuth(containerRef, response)) {
            response = client.head(uri, Map.of(Const.ACCEPT_HEADER, Const.MANIFEST_ACCEPT_TYPE));
            logResponse(response);
        }
        handleError(response);
        return response.headers();
    }

    /**
     * Collect layers from the container
     * @param containerRef The container
     * @param includeAll Include all layers or only the ones with title annotation
     * @return The layers
     */
    List<Layer> collectLayers(ContainerRef containerRef, String contentType, boolean includeAll) {
        List<Layer> layers = new LinkedList<>();
        if (isManifestMediaType(contentType)) {
            return getManifest(containerRef).getLayers();
        }
        Index index = getIndex(containerRef);
        for (ManifestDescriptor manifestDescriptor : index.getManifests()) {
            List<Layer> manifestLayers = getManifest(containerRef.withDigest(manifestDescriptor.getDigest()))
                    .getLayers();
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
     * Builder for the registry
     */
    public static class Builder {

        private final Registry registry = new Registry();

        /**
         * Hidden constructor
         */
        private Builder() {
            // Hide constructor
        }

        /**
         * Return a new builder with default authentication using existing host auth
         * @return The builder
         */
        public Builder defaults() {
            registry.setAuthProvider(new AuthStoreAuthenticationProvider());
            return this;
        }

        /**
         * Return a new builder with username and password authentication
         * @param username The username
         * @param password The password
         * @return The builder
         */
        public Builder defaults(String username, String password) {
            registry.setAuthProvider(new UsernamePasswordProvider(username, password));
            return this;
        }

        /**
         * Return a new builder with insecure communication and not authentification
         * @return The builder
         */
        public Builder insecure() {
            registry.setInsecure(true);
            registry.setSkipTlsVerify(true);
            registry.setAuthProvider(new NoAuthProvider());
            return this;
        }

        /**
         * Set the auth provider
         * @param authProvider The auth provider
         * @return The builder
         */
        public Builder withAuthProvider(AuthProvider authProvider) {
            registry.setAuthProvider(authProvider);
            return this;
        }

        /**
         * Set the insecure flag
         * @param insecure Insecure
         * @return The builder
         */
        public Builder withInsecure(boolean insecure) {
            registry.setInsecure(insecure);
            return this;
        }

        /**
         * Set the skip TLS verify flag
         * @param skipTlsVerify Skip TLS verify
         * @return The builder
         */
        public Builder withSkipTlsVerify(boolean skipTlsVerify) {
            registry.setSkipTlsVerify(skipTlsVerify);
            return this;
        }

        /**
         * Return a new builder
         * @return The builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Build the registry
         * @return The registry
         */
        public Registry build() {
            return registry.build();
        }
    }
}
