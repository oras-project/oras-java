package land.oras.utils;

import java.io.InputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import land.oras.OrasException;
import land.oras.auth.AuthProvider;
import land.oras.auth.NoAuthProvider;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client for ORAS
 */
@NullMarked
public final class OrasHttpClient {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(OrasHttpClient.class);

    /**
     * The HTTP client builder
     */
    private final HttpClient.Builder builder;

    /**
     * The HTTP client
     */
    private HttpClient client;

    /**
     * The authentication provider
     */
    private AuthProvider authProvider;

    /**
     * Skip TLS verification
     */
    private boolean skipTlsVerify;

    /**
     * Timeout in seconds
     */
    private Integer timeout;

    /**
     * Hidden constructor
     */
    private OrasHttpClient() {
        this.builder = HttpClient.newBuilder();
        this.builder.followRedirects(HttpClient.Redirect.NORMAL); // Some registry might redirect blob to other domain
        this.skipTlsVerify = false;
        this.builder.cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_NONE));
        this.authProvider = new NoAuthProvider();
        this.setTimeout(60);
    }

    /**
     * Set the timeout
     * @param timeout The timeout in seconds
     */
    private void setTimeout(@Nullable Integer timeout) {
        if (timeout != null) {
            this.timeout = timeout;
            this.builder.connectTimeout(Duration.ofSeconds(timeout));
        }
    }

    /**
     * Set the authentication
     * @param authProvider The auth provider
     */
    private void setAuthentication(@Nullable AuthProvider authProvider) {
        if (authProvider == null) {
            this.authProvider = new NoAuthProvider();
        }
        this.authProvider = authProvider;
    }

    /**
     * Update the authentication method for this client
     * Typically used to change from basic to bearer token authentication or
     * no auth to basic auth
     * @param authProvider The auth provider
     */
    public void updateAuthentication(AuthProvider authProvider) {
        setAuthentication(authProvider);
    }

    /**
     * Skip the TLS verification
     * @param skipTlsVerify Skip TLS verification
     */
    private void setTlsVerify(boolean skipTlsVerify) {
        this.skipTlsVerify = skipTlsVerify;
        if (skipTlsVerify) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[] {new InsecureTrustManager()}, new SecureRandom());
                builder.sslContext(sslContext);
            } catch (Exception e) {
                throw new OrasException("Unable to skip TLS verification", e);
            }
        }
    }

    /**
     * Create a new HTTP client
     * @return The client
     */
    public OrasHttpClient build() {
        this.client = this.builder.build();
        return this;
    }

    /**
     * Perform a GET request
     * @param uri The URI
     * @param headers The headers
     * @return The response
     */
    public ResponseWrapper<String> get(URI uri, Map<String, String> headers) {
        return executeRequest(
                "GET",
                uri,
                headers,
                new byte[0],
                HttpResponse.BodyHandlers.ofString(),
                HttpRequest.BodyPublishers.noBody());
    }

    /**
     * Download to a file
     * @param uri The URI
     * @param headers The headers
     * @param file The file
     * @return The response
     */
    public ResponseWrapper<Path> download(URI uri, Map<String, String> headers, Path file) {
        return executeRequest(
                "GET",
                uri,
                headers,
                new byte[0],
                HttpResponse.BodyHandlers.ofFile(file),
                HttpRequest.BodyPublishers.noBody());
    }

    /**
     * Download to to input stream
     * @param uri The URI
     * @param headers The headers
     * @return The response
     */
    public ResponseWrapper<InputStream> download(URI uri, Map<String, String> headers) {
        return executeRequest(
                "GET",
                uri,
                headers,
                new byte[0],
                HttpResponse.BodyHandlers.ofInputStream(),
                HttpRequest.BodyPublishers.noBody());
    }

    /**
     * Upload a file
     * @param method The method (POST or PUT)
     * @param uri The URI
     * @param headers The headers
     * @param file The file
     * @return The response
     */
    public ResponseWrapper<String> upload(String method, URI uri, Map<String, String> headers, Path file) {
        try {
            return executeRequest(
                    method,
                    uri,
                    headers,
                    new byte[0],
                    HttpResponse.BodyHandlers.ofString(),
                    HttpRequest.BodyPublishers.ofFile(file));
        } catch (Exception e) {
            throw new OrasException("Unable to upload file", e);
        }
    }

    /**
     * Perform a HEAD request
     * @param uri The URI
     * @param headers The headers
     * @return The response
     */
    public ResponseWrapper<String> head(URI uri, Map<String, String> headers) {
        return executeRequest(
                "HEAD",
                uri,
                headers,
                new byte[0],
                HttpResponse.BodyHandlers.ofString(),
                HttpRequest.BodyPublishers.noBody());
    }

    /**
     * Perform a DELETE request
     * @param uri The URI
     * @param headers The headers
     * @return The response
     */
    public ResponseWrapper<String> delete(URI uri, Map<String, String> headers) {
        return executeRequest(
                "DELETE",
                uri,
                headers,
                new byte[0],
                HttpResponse.BodyHandlers.ofString(),
                HttpRequest.BodyPublishers.noBody());
    }

    /**
     * Perform a POST request. Might not be suitable for large files. Use upload for large files.
     * @param uri The URI.
     * @param body The body
     * @param headers The headers
     * @return The response
     */
    public ResponseWrapper<String> post(URI uri, byte[] body, Map<String, String> headers) {
        return executeRequest(
                "POST",
                uri,
                headers,
                body,
                HttpResponse.BodyHandlers.ofString(),
                HttpRequest.BodyPublishers.ofByteArray(body));
    }

    /**
     * Perform a PUT request
     * @param uri The URI
     * @param body The body
     * @param headers The headers
     * @return The response
     */
    public ResponseWrapper<String> put(URI uri, byte[] body, Map<String, String> headers) {
        return executeRequest(
                "PUT",
                uri,
                headers,
                body,
                HttpResponse.BodyHandlers.ofString(),
                HttpRequest.BodyPublishers.ofByteArray(body));
    }

    /**
     * Execute a request
     * @param method The method
     * @param uri The URI
     * @param headers The headers
     * @param body The body
     * @param handler The response handler
     * @param bodyPublisher The body publisher
     * @return The response
     */
    private <T> ResponseWrapper<T> executeRequest(
            String method,
            URI uri,
            Map<String, String> headers,
            byte[] body,
            HttpResponse.BodyHandler<T> handler,
            HttpRequest.BodyPublisher bodyPublisher) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri).method(method, bodyPublisher);

            // Add authentication header if any
            if (this.authProvider.getAuthHeader() != null) {
                builder = builder.header(Const.AUTHORIZATION_HEADER, authProvider.getAuthHeader());
            }
            headers.forEach(builder::header);
            HttpRequest request = builder.build();
            logRequest(request, body);
            HttpResponse<T> response = client.send(request, handler);
            return new ResponseWrapper<T>(
                    response.body(),
                    response.statusCode(),
                    response.headers().map().entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey, e -> e.getValue().get(0))));
        } catch (Exception e) {
            throw new OrasException("Unable to create HTTP request", e);
        }
    }

    /**
     * Logs the request in debug/trace mode
     * @param request The request
     * @param body The body
     */
    private void logRequest(HttpRequest request, byte[] body) {
        LOG.debug("Executing {} request to {}", request.method(), request.uri());
        LOG.debug(
                "Headers: {}",
                request.headers().map().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> Const.AUTHORIZATION_HEADER.equalsIgnoreCase(entry.getKey())
                                        ? List.of("<redacted>") // Replace value with ****
                                        : entry.getValue())));
        // Log the body in trace mode
        if (LOG.isTraceEnabled()) {
            LOG.trace("Body: {}", new String(body, StandardCharsets.UTF_8));
        }
    }

    /**
     * Response wrapper
     * @param <T> The response type
     * @param response The response
     * @param statusCode The status code
     * @param headers The headers
     */
    public record ResponseWrapper<T>(T response, int statusCode, Map<String, String> headers) {}

    /**
     * Insecure trust manager when skipping TLS verification
     */
    private static class InsecureTrustManager extends X509ExtendedTrustManager {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {}

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {}
    }

    /**
     * Builder for the HTTP client
     */
    public static class Builder {
        private final OrasHttpClient client = new OrasHttpClient();

        /**
         * Hidden constructor
         */
        private Builder() {}

        /**
         * Set the timeout
         * @param timeout The timeout in seconds
         * @return The builder
         */
        public Builder withTimeout(@Nullable Integer timeout) {
            client.setTimeout(timeout);
            return this;
        }

        /**
         * Set the authentication
         * @param authProvider The auth provider
         * @return The builder
         */
        public Builder withAuthentication(@Nullable AuthProvider authProvider) {
            client.setAuthentication(authProvider);
            return this;
        }

        /**
         * Skip the TLS verification
         * @param skipTlsVerify Skip TLS verification
         * @return The builder
         */
        public Builder withSkipTlsVerify(boolean skipTlsVerify) {
            client.setTlsVerify(skipTlsVerify);
            return this;
        }

        /**
         * Build the client
         * @return The client
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Build the client
         * @return The client
         */
        public OrasHttpClient build() {
            return client.build();
        }
    }
}
