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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import land.oras.ContainerRef;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

class AuthStoreTest {

    @TempDir
    private Path tempDir;

    @TempDir
    private static Path homeDir;

    @TempDir
    private static Path xdgRuntimeDir;

    private AuthStore authStore;
    private AuthStore.Config mockConfig;
    private AuthStore.Credential mockCredential;
    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";

    // language=json
    public static final String SAMPLE_DOCKER_CONFIG =
            """
    {
        "auths": {
            "registry.example.com": {
                "auth": "dXNlcjpwYXNzd29yZA=="
            },
            "another.registry.com": {
                "auth": "dXNlcjpwYXNzd29yZA=="
            }
        },
        "credHelpers": {
            "registry.other.com": "foo-binary",
            "another.other.com": "bar-binary",
            "new.other.com": "pass",
            "other.other.com": "secretservice",
            "creds.other.com": "fake",
            "error.other.com": "fake"
        }
    }
    """;

    // language=json
    public static final String SAMPLE_PODMAN_CONFIG =
            """
    {
        "auths": {
            "registry.other.com": {
                "auth": "dXNlcjpwYXNzd29yZA=="
            },
            "another.other.com": {
                "auth": "dXNlcjpwYXNzd29yZA=="
            }
        },
        "credHelpers": {
            "registry.other.com": "foo-binary",
            "another.other.com": "bar-binary",
            "new.other.com": "pass",
            "other.other.com": "secretservice",
            "creds.other.com": "fake",
            "error.other.com": "fake"
        }
    }
    """;

    @BeforeAll
    static void init() throws Exception {

        // Write a sample Docker config file
        Files.createDirectory(homeDir.resolve(".docker"));
        Files.writeString(homeDir.resolve(".docker").resolve("config.json"), SAMPLE_DOCKER_CONFIG);

        // Write a sample Podman config file
        Files.createDirectory(xdgRuntimeDir.resolve("containers"));
        Files.writeString(xdgRuntimeDir.resolve("containers").resolve("auth.json"), SAMPLE_PODMAN_CONFIG);

        Path helper = Path.of("docker-credential-fake");
        String newPath =
                helper.toAbsolutePath().getParent() + System.getProperty("path.separator") + System.getenv("PATH");
        System.setProperty("PATH", newPath);
    }

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
    void testShouldReadCredentialsFromCredentialHelperNullCheck() throws Exception {
        new EnvironmentVariables().set("XDG_RUNTIME_DIR", "not-used").execute(() -> {
            new SystemProperties("user.home", homeDir.toAbsolutePath().toString()).execute(() -> {
                assertNotNull(System.getenv("XDG_RUNTIME_DIR"));
                AuthStore authStoreInstance = AuthStore.newStore();
                assertNotNull(authStoreInstance);

                // Verify
                AuthStore.Credential credential =
                        authStoreInstance.get(ContainerRef.parse("other.other.com/foo/bar:latest"));
                assertNull(credential);
            });
        });
    }

    @Test
    void testShouldReadCredentialsFromCredentialHelperFake() throws Exception {

        assumeTrue(
                Files.exists(Path.of("/usr/bin/docker-credential-fake")),
                "Skipping test: /usr/bin/docker-credential-fake not found");

        // Prepend to PATH
        Path helper = Path.of("docker-credential-fake");
        String newPath =
                helper.toAbsolutePath().getParent() + System.getProperty("path.separator") + System.getenv("PATH");
        new EnvironmentVariables()
                .set("XDG_RUNTIME_DIR", "not-used")
                .set("PATH", newPath)
                .execute(() -> {
                    new SystemProperties("user.home", homeDir.toAbsolutePath().toString()).execute(() -> {
                        assertNotNull(System.getenv("XDG_RUNTIME_DIR"));
                        AuthStore authStoreInstance = AuthStore.newStore();
                        assertNotNull(authStoreInstance);

                        // Verify
                        AuthStore.Credential credential =
                                authStoreInstance.get(ContainerRef.parse("creds.other.com/foo/bar:latest"));
                        assertNotNull(credential);
                    });
                });
    }

    @Test
    void testShouldReadCredentialsFromCredentialHelperHandleNonZeroReturnCode() throws Exception {

        assumeTrue(
                Files.exists(Path.of("/usr/bin/docker-credential-fake")),
                "Skipping test: /usr/bin/docker-credential-fake not found");

        // Prepend to PATH
        Path helper = Path.of("docker-credential-fake");
        String newPath =
                helper.toAbsolutePath().getParent() + System.getProperty("path.separator") + System.getenv("PATH");
        new EnvironmentVariables()
                .set("XDG_RUNTIME_DIR", "not-used")
                .set("PATH", newPath)
                .execute(() -> {
                    new SystemProperties("user.home", homeDir.toAbsolutePath().toString()).execute(() -> {
                        assertNotNull(System.getenv("XDG_RUNTIME_DIR"));
                        AuthStore authStoreInstance = AuthStore.newStore();
                        assertNotNull(authStoreInstance);

                        // Verify
                        AuthStore.Credential credential =
                                authStoreInstance.get(ContainerRef.parse("error.other.com/foo/bar:latest"));
                        assertNull(credential);
                    });
                });
    }

    @Test
    void testShouldReadCredentialsFromDockerConfig() throws Exception {
        new EnvironmentVariables().set("XDG_RUNTIME_DIR", "not-used").execute(() -> {
            new SystemProperties("user.home", homeDir.toAbsolutePath().toString()).execute(() -> {
                assertNotNull(System.getenv("XDG_RUNTIME_DIR"));
                AuthStore authStoreInstance = AuthStore.newStore();
                assertNotNull(authStoreInstance);

                // Verify
                AuthStore.Credential credential =
                        authStoreInstance.get(ContainerRef.parse("registry.example.com/foo/bar:latest"));
                assertNotNull(credential);
                assertEquals(USERNAME, credential.username());

                // Null
                assertNull(authStoreInstance.get(ContainerRef.parse("unknown.registry.com/foo/bar:latest")));

                String binary = authStoreInstance.getCredentialHelperBinary(
                        ContainerRef.parse("registry.other.com/foo/bar:latest"));
                assertNotNull(binary);
                assertEquals("docker-credential-foo-binary", binary);

                assertNull(authStoreInstance.getCredentialHelperBinary(
                        ContainerRef.parse("unknown.registry.com/foo/bar:latest")));
            });
        });
    }

    @Test
    void testShouldReadCredentialsFromPodManConfig() throws Exception {
        new EnvironmentVariables()
                .set("XDG_RUNTIME_DIR", xdgRuntimeDir.toAbsolutePath().toString())
                .execute(() -> {
                    new SystemProperties("user.home", "not-used").execute(() -> {
                        assertNotNull(System.getenv("XDG_RUNTIME_DIR"));
                        assertEquals(xdgRuntimeDir.toAbsolutePath().toString(), System.getenv("XDG_RUNTIME_DIR"));
                        AuthStore authStoreInstance = AuthStore.newStore();
                        assertNotNull(authStoreInstance);

                        // Verify
                        AuthStore.Credential credential =
                                authStoreInstance.get(ContainerRef.parse("registry.other.com/foo/bar:latest"));
                        assertNotNull(credential);
                        assertEquals(USERNAME, credential.username());

                        String binary = authStoreInstance.getCredentialHelperBinary(
                                ContainerRef.parse("registry.other.com/foo/bar:latest"));
                        assertNotNull(binary);
                        assertEquals("docker-credential-foo-binary", binary);

                        assertNull(authStoreInstance.getCredentialHelperBinary(
                                ContainerRef.parse("unknown.registry.com/foo/bar:latest")));
                    });
                });
    }

    @Test
    void testWithoutXdgRuntimeDir() throws Exception {
        new EnvironmentVariables().remove("XDG_RUNTIME_DIR").execute(() -> {
            assertNull(System.getenv("XDG_RUNTIME_DIR"));
            AuthStore authStoreInstance = AuthStore.newStore();
            assertNotNull(authStoreInstance);
        });
    }

    @Test
    void testGetCredential_success() throws Exception {

        ContainerRef ref = ContainerRef.parse("localhost:5000/myrepo/myimage:latest");

        // Mock the behavior of getting credentials
        Mockito.when(mockConfig.getCredential(ref)).thenReturn(mockCredential);

        AuthStore.Credential credential = authStore.get(ref);

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
