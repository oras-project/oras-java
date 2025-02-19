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
import org.jspecify.annotations.NonNull;

/**
 * A provider for username and password authentication
 */
public abstract class AbstractUsernamePasswordProvider implements AuthProvider {

    /**
     * The username
     */
    private final String username;

    /**
     * The password
     */
    private final String password;

    /**
     * Create a new username and password provider
     * @param username The username
     * @param password The password
     */
    public AbstractUsernamePasswordProvider(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Get the username
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get the password
     * @return The password
     */
    public String getPassword() {
        return password;
    }

    @Override
    @NonNull
    public String getAuthHeader(ContainerRef registry) {
        return "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }
}
