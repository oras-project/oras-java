package land.oras;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
     * @param overwrite Overwrite
     */
    public void pullArtifact(ContainerRef containerRef, Path path, boolean overwrite) {
        Manifest manifest = getManifest(containerRef);
        for (Layer layer : manifest.getLayers()) {
            Path targetPath =
                    path.resolve(layer.getAnnotations().getOrDefault(Const.ANNOTATION_TITLE, layer.getDigest()));

            try (InputStream is = fetchBlob(containerRef.withDigest(layer.getDigest()))) {
                Files.copy(
                        is,
                        targetPath,
                        overwrite ? StandardCopyOption.REPLACE_EXISTING : StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                throw new OrasException("Failed to pull artifact", e);
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
            try {
                if (Files.isDirectory(path)) {
                    // Create tar.gz archive for directory
                    Path tempArchive = ArchiveUtils.createTarGz(path);
                    try (InputStream is = Files.newInputStream(tempArchive)) {
                        long size = Files.size(tempArchive);
                        Layer layer = pushBlobStream(containerRef, is, size)
                                .withMediaType(Const.DEFAULT_BLOB_DIR_MEDIA_TYPE) // Use tar+gzip for directories
                                .withAnnotations(Map.of(
                                        Const.ANNOTATION_TITLE,
                                        path.getFileName().toString(),
                                        Const.ANNOTATION_ORAS_UNPACK,
                                        "true"));
                        layers.add(layer);
                        LOG.info("Uploaded directory: {}", layer.getDigest());
                    }
                    Files.delete(tempArchive);
                } else {
                    try (InputStream is = Files.newInputStream(path)) {
                        long size = Files.size(path);
                        // Set mediaType for individual files
                        String mediaType = Files.probeContentType(path);
                        if (mediaType == null) {
                            mediaType = "application/octet-stream";
                        }
                        Layer layer = pushBlobStream(containerRef, is, size)
                                .withMediaType(mediaType)
                                .withAnnotations(Map.of(
                                        Const.ANNOTATION_TITLE,
                                        path.getFileName().toString()));
                        layers.add(layer);
                        LOG.info("Uploaded: {}", layer.getDigest());
                    }
                }
            } catch (IOException e) {
                throw new OrasException("Failed to push artifact", e);
            }
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
     * Fetch blob and return it as input stream
     * @param containerRef The container
     * @return The input stream
     */
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

    /**
     * Push a blob using input stream to avoid loading the whole blob in memory
     * @param containerRef the container ref
     * @param input the input stream
     * @param size the size of the blob
     * @return The Layer containing the uploaded blob information
     * @throws OrasException if upload fails or digest calculation fails
     */
    public Layer pushBlobStream(ContainerRef containerRef, InputStream input, long size) {
        Path tempFile = null;
        try {
            // Create a temporary file to store the stream content
            tempFile = Files.createTempFile("oras-upload-", ".tmp");

            // Copy input stream to temp file while calculating digest
            String digest;
            try (InputStream bufferedInput = new BufferedInputStream(input);
                    DigestInputStream digestInput =
                            new DigestInputStream(bufferedInput, MessageDigest.getInstance("SHA-256"));
                    OutputStream fileOutput = Files.newOutputStream(tempFile)) {

                digestInput.transferTo(fileOutput);
                byte[] digestBytes = digestInput.getMessageDigest().digest();
                digest = "sha256:" + bytesToHex(digestBytes);
            }

            // Check if the blob already exists
            if (hasBlob(containerRef.withDigest(digest))) {
                LOG.info("Blob already exists: {}", digest);
                return Layer.fromDigest(digest, size);
            }

            // Construct the URI for initiating the upload
            URI baseUri = URI.create("%s://%s".formatted(getScheme(), containerRef.getBlobsUploadPath()));
            System.out.println("Initiating blob upload at: " + baseUri);

            // Create an empty input stream for the initial POST request
            InputStream emptyStream = new ByteArrayInputStream(new byte[0]);

            // Start with a POST request to initiate the upload
            OrasHttpClient.ResponseWrapper<String> initiateResponse = client.uploadStream(
                    "POST",
                    baseUri,
                    emptyStream,
                    0,
                    Map.of(Const.CONTENT_TYPE_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE));

            if (initiateResponse.statusCode() != 202) {
                throw new OrasException("Failed to initiate blob upload: " + initiateResponse.statusCode());
            }

            // Get the location URL for the actual upload
            String locationUrl = initiateResponse.headers().get("location");
            if (locationUrl == null || locationUrl.isEmpty()) {
                throw new OrasException("No location URL provided for blob upload");
            }

            // Ensure the location URL is absolute
            if (!locationUrl.startsWith("http")) {
                locationUrl = "%s://%s%s".formatted(getScheme(), containerRef.getRegistry(), locationUrl);
            }

            // Construct the final upload URI with the digest parameter
            String separator = locationUrl.contains("?") ? "&" : "?";
            URI finalizeUri = URI.create(locationUrl + separator + "digest=" + digest);

            // Upload the content from the temporary file
            try (InputStream uploadStream = Files.newInputStream(tempFile)) {
                OrasHttpClient.ResponseWrapper<String> uploadResponse = client.uploadStream(
                        "PUT",
                        finalizeUri,
                        uploadStream,
                        size,
                        Map.of(Const.CONTENT_TYPE_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE));

                if (uploadResponse.statusCode() != 201 && uploadResponse.statusCode() != 202) {
                    throw new OrasException("Failed to upload blob: " + uploadResponse.statusCode() + " - Response: "
                            + uploadResponse.response());
                }

                return Layer.fromDigest(digest, size);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Error during blob upload: " + e.getMessage());
            e.printStackTrace();
            throw new OrasException("Failed to push blob stream", e);
        } finally {
            // Clean up the temporary file
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    LOG.warn("Failed to delete temporary file: {}", tempFile, e);
                }
            }
        }
    }

    /**
     * Bites to hex string
     * @param bytes of bytes[]
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
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
}
