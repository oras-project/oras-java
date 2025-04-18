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

import land.oras.ContainerRef;
import land.oras.utils.HttpClient;
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
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(BearerTokenProvider.class);

    /**
     * The refreshed token
     */
    private HttpClient.@Nullable TokenResponse token;

    /**
     * Create a new bearer token provider
     */
    public BearerTokenProvider() {}

    /**
     * Get the token
     * @return The token
     */
    public HttpClient.@Nullable TokenResponse getToken() {
        return token;
    }

    /**
     * Set the token
     * @param token The token
     */
    public void setToken(HttpClient.TokenResponse token) {
        this.token = token;
    }

    @Override
    public @Nullable String getAuthHeader(ContainerRef registry) {
        if (token == null) {
            LOG.debug("No token available. No header will be set.");
            return null;
        }
        return "Bearer " + token.token();
    }

    @Override
    public AuthScheme getAuthScheme() {
        return AuthScheme.BEARER;
    }
}
