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

package land.oras;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import land.oras.exception.OrasException;
import land.oras.utils.SupportedAlgorithm;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

/**
 * Tests for {@link ContainerRef}.
 */
@Execution(ExecutionMode.CONCURRENT)
class ContainerRefTest {

    @TempDir
    private static Path homeDir1;

    @TempDir
    private static Path homeDir2;

    @TempDir
    private static Path homeDir3;

    @TempDir
    private static Path homeDir4;

    @BeforeAll
    static void init() throws Exception {

        // Write home registries.conf on the temp home directory
        Files.createDirectory(homeDir1.resolve(".config"));
        Files.createDirectory(homeDir1.resolve(".config").resolve("containers"));
        Files.createDirectory(homeDir2.resolve(".config"));
        Files.createDirectory(homeDir2.resolve(".config").resolve("containers"));
        Files.createDirectory(homeDir3.resolve(".config"));
        Files.createDirectory(homeDir3.resolve(".config").resolve("containers"));
        Files.createDirectory(homeDir4.resolve(".config"));
        Files.createDirectory(homeDir4.resolve(".config").resolve("containers"));
    }

    @Test
    void shouldReadRegistriesConfig() throws Exception {
        // language=toml
        String config =
                """
            [[registry]]
            location = "public.ecr.aws"
            blocked = true

            [[registry]]
            location = "docker.io/library/alpine"
            blocked = true

            [[registry]]
            location = "localhost:5000"
            insecure = true
            """;

        Files.writeString(homeDir1.resolve(".config").resolve("containers").resolve("registries.conf"), config);

        new EnvironmentVariables()
                .set("HOME", homeDir1.toAbsolutePath().toString())
                .execute(() -> {
                    Registry registry = Registry.builder().defaults().build();
                    assertEquals("https", registry.getScheme());
                    assertTrue(ContainerRef.parse("public.ecr.aws/whatever").isBlocked(registry));
                    assertFalse(ContainerRef.parse("docker.io/library/whatever").isInsecure(registry));
                    assertTrue(ContainerRef.parse("docker.io/library/whatever").isInsecure(registry.asInsecure()));
                    assertTrue(ContainerRef.parse("docker.io/library/alpine:latest")
                            .isBlocked(registry));
                    assertFalse(ContainerRef.parse("docker.io/library/alpine:latest")
                            .isInsecure(registry));
                    assertFalse(
                            ContainerRef.parse("docker.io/library/test:latest").isBlocked(registry));
                    assertTrue(ContainerRef.parse("localhost:5000/library/test").isInsecure(registry));
                    assertThrows(
                            OrasException.class,
                            () -> registry.getIndex(ContainerRef.parse("public.ecr.aws/docker/library/alpine:latest")));
                    assertThrows(
                            OrasException.class,
                            () -> registry.getIndex(ContainerRef.parse("docker.io/library/alpine:latest")));
                    assertDoesNotThrow(() -> registry.getIndex(ContainerRef.parse("docker.io/library/mongo:latest")));
                });
    }

    @Test
    void shouldDetermineFromAlias() throws Exception {

        // language=toml
        String config =
                """
            [aliases]
            "my-library/my-namespace"="localhost/test"
            "my-library"="localhost/test2"
            """;

        Files.writeString(homeDir2.resolve(".config").resolve("containers").resolve("registries.conf"), config);

        new EnvironmentVariables()
                .set("HOME", homeDir2.toAbsolutePath().toString())
                .execute(() -> {
                    Registry registry = Registry.builder().defaults().build();
                    ContainerRef unqualifiedRef = ContainerRef.parse("my-library/my-namespace");
                    assertEquals("localhost/test", unqualifiedRef.getEffectiveRegistry(registry));
                    ContainerRef unqualifiedRef2 = ContainerRef.parse("my-library");
                    assertEquals("localhost/test2", unqualifiedRef2.getEffectiveRegistry(registry));
                });
    }

    @Test
    void shouldRewriteAllSubdomainToLocalProxy() throws Exception {

        // language=toml
        String config =
                """
                [[registry]]
                prefix = "*.example.com"
                location = "localhost:5000/example-com"

                [[registry]]
                prefix = "*.otherexample.io/library"
                location = "localhost:5001/docker"
                """;

        Files.writeString(homeDir3.resolve(".config").resolve("containers").resolve("registries.conf"), config);

        new EnvironmentVariables()
                .set("HOME", homeDir3.toAbsolutePath().toString())
                .execute(() -> {
                    Registry registry = Registry.builder().defaults().build();

                    // One subdomain
                    ContainerRef containerRef = ContainerRef.parse("toto.example.com/library/alpine:latest");
                    assertEquals("localhost:5000", containerRef.getEffectiveRegistry(registry));
                    ContainerRef newRef = registry.getRegistriesConf().rewrite(containerRef);
                    assertEquals("localhost:5000/example-com/library/alpine:latest", newRef.toString());

                    // Several subdomain
                    containerRef = ContainerRef.parse("test.foobar.example.com/library/alpine:latest");
                    assertEquals("localhost:5000", containerRef.getEffectiveRegistry(registry));
                    newRef = registry.getRegistriesConf().rewrite(containerRef);
                    assertEquals("localhost:5000/example-com/library/alpine:latest", newRef.toString());

                    // With path
                    containerRef = ContainerRef.parse("test.otherexample.io/library/alpine:latest");
                    assertEquals("localhost:5001", containerRef.getEffectiveRegistry(registry));
                    newRef = registry.getRegistriesConf().rewrite(containerRef);
                    assertEquals("localhost:5001/docker/alpine:latest", newRef.toString());

                    // No rewrite if library does not match
                    containerRef = ContainerRef.parse("test.otherexample.io/foobar/alpine:latest");
                    assertEquals("test.otherexample.io", containerRef.getEffectiveRegistry(registry));
                    newRef = registry.getRegistriesConf().rewrite(containerRef);
                    assertEquals("test.otherexample.io/foobar/alpine:latest", newRef.toString());
                });
    }

    @Test
    void shouldDetermineEffectiveRegistry() throws Exception {

        // Use from container ref
        Registry registry = Registry.builder().defaults().build();
        ContainerRef containerRef = ContainerRef.parse("docker.io/library/foo/alpine:latest@sha256:1234567890abcdef");
        assertEquals("docker.io", containerRef.getEffectiveRegistry(registry));

        // Took from registry
        assertEquals("foo.io", containerRef.forRegistry("foo.io").getEffectiveRegistry(registry));

        // Unqualified with registry
        Registry registryWithRegistry =
                Registry.builder().defaults().withRegistry("foo.io").build();
        ContainerRef unqualifiedWithRegistryRef = ContainerRef.parse("library/foo/alpine:latest");
        assertEquals("foo.io", unqualifiedWithRegistryRef.getEffectiveRegistry(registryWithRegistry));

        // language=toml
        // Ensure empty config does not cause error with machine contains default registry
        String config = "";

        Files.writeString(homeDir4.resolve(".config").resolve("containers").resolve("registries.conf"), config);

        new EnvironmentVariables()
                .set("HOME", homeDir4.toAbsolutePath().toString())
                .execute(() -> {
                    Registry r = Registry.builder().defaults().build();

                    // Unqualified without config use docker.io
                    ContainerRef unqualifiedRef = ContainerRef.parse("alpine:latest");
                    assertTrue(unqualifiedRef.isUnqualified(), "ContainerRef must be unqualified");
                    assertEquals("docker.io", unqualifiedRef.getEffectiveRegistry(r));
                });
    }

    @Test
    void shouldReturnWithDigest() {

        // Explicit
        ContainerRef containerRef1 = ContainerRef.parse("docker.io/library/foo");
        ContainerRef withDigest1 = containerRef1.withDigest("sha256@12344");
        assertFalse(containerRef1.isUnqualified(), "ContainerRef must be qualified");
        assertEquals("sha256@12344", withDigest1.getDigest());
        assertEquals("library", withDigest1.getNamespace());
        assertEquals(
                "library/foo",
                containerRef1.getFullRepository(Registry.builder().build()));
        assertEquals(
                "library",
                withDigest1.getNamespace(
                        Registry.builder().withRegistry("foo.io").build()));

        // Implicit
        ContainerRef containerRef2 = ContainerRef.parse("foo");
        ContainerRef withDigest2 = containerRef2.withDigest("sha256@12344");
        assertEquals("sha256@12344", withDigest2.getDigest());
        assertEquals("library", withDigest2.getNamespace());
        assertEquals(
                null,
                withDigest2.getNamespace(
                        Registry.builder().withRegistry("foo.io").build()));
    }

    @Test
    void shouldParseImageWithAllParts() {
        ContainerRef containerRef = ContainerRef.parse("docker.io/library/foo/alpine:latest@sha256:1234567890abcdef");
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("registry-1.docker.io", containerRef.getApiRegistry());
        assertEquals("library/foo", containerRef.getNamespace());
        assertEquals("library/foo", containerRef.getNamespace(Registry.builder().build()));
        assertEquals("alpine", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertEquals(SupportedAlgorithm.SHA256, containerRef.getAlgorithm());
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());
    }

    @Test
    void shouldFailWithUnSupportedAlgorithm() {
        assertThrows(
                OrasException.class,
                () -> ContainerRef.parse("docker.io/library/foo/alpine:latest@test:1234567890abcdef"),
                "Unsupported algorithm: test");
        assertThrows(
                OrasException.class,
                () -> ContainerRef.parse("docker.io/library/foo/alpine:latest@sha256:sha256:1234567890abcdef"),
                "Unsupported algorithm: test");
    }

    @Test
    void shouldParseImageWithNoNamespace() {
        ContainerRef containerRef = ContainerRef.parse("docker.io/alpine:latest@sha256:1234567890abcdef");
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("library", containerRef.getNamespace());
        assertEquals(
                "library/alpine",
                containerRef.getFullRepository(Registry.builder().build()));
        assertEquals("library", containerRef.getNamespace(Registry.builder().build()));
        assertEquals("alpine", containerRef.getRepository());
        assertEquals(
                "registry-1.docker.io/v2/library/alpine/manifests/sha256:1234567890abcdef",
                containerRef.getManifestsPath());
        assertEquals("latest", containerRef.getTag());
        assertEquals(
                "registry-1.docker.io/v2/library/alpine/referrers/sha256:1234567890abcdef?artifactType=test%2Fbar",
                containerRef.getReferrersPath(ArtifactType.from("test/bar")));
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());
        assertEquals(
                "registry-1.docker.io/v2/library/alpine/manifests/sha256:1234567890abcdef",
                containerRef.getManifestsPath());
        containerRef = ContainerRef.parse("demo.goharbor.com/alpine:latest@sha256:1234567890abcdef");
        assertEquals("demo.goharbor.com", containerRef.getRegistry());
        assertEquals("demo.goharbor.com/v2/alpine/tags/list", containerRef.getTagsPath());
        assertNull(containerRef.getNamespace());
        assertEquals("alpine", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());
    }

    @Test
    void shouldParseImageWithNoTag() {
        ContainerRef containerRef = ContainerRef.parse("docker.io/alpine@sha256:1234567890abcdef");
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("registry-1.docker.io/v2/library/alpine/tags/list", containerRef.getTagsPath());
        assertEquals("library", containerRef.getNamespace());
        assertEquals("library", containerRef.getNamespace(Registry.builder().build()));
        assertEquals("alpine", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertEquals(
                "registry-1.docker.io/v2/library/alpine/blobs/sha256:1234567890abcdef",
                containerRef.getBlobsPath(null));
        assertEquals(
                "foo.io/v2/alpine/blobs/sha256:1234567890abcdef",
                containerRef.getBlobsPath(
                        Registry.builder().withRegistry("foo.io").build()));
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());

        containerRef = ContainerRef.parse("docker.io/foobar/alpine@sha256:1234567890abcdef");
        assertEquals("registry-1.docker.io/v2/foobar/alpine/tags/list", containerRef.getTagsPath());
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("foobar", containerRef.getNamespace());
        assertEquals("alpine", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertEquals(
                "registry-1.docker.io/v2/foobar/alpine/blobs/sha256:1234567890abcdef", containerRef.getBlobsPath(null));
        assertEquals(
                "foo.io/v2/foobar/alpine/blobs/sha256:1234567890abcdef",
                containerRef.getBlobsPath(
                        Registry.builder().withRegistry("foo.io").build()));
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());
    }

    @Test
    void shouldParseImageWithNoDigest() {
        ContainerRef containerRef = ContainerRef.parse("docker.io/alpine:latest");
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("library", containerRef.getNamespace());
        assertEquals("alpine", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertNull(containerRef.getDigest());
    }

    @Test
    void shouldParseImageWithNoRegistry() {
        ContainerRef containerRef = ContainerRef.parse("alpine:latest");
        assertTrue(containerRef.isUnqualified(), "ContainerRef must be unqualified");
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("registry-1.docker.io", containerRef.getApiRegistry());
        assertEquals(
                "foo.io",
                containerRef.getApiRegistry(
                        Registry.builder().withRegistry("foo.io").build()));
        assertEquals("registry-1.docker.io/v2/library/alpine/tags/list", containerRef.getTagsPath());
        assertEquals("registry-1.docker.io/v2/_catalog", containerRef.getRepositoriesPath());
        assertEquals("library", containerRef.getNamespace());
        assertEquals("alpine", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertNull(containerRef.getDigest());
        assertFalse(containerRef.forRegistry("docker.io").isUnqualified(), "ContainerRef must be qualified");
    }

    @Test
    void shouldParseImageWithNoTagAndNoRegistry() {
        ContainerRef containerRef = ContainerRef.parse("alpine");
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("library", containerRef.getNamespace());
        assertEquals(
                "library/alpine",
                containerRef.getFullRepository(Registry.builder().build()));
        assertEquals("library", containerRef.getNamespace(Registry.builder().build()));
        assertEquals(
                "library",
                containerRef.getNamespace(
                        Registry.builder().withRegistry("docker.io").build()));
        assertNull(
                containerRef.getNamespace(
                        Registry.builder().withRegistry("test").build()),
                "Default library must be null when changing registry");
        assertEquals("alpine", containerRef.getRepository());
        assertEquals("library", containerRef.getNamespace());
        assertEquals("latest", containerRef.getTag());
        assertNull(containerRef.getDigest());

        containerRef = ContainerRef.parse("foobar/alpine");
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("foobar", containerRef.getNamespace());
        assertEquals("foobar", containerRef.getNamespace(Registry.builder().build()));
        assertEquals(
                "foobar",
                containerRef.getNamespace(
                        Registry.builder().withRegistry("docker.io").build()));
        assertEquals(
                "foobar",
                containerRef.getNamespace(
                        Registry.builder().withRegistry("test").build()));
        assertEquals("alpine", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertNull(containerRef.getDigest());
    }

    @Test
    void shouldGetTagsPathDockerIo() {
        ContainerRef containerRef = ContainerRef.parse("docker.io/library/foo/alpine:latest@sha256:1234567890abcdef");
        assertEquals("registry-1.docker.io/v2/library/foo/alpine/tags/list", containerRef.getTagsPath());
    }

    @Test
    void shouldBuildNewRefForRegistry() {
        Registry registry =
                Registry.builder().defaults().withRegistry("my-registry.io").build();
        ContainerRef containerRef = ContainerRef.parse("library/foo/alpine:latest@sha256:1234567890abcdef")
                .forRegistry(registry);
        assertEquals("my-registry.io/v2/library/foo/alpine/tags/list", containerRef.getTagsPath());
        containerRef = ContainerRef.parse("library/foo/alpine:latest@sha256:1234567890abcdef")
                .forRegistry("my-registry.io");
        assertEquals("my-registry.io/v2/library/foo/alpine/tags/list", containerRef.getTagsPath());
    }

    @Test
    void testEqualsAndHashCode() {
        ContainerRef containerRef1 = ContainerRef.parse("docker.io/library/foo/alpine:latest@sha256:1234567890abcdef");
        ContainerRef containerRef2 = ContainerRef.parse("docker.io/library/foo/alpine:latest@sha256:1234567890abcdef");
        assertEquals(containerRef1, containerRef2);
        assertEquals(containerRef1.hashCode(), containerRef2.hashCode());

        // Not equals
        assertNotEquals("foo", containerRef1);
        assertNotEquals(null, containerRef1);
    }

    @Test
    void testEqualsAndHashCodeWithoutNamespace() {
        ContainerRef containerRef1 = ContainerRef.parse("my-registry.com/alpine:latest");
        ContainerRef containerRef2 = ContainerRef.parse("my-registry.com/alpine:latest");
        assertEquals(containerRef1, containerRef2);
        assertEquals(containerRef1.hashCode(), containerRef2.hashCode());
    }

    @Test
    void testNotEqualsWithOtherTag() {
        ContainerRef containerRef1 = ContainerRef.parse("my-registry.com/alpine:latest");
        ContainerRef containerRef2 = ContainerRef.parse("my-registry.com/alpine:latest2");
        assertNotEquals(containerRef1, containerRef2);
        assertNotEquals(containerRef1.hashCode(), containerRef2.hashCode());
    }

    @Test
    void testNotEqualsWithOtherDigest() {
        ContainerRef containerRef1 = ContainerRef.parse("my-registry.com/alpine@sha256:1234567890abcdef");
        ContainerRef containerRef2 = ContainerRef.parse("my-registry.com/alpine@sha256:5543535353535353");
        assertNotEquals(containerRef1, containerRef2);
        assertNotEquals(containerRef1.hashCode(), containerRef2.hashCode());
    }

    @Test
    void testNotEqualsWithOtherRegistry() {
        ContainerRef containerRef1 = ContainerRef.parse("my-registry1.com/alpine:latest");
        ContainerRef containerRef2 = ContainerRef.parse("my-registry2.com/alpine:latest");
        assertNotEquals(containerRef1, containerRef2);
        assertNotEquals(containerRef1.hashCode(), containerRef2.hashCode());
    }

    @Test
    void testNotEqualsWithOtherRepository() {
        ContainerRef containerRef1 = ContainerRef.parse("my-registry/alpine1:latest");
        ContainerRef containerRef2 = ContainerRef.parse("my-registry/alpine2:latest");
        assertNotEquals(containerRef1, containerRef2);
        assertNotEquals(containerRef1.hashCode(), containerRef2.hashCode());
    }

    @Test
    void testToString() {
        ContainerRef containerRef1 = ContainerRef.parse("my-registry.com/alpine1:latest");
        assertEquals("my-registry.com/alpine1:latest", containerRef1.toString());
    }

    @Test
    void testToStringDefault() {
        ContainerRef containerRef1 = ContainerRef.parse("alpine1@sha256:1234567890abcdef");
        assertEquals("alpine1:latest@sha256:1234567890abcdef", containerRef1.toString());
    }

    @Test
    void testToStringWithNamespaceAndNoRegistry() {
        ContainerRef containerRef1 = ContainerRef.parse("library/alpine");
        assertEquals("library/alpine:latest", containerRef1.toString());
    }

    @Test
    void testToStringWithNamespace() {
        ContainerRef containerRef1 = ContainerRef.parse("my-registry.com/foo/alpine1:latest");
        assertEquals("my-registry.com/foo/alpine1:latest", containerRef1.toString());
    }

    @Test
    void shouldHandleNoNamespace() {
        Registry registry =
                Registry.builder().defaults().withRegistry("demo.goharbor.io").build();
        ContainerRef containerRef = ContainerRef.parse("demo.goharbor.io/alpine:latest");
        assertEquals("demo.goharbor.io", containerRef.getRegistry());
        assertNull(containerRef.getNamespace(registry));
        assertNull(containerRef.getNamespace());
        assertEquals("alpine", containerRef.getRepository());
        assertEquals("alpine", containerRef.getFullRepository());
        assertEquals("alpine", containerRef.getFullRepository(registry));
    }

    @Test
    void shouldGetTagsPathOtherRegistry() {
        ContainerRef containerRef = ContainerRef.parse("demo.goharbor.io/foo/alpine:latest@sha256:1234567890abcdef");
        assertEquals("demo.goharbor.io/v2/foo/alpine/tags/list", containerRef.getTagsPath());
        assertEquals("demo.goharbor.io/v2/_catalog", containerRef.getRepositoriesPath());
        assertEquals("demo.goharbor.io/v2/foo/alpine/blobs/sha256:1234567890abcdef", containerRef.getBlobsPath(null));
        assertEquals(
                "foo/alpine",
                containerRef.getFullRepository(
                        Registry.builder().withRegistry("demo.goharbor.io").build()));
        assertEquals(
                "foo.io/v2/foo/alpine/blobs/sha256:1234567890abcdef",
                containerRef.getBlobsPath(
                        Registry.builder().withRegistry("foo.io").build()));
    }
}
