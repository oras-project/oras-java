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

package land.oras.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache for storing token responses based on scopes.
 */
@NullMarked
public final class TokenCache {

    /**
     * Hard cache limit
     */
    public static final int MAX_CACHE_SIZE = 500;

    /**
     * Logger for this class
     */
    private static final Logger LOG = LoggerFactory.getLogger(TokenCache.class);

    /**
     * Private constructor to prevent instantiation of the utility class.
     */
    private TokenCache() {
        // Private constructor to prevent instantiation
    }

    /**
     * Cache for storing service information based on the service URL. This is used to avoid redundant
     */
    private static final Cache<String, String> SERVICE_CACHE =
            Caffeine.newBuilder().maximumSize(MAX_CACHE_SIZE).build();

    /**
     * The cache
     */
    private static final Cache<Scopes, HttpClient.TokenResponse> CACHE = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .expireAfter(new Expiry<Scopes, HttpClient.TokenResponse>() {
                @Override
                public long expireAfterCreate(Scopes key, HttpClient.TokenResponse token, long currentTime) {
                    return getExpiration(token);
                }

                @Override
                public long expireAfterUpdate(
                        Scopes key, HttpClient.TokenResponse token, long currentTime, long currentDuration) {
                    return currentDuration;
                }

                @Override
                public long expireAfterRead(
                        Scopes key, HttpClient.TokenResponse token, long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            .build();

    /**
     * Put a token response in the cache with the associated scopes.
     * @param scopes The scopes associated with the token response
     * @param token The token response to be cached
     */
    public static void put(Scopes scopes, HttpClient.TokenResponse token) {
        Scopes newScopes = scopes.getService() != null ? scopes : scopes.withService(token.service());
        LOG.trace("Caching token for scopes: {}", newScopes);
        CACHE.put(newScopes, token);
        if (newScopes.getService() != null) {
            LOG.trace("Caching service '{}' for registry '{}'", newScopes.getService(), newScopes.getRegistry());
            SERVICE_CACHE.put(scopes.getRegistry(), newScopes.getService());
        }
        // Store global scopes if present for future lookups
        if (scopes.hasGlobalScopes()) {
            Scopes newScopesWithService =
                    scopes.withService(newScopes.getService()).withOnlyGlobalScopes();
            CACHE.put(newScopesWithService, token);
            Scopes newScopesWithoutGlobal =
                    scopes.withService(newScopes.getService()).withoutGlobalScopes();
            CACHE.put(newScopesWithoutGlobal, token);
        }
    }

    /**
     * Get a token response from the cache based on the provided scopes.
     * @param scopes The scopes to look up in the cache
     * @return the token response associated with the scopes, or null if not found or expired
     */
    public static HttpClient.@Nullable TokenResponse get(Scopes scopes) {
        String service =
                scopes.getService() != null ? scopes.getService() : SERVICE_CACHE.getIfPresent(scopes.getRegistry());
        HttpClient.TokenResponse token = CACHE.getIfPresent(scopes);
        if (token != null) {
            LOG.trace("Direct cache hit for scopes: {}", scopes);
            return token;
        }
        // Try lookup with service
        token = CACHE.getIfPresent(scopes.withService(service));
        if (token != null) {
            LOG.trace("Cache lookup for scopes: {}, found with service '{}'", scopes, service);
            return token;
        }
        // Check englobing scopes
        if (scopes.isPullOnly() && !scopes.hasGlobalScopes()) {
            Scopes newScopes = scopes.withService(service).withAddedRegistryScopes(Scope.PUSH); // Just add push scope
            token = CACHE.getIfPresent(newScopes);
            if (token != null) {
                LOG.trace("Cache lookup for scopes: {}, found with push scope", scopes);
                return token;
            }
            newScopes = newScopes.withService(service).withAddedRegistryScopes(Scope.DELETE); // Add delete scope
            token = CACHE.getIfPresent(newScopes);
            if (token != null) {
                LOG.trace("Cache lookup for scopes: {}, found with push and delete scopes", scopes);
                return token;
            }
            newScopes = newScopes.withService(service).withRegistryScopes(Scope.ALL); // All replace all scopes
            token = CACHE.getIfPresent(newScopes);
            if (token != null) {
                LOG.trace("Cache lookup for scopes: {}, found with all scopes", scopes);
                return token;
            }
            return null;
        }
        if (scopes.hasGlobalScopes()) {
            token = CACHE.getIfPresent(scopes.withOnlyGlobalScopes());
            if (token != null) {
                LOG.trace("Cache lookup for scopes: {}, found with only global scopes", scopes);
                return token;
            }
        }
        LOG.trace("Cache miss for scopes: {}", scopes);
        return null;
    }

    /**
     * Get the expiration time for a token response.
     * @param token The token response
     * @return the expiration time in nanoseconds or 60 seconds if expires_in is not provided
     */
    private static long getExpiration(HttpClient.TokenResponse token) {
        if (token.expires_in() == null) {
            return TimeUnit.SECONDS.toNanos(60);
        }
        return TimeUnit.SECONDS.toNanos(token.expires_in());
    }
}
