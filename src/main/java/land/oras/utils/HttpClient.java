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

package land.oras.utils;

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
import land.oras.auth.AuthProvider;
import land.oras.auth.AuthScheme;
import land.oras.exception.OrasException;
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
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<String> get(URI uri, Map<String, String> headers, AuthProvider authProvider) {
        return executeRequest(
                "GET",
                uri,
                headers,
                new byte[0],
                HttpResponse.BodyHandlers.ofString(),
                HttpRequest.BodyPublishers.noBody(),
                authProvider);
    }

    /**
     * Download to a file
     * @param uri The URI
     * @param headers The headers
     * @param file The file
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<Path> download(URI uri, Map<String, String> headers, Path file, AuthProvider authProvider) {
        return executeRequest(
                "GET",
                uri,
                headers,
                new byte[0],
                HttpResponse.BodyHandlers.ofFile(file),
                HttpRequest.BodyPublishers.noBody(),
                authProvider);
    }

    /**
     * Download to to input stream
     * @param uri The URI
     * @param headers The headers
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<InputStream> download(URI uri, Map<String, String> headers, AuthProvider authProvider) {
        return executeRequest(
                "GET",
                uri,
                headers,
                new byte[0],
                HttpResponse.BodyHandlers.ofInputStream(),
                HttpRequest.BodyPublishers.noBody(),
                authProvider);
    }

    /**
     * Upload a file
     * @param method The method (POST or PUT)
     * @param uri The URI
     * @param headers The headers
     * @param file The file
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<String> upload(
            String method, URI uri, Map<String, String> headers, Path file, AuthProvider authProvider) {
        try {
            return executeRequest(
                    method,
                    uri,
                    headers,
                    new byte[0],
                    HttpResponse.BodyHandlers.ofString(),
                    HttpRequest.BodyPublishers.ofFile(file),
                    authProvider);
        } catch (Exception e) {
            throw new OrasException("Unable to upload file", e);
        }
    }

    /**
     * Perform a HEAD request
     * @param uri The URI
     * @param headers The headers
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<String> head(URI uri, Map<String, String> headers, AuthProvider authProvider) {
        return executeRequest(
                "HEAD",
                uri,
                headers,
                new byte[0],
                HttpResponse.BodyHandlers.ofString(),
                HttpRequest.BodyPublishers.noBody(),
                authProvider);
    }

    /**
     * Perform a DELETE request
     * @param uri The URI
     * @param headers The headers
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<String> delete(URI uri, Map<String, String> headers, AuthProvider authProvider) {
        return executeRequest(
                "DELETE",
                uri,
                headers,
                new byte[0],
                HttpResponse.BodyHandlers.ofString(),
                HttpRequest.BodyPublishers.noBody(),
                authProvider);
    }

    /**
     * Perform a POST request. Might not be suitable for large files. Use upload for large files.
     * @param uri The URI.
     * @param body The body
     * @param headers The headers
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<String> post(URI uri, byte[] body, Map<String, String> headers, AuthProvider authProvider) {
        return executeRequest(
                "POST",
                uri,
                headers,
                body,
                HttpResponse.BodyHandlers.ofString(),
                HttpRequest.BodyPublishers.ofByteArray(body),
                authProvider);
    }

    /**
     * Perform a Patch request
     * @param uri The URI
     * @param body The body
     * @param headers The headers
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<String> patch(URI uri, byte[] body, Map<String, String> headers, AuthProvider authProvider) {
        return executeRequest(
                "PATCH",
                uri,
                headers,
                body,
                HttpResponse.BodyHandlers.ofString(),
                HttpRequest.BodyPublishers.ofByteArray(body),
                authProvider);
    }

    /**
     * Perform a PUT request
     * @param uri The URI
     * @param body The body
     * @param headers The headers
     * @param authProvider The authentication provider
     * @return The response
     */
    public ResponseWrapper<String> put(URI uri, byte[] body, Map<String, String> headers, AuthProvider authProvider) {
        return executeRequest(
                "PUT",
                uri,
                headers,
                body,
                HttpResponse.BodyHandlers.ofString(),
                HttpRequest.BodyPublishers.ofByteArray(body),
                authProvider);
    }

    /**
     * Retrieve a token from the registry
     * @param response The response that may contain a the WWW-Authenticate header
     * @param authProvider The authentication provider
     * @param <T> The response type
     * @return The token
     */
    public <T> HttpClient.ResponseWrapper<String> refreshToken(
            HttpClient.ResponseWrapper<T> response, AuthProvider authProvider) {

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

        LOG.debug("WWW-Authenticate header: realm={}, service={}, scope={}, error={}", realm, service, scope, error);

        URI uri = URI.create(realm + "?scope=" + scope + "&service=" + service);

        // Perform the request to get the token
        Map<String, String> headers = new HashMap<>();
        HttpClient.ResponseWrapper<String> responseWrapper = get(uri, headers, authProvider);

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

        return responseWrapper;
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
            AuthProvider authProvider) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri).method(method, bodyPublisher);

            // Add authentication header if any
            ContainerRef registry = ContainerRef.fromUrl(uri.toASCIIString());
            if (authProvider.getAuthHeader(registry) != null
                    && !authProvider.getAuthScheme().equals(AuthScheme.NONE)) {
                builder = builder.header(Const.AUTHORIZATION_HEADER, authProvider.getAuthHeader(registry));
            }
            headers.forEach(builder::header);

            // Add user agent
            builder = builder.header(Const.USER_AGENT_HEADER, Const.USER_AGENT_VALUE);

            HttpRequest request = builder.build();
            logRequest(request, body);
            HttpResponse<T> response = client.send(request, handler);

            if (response.statusCode() == HttpURLConnection.HTTP_MOVED_PERM
                    || response.statusCode() == HttpURLConnection.HTTP_MOVED_TEMP
                    || response.statusCode() == 307) {

                LOG.debug(
                        "Redirecting to {}",
                        response.headers().firstValue("Location").orElseThrow());
                URI redirectUri =
                        new URI(response.headers().firstValue("Location").orElseThrow());
                HttpRequest.Builder newBuilder =
                        HttpRequest.newBuilder().uri(redirectUri).method(method, bodyPublisher);
                HttpRequest newRequest = newBuilder.build();
                logRequest(newRequest, body);
                HttpResponse<T> newResponse = client.send(newRequest, handler);
                return redoRequest(newResponse, newBuilder, handler, authProvider);
            }
            return redoRequest(response, builder, handler, authProvider);
        } catch (Exception e) {
            LOG.error("Failed to execute request", e);
            throw new OrasException("Unable to create HTTP request", e);
        }
    }

    /**
     * Redo the request based on the response
     * @param response The response
     * @param builder The request builder
     * @return The response wrapper
     * @param <T> The response type
     */
    private <T> ResponseWrapper<T> redoRequest(
            HttpResponse<T> response,
            HttpRequest.Builder builder,
            HttpResponse.BodyHandler<T> handler,
            AuthProvider authProvider) {
        if ((response.statusCode() == 401 || response.statusCode() == 403)) {
            LOG.debug("Requesting new token...");
            ResponseWrapper<String> tokenResponse = refreshToken(toResponseWrapper(response), authProvider);
            HttpClient.TokenResponse token =
                    JsonUtils.fromJson(tokenResponse.response(), HttpClient.TokenResponse.class);
            LOG.debug(
                    "Found token issued_at {}, expire_id {} and expiring at {} ",
                    token.issued_at(),
                    token.expires_in(),
                    token.issued_at().plusSeconds(token.expires_in()));
            try {
                builder = builder.header(Const.AUTHORIZATION_HEADER, "Bearer " + token.token());
                return toResponseWrapper(client.send(builder.build(), handler));
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
    public record TokenResponse(String token, String access_token, Integer expires_in, ZonedDateTime issued_at) {}

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
