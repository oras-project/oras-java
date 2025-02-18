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

import org.jspecify.annotations.NullMarked;

/**
 * A provider for username and password authentication
 */
@NullMarked
public class UsernamePasswordProvider extends AbstractUsernamePasswordProvider {

    /**
     * Create a new username and password provider
     * @param username The username
     * @param password The password
     */
    public UsernamePasswordProvider(String username, String password) {
        super(username, password);
    }
}
