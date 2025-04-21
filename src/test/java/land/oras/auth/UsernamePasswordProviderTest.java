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

import static org.junit.jupiter.api.Assertions.assertEquals;

import land.oras.ContainerRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class UsernamePasswordProviderTest {

    @Test
    void shouldReturnCorrectValues() {
        AbstractUsernamePasswordProvider authProvider = new UsernamePasswordProvider("user", "pass");

        // Same for any registry
        assertEquals(
                "Basic dXNlcjpwYXNz",
                authProvider.getAuthHeader(ContainerRef.parse("localhost:5000/foo")),
                "Auth header should be correct");
        assertEquals(
                "Basic dXNlcjpwYXNz",
                authProvider.getAuthHeader(ContainerRef.parse("docker.io/bar")),
                "Auth header should be correct");

        // Getters
        assertEquals("user", authProvider.getUsername(), "Username should be correct");
        assertEquals("pass", authProvider.getPassword(), "Password should be correct");
    }
}
