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

import land.oras.ContainerRef;
import org.jspecify.annotations.Nullable;

/**
 * Interface for auth provider
 * Must return the authentication header to pass to HTTP requests
 */
public interface AuthProvider {

    /**
     * Get the authentication header for this provider
     * @param registry The registry
     * @return The authentication header or null if not applicable
     */
    @Nullable
    String getAuthHeader(ContainerRef registry);

    /**
     * Get the authentication scheme for this provider
     * @return The authentication scheme
     */
    AuthScheme getAuthScheme();

    /**
     * Get an opaque identity marker for the credentials this provider resolves for the given registry. This marker is
     * used to key {@link TokenCache} entries alongside the requested {@link Scopes}
     * @param registry The registry
     * @return A non-null identity marker
     */
    default String getIdentity(ContainerRef registry) {
        return getAuthScheme().name();
    }
}
