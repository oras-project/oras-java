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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class RegistryConfTest {

    @TempDir
    private static Path homeDir;

    @BeforeAll
    static void init() throws Exception {
        Files.createDirectory(homeDir.resolve(".config"));
        Files.createDirectory(homeDir.resolve(".config").resolve("containers"));
    }

    @Test
    void shouldCheckRegistryStatus() {

        // With null
        RegistriesConf.RegistryConfig registry = new RegistriesConf.RegistryConfig("test", null, null);
        assertEquals("test", registry.location());
        assertNull(registry.blocked(), "Blocked should be null when not set");
        assertNull(registry.insecure(), "Insecure should be null when not set");
        assertFalse(registry.isBlocked(), "Registry should not be blocked when blocked is null");
        assertFalse(registry.isInsecure(), "Registry should not be insecure when insecure is null");

        // With blocked true
        registry = new RegistriesConf.RegistryConfig("test", true, null);
        assertTrue(registry.isBlocked(), "Registry should be blocked when blocked is true");

        // With insecure true
        registry = new RegistriesConf.RegistryConfig("test", null, true);
        assertTrue(registry.isInsecure(), "Registry should be insecure when insecure is true");

        // With blocked false
        registry = new RegistriesConf.RegistryConfig("test", false, null);
        assertFalse(registry.isBlocked(), "Registry should be blocked when blocked is false");

        // With insecure false
        registry = new RegistriesConf.RegistryConfig("test", null, false);
        assertFalse(registry.isInsecure(), "Registry should be insecure when insecure is false");
    }
}
