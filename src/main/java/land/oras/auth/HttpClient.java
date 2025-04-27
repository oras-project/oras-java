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

package land.oras.auth;

import java.io.InputStream;
import java.net.*;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import land.oras.ContainerRef;
import land.oras.OrasModel;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import land.oras.utils.Versions;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client for ORAS
 */
@NullMarked
public final class HttpClient {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(HttpClient.class);

    /**
     * The pattern for the WWW-Authenticate header value
     */
    private static final Pattern WWW_AUTH_VALUE_PATTERN =
            Pattern.compile("Bearer realm=\"([^\"]+)\",service=\"([^\"]+)\",scope=\"([^\"]+)\"(,error=\"([^\"]+)\")?");

    /**
     * The HTTP client builder
     */
    private final java.net.http.HttpClient.Builder builder;

    /**
     * The HTTP client
     */
    private java.net.http.HttpClient client;

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
    private HttpClient() {
        this.builder = java.net.http.HttpClient.newBuilder();
        this.builder.followRedirects(
                java.net.http.HttpClient.Redirect
                        .NEVER); // No automatic redirect, only GET and HEAD request will redirect
        this.skipTlsVerify = false;
        this.builder.cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_NONE));
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
    public HttpClient build() {
        this.client = this.builder.build();
        return this;
    }

    /**
     * Perform a GET request
     * @param uri The URI
     * @param headers The headers
     * @param scopes The scopes
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<String> get(URI uri, Map<String, String> headers, Scopes scopes, AuthProvider authProvider) {
        return executeRequest(
                "GET",
                uri,
                headers,
                new byte[0],
                HttpResponse.BodyHandlers.ofString(),
                HttpRequest.BodyPublishers.noBody(),
                scopes,
                authProvider);
    }

    /**
     * Download to a file
     * @param uri The URI
     * @param headers The headers
     * @param file The file
     * @param scopes The scopes
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<Path> download(
            URI uri, Map<String, String> headers, Path file, Scopes scopes, AuthProvider authProvider) {
        return executeRequest(
                "GET",
                uri,
                headers,
                new byte[0],
                HttpResponse.BodyHandlers.ofFile(file),
                HttpRequest.BodyPublishers.noBody(),
                scopes,
                authProvider);
    }

    /**
     * Download to to input stream
     * @param uri The URI
     * @param headers The headers
     * @param scopes The scopes
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<InputStream> download(
            URI uri, Map<String, String> headers, Scopes scopes, AuthProvider authProvider) {
        return executeRequest(
                "GET",
                uri,
                headers,
                new byte[0],
                HttpResponse.BodyHandlers.ofInputStream(),
                HttpRequest.BodyPublishers.noBody(),
                scopes,
                authProvider);
    }

    /**
     * Upload a file
     * @param method The method (POST or PUT)
     * @param uri The URI
     * @param headers The headers
     * @param file The file
     * @param scopes The scopes
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<String> upload(
            String method, URI uri, Map<String, String> headers, Path file, Scopes scopes, AuthProvider authProvider) {
        try {
            return executeRequest(
                    method,
                    uri,
                    headers,
                    new byte[0],
                    HttpResponse.BodyHandlers.ofString(),
                    HttpRequest.BodyPublishers.ofFile(file),
                    scopes,
                    authProvider);
        } catch (Exception e) {
            throw new OrasException("Unable to upload file", e);
        }
    }

    /**
     * Perform a HEAD request
     * @param uri The URI
     * @param headers The headers
     * @param scopes The scopes
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<String> head(
            URI uri, Map<String, String> headers, Scopes scopes, AuthProvider authProvider) {
        return executeRequest(
                "HEAD",
                uri,
                headers,
                new byte[0],
                HttpResponse.BodyHandlers.ofString(),
                HttpRequest.BodyPublishers.noBody(),
                scopes,
                authProvider);
    }

    /**
     * Perform a DELETE request
     * @param uri The URI
     * @param headers The headers
     * @param scopes The scopes
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<String> delete(
            URI uri, Map<String, String> headers, Scopes scopes, AuthProvider authProvider) {
        return executeRequest(
                "DELETE",
                uri,
                headers,
                new byte[0],
                HttpResponse.BodyHandlers.ofString(),
                HttpRequest.BodyPublishers.noBody(),
                scopes,
                authProvider);
    }

    /**
     * Perform a POST request. Might not be suitable for large files. Use upload for large files.
     * @param uri The URI.
     * @param body The body
     * @param headers The headers
     * @param scopes The scopes
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<String> post(
            URI uri, byte[] body, Map<String, String> headers, Scopes scopes, AuthProvider authProvider) {
        return executeRequest(
                "POST",
                uri,
                headers,
                body,
                HttpResponse.BodyHandlers.ofString(),
                HttpRequest.BodyPublishers.ofByteArray(body),
                scopes,
                authProvider);
    }

    /**
     * Perform a Patch request
     * @param uri The URI
     * @param body The body
     * @param headers The headers
     * @param scopes The scopes
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<String> patch(
            URI uri, byte[] body, Map<String, String> headers, Scopes scopes, AuthProvider authProvider) {
        return executeRequest(
                "PATCH",
                uri,
                headers,
                body,
                HttpResponse.BodyHandlers.ofString(),
                HttpRequest.BodyPublishers.ofByteArray(body),
                scopes,
                authProvider);
    }

    /**
     * Perform a PUT request
     * @param uri The URI
     * @param body The body
     * @param headers The headers
     * @param scopes The scopes
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<String> put(
            URI uri, byte[] body, Map<String, String> headers, Scopes scopes, AuthProvider authProvider) {
        return executeRequest(
                "PUT",
                uri,
                headers,
                body,
                HttpResponse.BodyHandlers.ofString(),
                HttpRequest.BodyPublishers.ofByteArray(body),
                scopes,
                authProvider);
    }

    /**
     * Retrieve a token from the registry
     * @param response The response that may contain a the WWW-Authenticate header
     * @param scopes The scopes
     * @param authProvider The authentication provider
     * @param <T> The response type
     * @return The token
     */
    public <T> TokenResponse refreshToken(
            HttpClient.ResponseWrapper<T> response, Scopes scopes, AuthProvider authProvider) {

        String wwwAuthHeader = response.headers().getOrDefault(Const.WWW_AUTHENTICATE_HEADER.toLowerCase(), "");
        LOG.debug("WWW-Authenticate header: {}", wwwAuthHeader);
        if (wwwAuthHeader.isEmpty()) {
            throw new OrasException("No WWW-Authenticate header found in response");
        }

        Matcher matcher = WWW_AUTH_VALUE_PATTERN.matcher(wwwAuthHeader);
        if (!matcher.matches()) {
            throw new OrasException("Invalid WWW-Authenticate header value: " + wwwAuthHeader);
        }

        // Extract parts
        String realm = matcher.group(1);
        String service = matcher.group(2);
        String scope = matcher.group(3);
        String error = matcher.group(5);

        // Add server scope to existing scopes
        Scopes newScopes = scopes.withNewScope(scope);
        LOG.debug("New scopes with server: {}", newScopes.getScopes());

        LOG.debug("WWW-Authenticate header: realm={}, service={}, scope={}, error={}", realm, service, scope, error);

        URI uri = URI.create(realm + "?scope=" + scope + "&service=" + service);

        // Perform the request to get the token
        Map<String, String> headers = new HashMap<>();
        HttpClient.ResponseWrapper<String> responseWrapper = get(uri, headers, scopes, authProvider);

        // Log the response
        LOG.debug(
                "Response: {}",
                responseWrapper
                        .response()
                        .replaceAll("\"token\"\\s*:\\s*\"([A-Za-z0-9\\-_\\.]+)\"", "\"token\":\"<redacted>\"")
                        .replaceAll(
                                "\"access_token\"\\s*:\\s*\"([A-Za-z0-9\\-_\\.]+)\"",
                                "\"access_token\":\"<redacted>\""));
        LOG.debug(
                "Headers: {}",
                responseWrapper.headers().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> Const.AUTHORIZATION_HEADER.equalsIgnoreCase(entry.getKey())
                                        ? "<redacted" // Replace value with ****
                                        : entry.getValue())));

        return JsonUtils.fromJson(responseWrapper.response(), TokenResponse.class);
    }

    /**
     * Execute a request
     * @param method The method
     * @param uri The URI
     * @param headers The headers
     * @param body The body
     * @param handler The response handler
     * @param bodyPublisher The body publisher
     * @param authProvider The authentication provider
     * @return The response
     */
    private <T> ResponseWrapper<T> executeRequest(
            String method,
            URI uri,
            Map<String, String> headers,
            byte[] body,
            HttpResponse.BodyHandler<T> handler,
            HttpRequest.BodyPublisher bodyPublisher,
            Scopes scopes,
            AuthProvider authProvider) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri).method(method, bodyPublisher);

            // Get scope based on method
            ContainerRef containerRef = scopes.getContainerRef();
            Scopes newScopes =
                    switch (method) {
                        case "GET", "HEAD" -> scopes.withNewRegistryScopes(Scope.PULL);
                        case "POST", "PUT", "PATCH" -> scopes.withNewRegistryScopes(Scope.PUSH);
                        case "DELETE" -> scopes.withNewRegistryScopes(Scope.DELETE);
                        default -> throw new OrasException("Unsupported HTTP method: " + method);
                    };

            LOG.debug("Existing scopes: {}", scopes.getScopes());
            LOG.debug("New scopes: {}", newScopes.getScopes());

            // Add authentication header if any
            if (authProvider.getAuthHeader(containerRef) != null
                    && !authProvider.getAuthScheme().equals(AuthScheme.NONE)) {
                builder = builder.header(Const.AUTHORIZATION_HEADER, authProvider.getAuthHeader(containerRef));
            }
            headers.forEach(builder::header);

            // Add user agent
            builder = builder.header(Const.USER_AGENT_HEADER, Versions.USER_AGENT_VALUE);

            HttpRequest request = builder.build();
            logRequest(request, body);
            HttpResponse<T> response = client.send(request, handler);

            // Follow redirect
            if (shouldRedirect(response)) {
                String location = getLocationHeader(response);
                LOG.debug("Redirecting to {}", location);
                return executeRequest(
                        method, URI.create(location), headers, body, handler, bodyPublisher, newScopes, authProvider);
            }
            return redoRequest(response, builder, handler, newScopes, authProvider);
        } catch (Exception e) {
            LOG.error("Failed to execute request", e);
            throw new OrasException("Unable to create HTTP request", e);
        }
    }

    private <T> boolean shouldRedirect(HttpResponse<T> response) {
        return response.statusCode() == HttpURLConnection.HTTP_MOVED_PERM
                || response.statusCode() == HttpURLConnection.HTTP_MOVED_TEMP
                || response.statusCode() == 307;
    }

    private <T> String getLocationHeader(HttpResponse<T> response) {
        return response.headers()
                .firstValue("Location")
                .orElseThrow(() -> new OrasException("No Location header found"));
    }

    private <T> ResponseWrapper<T> redoRequest(
            HttpResponse<T> response,
            HttpRequest.Builder builder,
            HttpResponse.BodyHandler<T> handler,
            Scopes scopes,
            AuthProvider authProvider) {
        if ((response.statusCode() == 401 || response.statusCode() == 403)) {
            LOG.debug("Requesting new token...");
            HttpClient.TokenResponse token = refreshToken(toResponseWrapper(response), scopes, authProvider);
            if (token.issued_at() != null && token.expires_in() != null) {
                LOG.debug(
                        "Found token issued_at {}, expire_id {} and expiring at {} ",
                        token.issued_at(),
                        token.expires_in(),
                        token.issued_at().plusSeconds(token.expires_in()));
            }
            try {
                builder = builder.header(Const.AUTHORIZATION_HEADER, "Bearer " + token.token());
                HttpResponse<T> newResponse = client.send(builder.build(), handler);

                // Follow redirect
                if (shouldRedirect(newResponse)) {
                    String location = getLocationHeader(newResponse);
                    LOG.debug("Redirecting after auth to {}", location);
                    return toResponseWrapper(
                            client.send(builder.uri(URI.create(location)).build(), handler));
                }
                return toResponseWrapper(newResponse);

            } catch (Exception e) {
                LOG.error("Failed to redo request", e);
                throw new OrasException("Unable to redo HTTP request", e);
            }
        }
        return toResponseWrapper(response);
    }

    private <T> ResponseWrapper<T> toResponseWrapper(HttpResponse<T> response) {
        return new ResponseWrapper<>(
                response.body(),
                response.statusCode(),
                response.headers().map().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey, e -> e.getValue().get(0))));
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
     * The token response
     * @param token The token
     * @param access_token The access token
     * @param expires_in The expires in
     * @param issued_at The issued at
     */
    @OrasModel
    public record TokenResponse(
            String token,
            @Nullable String access_token,
            @Nullable Integer expires_in,
            @Nullable ZonedDateTime issued_at) {}

    /**
     * Builder for the HTTP client
     */
    public static class Builder {
        private final HttpClient client = new HttpClient();

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
        public HttpClient build() {
            return client.build();
        }
    }
}
