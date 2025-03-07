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

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import land.oras.ContainerRef;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import land.oras.utils.OrasHttpClient;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A provider for bearer token authentication
 */
@NullMarked
public final class BearerTokenProvider implements AuthProvider {

    /**
     * The pattern for the WWW-Authenticate header value
     */
    private static final Pattern WWW_AUTH_VALUE_PATTERN =
            Pattern.compile("Bearer realm=\"([^\"]+)\",service=\"([^\"]+)\",scope=\"([^\"]+)\"(,error=\"([^\"]+)\")?");

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(BearerTokenProvider.class);

    /**
     * The refreshed token
     */
    private @Nullable TokenResponse token;

    /**
     * The provider for username and password in case of refresh token done
     */
    private final AuthProvider provider;

    /**
     * Create a new bearer token provider
     * @param provider The provider for username and password
     */
    public BearerTokenProvider(AuthProvider provider) {
        this.provider = provider;
    }

    /**
     * Retrieve
     * @param response The response
     * @param client The original client
     * @param containerRef The container reference
     * @return The token
     */
    public BearerTokenProvider refreshToken(
            ContainerRef containerRef, OrasHttpClient client, OrasHttpClient.ResponseWrapper<String> response) {

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
        String authHeader = provider.getAuthHeader(containerRef);
        if (authHeader != null) {
            headers.put(Const.AUTHORIZATION_HEADER, authHeader);
        }
        OrasHttpClient.ResponseWrapper<String> responseWrapper = client.get(uri, headers);

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

        this.token = JsonUtils.fromJson(responseWrapper.response(), TokenResponse.class);
        return this;
    }

    /**
     * Get the token
     * @return The token
     */
    public @Nullable TokenResponse getToken() {
        return token;
    }

    @Override
    public @Nullable String getAuthHeader(ContainerRef registry) {
        if (token == null) {
            return null;
        }
        return "Bearer " + token.token;
    }

    /**
     * The token response
     * @param token The token
     * @param access_token The access token
     * @param expire_in The expire in
     * @param issued_at The issued at
     */
    @NullMarked
    public record TokenResponse(String token, String access_token, Integer expire_in, ZonedDateTime issued_at) {}
}
