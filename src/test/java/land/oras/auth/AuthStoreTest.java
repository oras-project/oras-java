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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import land.oras.ContainerRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class AuthStoreTest {

    @TempDir
    private Path tempDir;

    private AuthStore authStore;
    private AuthStore.Config mockConfig;
    private AuthStore.Credential mockCredential;
    private ContainerRef SERVER_ADDRESS;
    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";

    @BeforeEach
    void setUp() {
        // Mock Config and Credential
        mockConfig = Mockito.mock(AuthStore.Config.class);
        mockCredential = new AuthStore.Credential(USERNAME, PASSWORD);
        authStore = new AuthStore(mockConfig);
    }

    @Test
    void testNewStore_success() throws Exception {
        // Simulate loading configuration
        AuthStore.Config mockConfig = Mockito.mock(AuthStore.Config.class);
        AuthStore authStoreInstance = new AuthStore(mockConfig);

        assertNotNull(authStoreInstance);
    }

    @Test
    void testNewStore_defaultLocation_success() throws Exception {
        // Simulate loading configuration from default location
        AuthStore authStoreInstance = AuthStore.newStore();
        assertNotNull(authStoreInstance);
    }

    @Test
    void testGetCredential_success() throws Exception {
        // Mock the behavior of getting credentials
        Mockito.when(mockConfig.getCredential(SERVER_ADDRESS)).thenReturn(mockCredential);

        AuthStore.Credential credential = authStore.get(SERVER_ADDRESS);

        assertNotNull(credential);
        assertEquals(USERNAME, credential.username());
        assertEquals(PASSWORD, credential.password());
    }

    @Test
    void testConfigLoad_success() throws Exception {
        // Create a temporary JSON file for testing
        ContainerRef containerRef =
                ContainerRef.parse("docker.io/library/foo/hello-world:latest@sha256:1234567890abcdef");

        AuthStore.ConfigFile configFile =
                AuthStore.ConfigFile.fromCredential(new AuthStore.Credential("admin", "password123"));

        // Load the configuration from the temporary file
        AuthStore.Config.load(List.of(configFile));

        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("library/foo", containerRef.getNamespace());
        assertEquals("hello-world", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());

        // Clean up by deleting the temporary file
        Files.delete(tempDir);
    }
}
