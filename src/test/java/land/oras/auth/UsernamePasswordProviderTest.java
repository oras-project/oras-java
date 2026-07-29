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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import land.oras.ContainerRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class UsernamePasswordProviderTest {

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

    @Test
    void identityShouldDependOnUsernameOnlyAndNeverLeakThePassword() {
        ContainerRef registry = ContainerRef.parse("localhost:5000/foo");
        AbstractUsernamePasswordProvider user = new UsernamePasswordProvider("user", "pass");
        AbstractUsernamePasswordProvider sameUserOtherPassword = new UsernamePasswordProvider("user", "other-pass");
        AbstractUsernamePasswordProvider otherUser = new UsernamePasswordProvider("other-user", "pass");

        assertEquals(
                user.getIdentity(registry),
                sameUserOtherPassword.getIdentity(registry),
                "Identity should not depend on the password");
        assertNotEquals(
                user.getIdentity(registry),
                otherUser.getIdentity(registry),
                "Different usernames are different identities");
        assertFalse(user.getIdentity(registry).contains("pass"), "Identity must never contain the raw password");
    }
}
