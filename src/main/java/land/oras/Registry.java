package land.oras;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import land.oras.auth.*;
import land.oras.utils.ArchiveUtils;
import land.oras.utils.Const;
import land.oras.utils.DigestUtils;
import land.oras.utils.JsonUtils;
import land.oras.utils.OrasHttpClient;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A registry is the main entry point for interacting with a container registry
 */
@NullMarked
public final class Registry {

    /**
     * The logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Registry.class);

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
        if (switchTokenAuth(response)) {
            response = client.get(uri, Map.of(Const.ACCEPT_HEADER, Const.DEFAULT_JSON_MEDIA_TYPE));
        }
        handleError(response);
        return JsonUtils.fromJson(response.response(), Tags.class).tags();
    }

    /**
     * Delete a manifest
     * @param containerRef The artifact
     */
    public void deleteManifest(ContainerRef containerRef) {
        URI uri = URI.create("%s://%s".formatted(getScheme(), containerRef.getManifestsPath()));
        OrasHttpClient.ResponseWrapper<String> response = client.delete(uri, Map.of());
        logResponse(response);
        if (switchTokenAuth(response)) {
            response = client.delete(uri, Map.of());
            logResponse(response);
        }
        handleError(response);
    }

    /**
     * Push a manifest
     * @param containerRef The container
     * @param manifest The manifest
     * @return The location
     */
    public String pushManifest(ContainerRef containerRef, Manifest manifest) {
        URI uri = URI.create("%s://%s".formatted(getScheme(), containerRef.getManifestsPath()));
        OrasHttpClient.ResponseWrapper<String> response = client.put(
                uri,
                JsonUtils.toJson(manifest).getBytes(),
                Map.of(Const.CONTENT_TYPE_HEADER, Const.DEFAULT_MANIFEST_MEDIA_TYPE));
        if (switchTokenAuth(response)) {
            response = client.put(
                    uri,
                    JsonUtils.toJson(manifest).getBytes(),
                    Map.of(Const.CONTENT_TYPE_HEADER, Const.DEFAULT_MANIFEST_MEDIA_TYPE));
        }
        logResponse(response);
        handleError(response);
        return response.headers().get(Const.LOCATION_HEADER.toLowerCase());
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
        if (switchTokenAuth(response)) {
            response = client.delete(uri, Map.of());
            logResponse(response);
        }
        handleError(response);
    }

    /**
     * Upload an ORAS artifact
     * @param containerRef The container
     * @param paths The paths
     * @return The manifest
     */
    public Manifest pushArtifact(ContainerRef containerRef, Path... paths) {
        return pushArtifact(containerRef, null, Annotations.empty(), Config.empty(), paths);
    }

    /**
     * Upload an ORAS artifact
     * @param containerRef The container
     * @param artifactType The artifact type
     * @param paths The paths
     * @return The manifest
     */
    public Manifest pushArtifact(ContainerRef containerRef, String artifactType, Path... paths) {
        return pushArtifact(containerRef, artifactType, Annotations.empty(), Config.empty(), paths);
    }

    /**
     * Upload an ORAS artifact
     * @param containerRef The container
     * @param artifactType The artifact type
     * @param annotations The annotations
     * @param paths The paths
     * @return The manifest
     */
    public Manifest pushArtifact(
            ContainerRef containerRef, String artifactType, Annotations annotations, Path... paths) {
        return pushArtifact(containerRef, artifactType, annotations, Config.empty(), paths);
    }

    /**
     * Download an ORAS artifact
     * @param containerRef The container
     * @param path The path
     */
    public void pullArtifact(ContainerRef containerRef, Path path) {
        Manifest manifest = getManifest(containerRef);
        for (Layer layer : manifest.getLayers()) {
            // Archive
            if (layer.getMediaType().equals(Const.DEFAULT_BLOB_DIR_MEDIA_TYPE)) {
                Path archive = ArchiveUtils.createTempArchive();
                fetchBlob(containerRef.withDigest(layer.getDigest()), archive);
                LOG.debug("Extracting: {} to {}", archive, path);
                ArchiveUtils.extractTarGz(archive, path);
                LOG.debug("Extracted: {}", path);
            }
            // Single artifact layer
            else {
                // Take the filename of default to the digest
                String fileName = layer.getAnnotations().getOrDefault(Const.ANNOTATION_TITLE, layer.getDigest());
                Path filePath = path.resolve(fileName);
                if (Files.exists(filePath)) {
                    LOG.info("File already exists. Not overriding: {}", filePath);
                } else {
                    fetchBlob(containerRef.withDigest(layer.getDigest()), filePath);
                }
            }
        }
    }

    /**
     * Upload an ORAS artifact
     * @param containerRef The container
     * @param artifactType The artifact type. Can be null
     * @param annotations The annotations
     * @param config The config
     * @param paths The paths
     * @return The manifest
     */
    public Manifest pushArtifact(
            ContainerRef containerRef,
            @Nullable String artifactType,
            Annotations annotations,
            @Nullable Config config,
            Path... paths) {
        Manifest manifest = Manifest.empty();
        if (artifactType != null) {
            manifest = manifest.withArtifactType(artifactType);
        }
        manifest = manifest.withAnnotations(annotations.manifestAnnotations());
        if (config != null) {
            config = config.withAnnotations(annotations);
            manifest = manifest.withConfig(config);
        }
        List<Layer> layers = new ArrayList<>();
        // Upload all files as blobs
        for (Path path : paths) {

            // Save filename in case of compressed archive
            String fileName = path.getFileName().toString();
            boolean isDirectory = false;

            // Add layer annotation for the specific file
            Map<String, String> layerAnnotations = new HashMap<>(annotations.getFileAnnotations(fileName));

            // Add title annotation
            layerAnnotations.put(Const.ANNOTATION_TITLE, fileName);

            // Compress directory to tar.gz
            if (path.toFile().isDirectory()) {
                path = ArchiveUtils.createTarGz(path);
                isDirectory = true;
                layerAnnotations.put(Const.ANNOTATION_ORAS_UNPACK, "true");
            }
            Layer layer = uploadBlob(containerRef, path, layerAnnotations);
            if (isDirectory) {
                layer = layer.withMediaType(Const.DEFAULT_BLOB_DIR_MEDIA_TYPE);
            }
            layers.add(layer);
            LOG.info("Uploaded: {}", layer.getDigest());
        }
        // Push the config like any other blob
        Config pushedConfig = pushConfig(containerRef, config != null ? config : Config.empty());

        // Add layer and config
        manifest = manifest.withLayers(layers).withConfig(pushedConfig);

        // Push the manifest
        String location = pushManifest(containerRef, manifest);
        LOG.debug("Manifest pushed to: {}", location);
        return manifest;
    }

    /**
     * Push a blob from file
     * @param containerRef The container
     * @param blob The blob
     * @return The layer
     */
    public Layer uploadBlob(ContainerRef containerRef, Path blob) {
        return uploadBlob(containerRef, blob, Map.of());
    }

    /**
     * Push a blob from file
     * @param containerRef The container
     * @param blob The blob
     * @param annotations The annotations
     * @return The layer
     */
    public Layer uploadBlob(ContainerRef containerRef, Path blob, Map<String, String> annotations) {
        String digest = DigestUtils.sha256(blob);
        LOG.debug("Digest: {}", digest);
        if (hasBlob(containerRef.withDigest(digest))) {
            LOG.info("Blob already exists: {}", digest);
            return Layer.fromFile(blob).withAnnotations(annotations);
        }
        URI uri = URI.create(
                "%s://%s".formatted(getScheme(), containerRef.withDigest(digest).getBlobsUploadDigestPath()));
        OrasHttpClient.ResponseWrapper<String> response = client.upload(
                "POST", uri, Map.of(Const.CONTENT_TYPE_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE), blob);
        logResponse(response);

        // Switch to bearer auth if needed and retry first request
        if (switchTokenAuth(response)) {
            response = client.upload(
                    "POST", uri, Map.of(Const.CONTENT_TYPE_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE), blob);
            logResponse(response);
        }

        // Accepted single POST push
        if (response.statusCode() == 201) {
            return Layer.fromFile(blob).withAnnotations(annotations);
        }

        // We need to push via PUT
        if (response.statusCode() == 202) {
            String location = response.headers().get(Const.LOCATION_HEADER.toLowerCase());
            // Ensure location is absolute URI
            if (!location.startsWith("http") && !location.startsWith("https")) {
                location = "%s://%s/%s"
                        .formatted(getScheme(), containerRef.getRegistry(), location.replaceFirst("^/", ""));
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
        return Layer.fromFile(blob).withAnnotations(annotations);
    }

    /**
     * Push config
     * @param containerRef The container
     * @param config The config
     * @return The config
     */
    public Config pushConfig(ContainerRef containerRef, Config config) {
        Layer layer = pushBlob(containerRef, config.getDataBytes());
        LOG.debug("Config pushed: {}", layer.getDigest());
        return config;
    }

    /**
     * Push the blob for the given layer in a single post request. Might not be supported by all registries
     * Fallback to POST/then PUT (end-4a) if not supported
     * @param containerRef The container ref
     * @param data The data
     * @return The layer
     */
    public Layer pushBlob(ContainerRef containerRef, byte[] data) {
        String digest = DigestUtils.sha256(data);
        if (hasBlob(containerRef.withDigest(digest))) {
            LOG.info("Blob already exists: {}", digest);
            return Layer.fromData(data);
        }
        URI uri = URI.create(
                "%s://%s".formatted(getScheme(), containerRef.withDigest(digest).getBlobsUploadDigestPath()));
        OrasHttpClient.ResponseWrapper<String> response =
                client.post(uri, data, Map.of(Const.CONTENT_TYPE_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE));
        logResponse(response);

        // Switch to bearer auth if needed and retry first request
        if (switchTokenAuth(response)) {
            response = client.post(
                    uri, data, Map.of(Const.CONTENT_TYPE_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE));
            logResponse(response);
        }

        // Accepted single POST push
        if (response.statusCode() == 201) {
            return Layer.fromData(data);
        }

        // We need to push via PUT
        if (response.statusCode() == 202) {
            String location = response.headers().get(Const.LOCATION_HEADER.toLowerCase());
            // Ensure location is absolute URI
            if (!location.startsWith("http") && !location.startsWith("https")) {
                location = "%s://%s/%s"
                        .formatted(getScheme(), containerRef.getRegistry(), location.replaceFirst("^/", ""));
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
        return Layer.fromData(data);
    }

    /**
     * Return if the registry contains already the blob
     * @param containerRef The container
     * @return True if the blob exists
     */
    public boolean hasBlob(ContainerRef containerRef) {
        URI uri = URI.create("%s://%s".formatted(getScheme(), containerRef.getBlobsPath()));
        OrasHttpClient.ResponseWrapper<String> response =
                client.head(uri, Map.of(Const.ACCEPT_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE));
        logResponse(response);

        // Switch to bearer auth if needed and retry first request
        if (switchTokenAuth(response)) {
            response = client.head(uri, Map.of(Const.ACCEPT_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE));
            logResponse(response);
        }
        return response.statusCode() == 200;
    }

    /**
     * Get the blob for the given digest. Not be suitable for large blobs
     * @param containerRef The container
     * @return The blob as bytes
     */
    public byte[] getBlob(ContainerRef containerRef) {
        if (!hasBlob(containerRef)) {
            throw new OrasException(new OrasHttpClient.ResponseWrapper<>("", 404, Map.of()));
        }
        URI uri = URI.create("%s://%s".formatted(getScheme(), containerRef.getBlobsPath()));
        OrasHttpClient.ResponseWrapper<String> response =
                client.get(uri, Map.of(Const.ACCEPT_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE));
        logResponse(response);

        // Switch to bearer auth if needed and retry first request
        if (switchTokenAuth(response)) {
            response = client.get(uri, Map.of(Const.ACCEPT_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE));
            logResponse(response);
        }
        handleError(response);
        return response.response().getBytes();
    }

    /**
     * Fetch blob and save it to file
     * @param containerRef The container
     * @param path The path to save the blob
     */
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

    /**
     * Get the manifest of a container
     * @param containerRef The container
     * @return The manifest
     */
    public Manifest getManifest(ContainerRef containerRef) {
        URI uri = URI.create("%s://%s".formatted(getScheme(), containerRef.getManifestsPath()));
        OrasHttpClient.ResponseWrapper<String> response =
                client.head(uri, Map.of(Const.ACCEPT_HEADER, Const.DEFAULT_MANIFEST_MEDIA_TYPE));
        logResponse(response);

        // Switch to bearer auth if needed and retry first request
        if (switchTokenAuth(response)) {
            response = client.head(uri, Map.of(Const.ACCEPT_HEADER, Const.DEFAULT_MANIFEST_MEDIA_TYPE));
            logResponse(response);
        }
        handleError(response);
        response = client.get(uri, Map.of("Accept", Const.DEFAULT_MANIFEST_MEDIA_TYPE));
        logResponse(response);
        handleError(response);
        return JsonUtils.fromJson(response.response(), Manifest.class);
    }

    /**
     * Switch the current authentication to token auth
     * @param response The response
     */
    private boolean switchTokenAuth(OrasHttpClient.ResponseWrapper<String> response) {
        if (response.statusCode() == 401 && authProvider instanceof AbstractUsernamePasswordProvider) {
            LOG.debug("Requesting token with token flow");
            setAuthProvider(
                    new BearerTokenProvider((AbstractUsernamePasswordProvider) authProvider).refreshToken(response));
            return true;
        }
        // Need token refresh (expired or wrong scope)
        if ((response.statusCode() == 401 || response.statusCode() == 403)
                && authProvider instanceof BearerTokenProvider) {
            LOG.debug("Requesting new token with username password flow");
            setAuthProvider(((BearerTokenProvider) authProvider).refreshToken(response));
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
