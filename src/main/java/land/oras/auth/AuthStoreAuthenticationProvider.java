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
import land.oras.auth.AuthStore.Credential;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * AuthStoreAuthenticationProvider is an implementation of the {@link AuthProvider} interface.
 * It retrieves credentials from a {@link AuthStore} and generates a Basic Authentication header.
 */
@NullMarked
public final class AuthStoreAuthenticationProvider implements AuthProvider {

    private final AuthStore authStore;

    /**
     * Default constructor
     */
    public AuthStoreAuthenticationProvider() {
        this(AuthStore.newStore());
    }

    /**
     * Constructor.
     *
     * @param authStore The FileStore instance to retrieve credentials from.
     */
    public AuthStoreAuthenticationProvider(AuthStore authStore) {
        this.authStore = authStore;
    }

    @Override
    @Nullable
    public String getAuthHeader(ContainerRef registry) {
        Credential credential = authStore.get(registry);
        if (credential == null) {
            return null;
        }
        return new UsernamePasswordProvider(credential.username(), credential.password()).getAuthHeader(registry);
    }

    @Override
    public AuthScheme getAuthScheme() {
        return AuthScheme.BASIC;
    }
}
