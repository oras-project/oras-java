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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import land.oras.utils.RegistryContainer;
import land.oras.utils.SupportedAlgorithm;
import land.oras.utils.SupportedCompression;
import land.oras.utils.ZotContainer;
import land.oras.utils.ZotUnsecureContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Execution(ExecutionMode.CONCURRENT)
class RegistryTest {

    @Container
    private final ZotContainer registry = new ZotContainer().withStartupAttempts(3);

    @Container
    private final ZotUnsecureContainer unsecureRegistry = new ZotUnsecureContainer().withStartupAttempts(3);

    @TempDir
    private Path blobDir;

    @TempDir
    private Path artifactDir;

    @TempDir
    private Path extractDir;

    @TempDir
    private Path extractDirZip;

    @BeforeEach
    void before() {
        registry.withFollowOutput();
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void shouldThrowIfUnableToFindOnAnyUnQualifiedSearchRegistry(@TempDir Path homeDir) throws Exception {

        // language=toml
        String config = """
                unqualified-search-registries = ["%s"]
                """
                .formatted(registry.getRegistry());

        TestUtils.createRegistriesConfFile(homeDir, config);

        TestUtils.withHome(homeDir, () -> {
            Registry registry = Registry.builder().insecure().defaults().build();
            ContainerRef unqualifiedRef = ContainerRef.parse("docker/library/alpine:latest");
            assertTrue(unqualifiedRef.isUnqualified(), "ContainerRef must be unqualified");
            OrasException e = assertThrows(OrasException.class, () -> unqualifiedRef.getEffectiveRegistry(registry));
            assertEquals("Invalid WWW-Authenticate header", e.getMessage());
        });
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void shouldEnforceMultipleRegistriesWithDefaultEnforcingMode(@TempDir Path homeDir) throws Exception {

        // language=toml
        String config = """
                unqualified-search-registries = ["%s", "localhost:5000"]
                """
                .formatted(registry.getRegistry());

        TestUtils.createRegistriesConfFile(homeDir, config);

        TestUtils.withHome(homeDir, () -> {
            Registry registry = Registry.builder().insecure().defaults().build();
            ContainerRef unqualifiedRef = ContainerRef.parse("docker/library/alpine:latest");
            assertTrue(unqualifiedRef.isUnqualified(), "ContainerRef must be unqualified");
            OrasException e = assertThrows(OrasException.class, () -> unqualifiedRef.getEffectiveRegistry(registry));
            assertEquals(
                    "Short name mode is set to ENFORCING/PERMISSION but multiple unqualified registries are configured: [%s, localhost:5000]"
                            .formatted(this.registry.getRegistry()),
                    e.getMessage());
        });
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void shouldAllowMultipleRegistriesWithDisabledEnforcingMode(@TempDir Path homeDir) throws Exception {

        // language=toml
        String config =
                """
                short-name-mode = "disabled"
                unqualified-search-registries = ["%s", "localhost:5000"]
                """
                        .formatted(registry.getRegistry());

        TestUtils.createRegistriesConfFile(homeDir, config);

        TestUtils.withHome(homeDir, () -> {
            Registry registry = Registry.builder().insecure().defaults().build();
            ContainerRef unqualifiedRef = ContainerRef.parse("docker/library/alpine:latest");
            assertTrue(unqualifiedRef.isUnqualified(), "ContainerRef must be unqualified");
            OrasException e = assertThrows(OrasException.class, () -> unqualifiedRef.getEffectiveRegistry(registry));
            assertEquals("Invalid WWW-Authenticate header", e.getMessage());
        });
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void shouldEnforceMultipleRegistriesWithPermissiveEnforcingMode(@TempDir Path homeDir) throws Exception {

        // language=toml
        String config =
                """
                short-name-mode = "permissive"
                unqualified-search-registries = ["%s", "localhost:5000"]
                """
                        .formatted(registry.getRegistry());

        TestUtils.createRegistriesConfFile(homeDir, config);

        TestUtils.withHome(homeDir, () -> {
            Registry registry = Registry.builder().insecure().defaults().build();
            ContainerRef unqualifiedRef = ContainerRef.parse("docker/library/alpine:latest");
            assertTrue(unqualifiedRef.isUnqualified(), "ContainerRef must be unqualified");
            OrasException e = assertThrows(OrasException.class, () -> unqualifiedRef.getEffectiveRegistry(registry));
            assertEquals(
                    "Short name mode is set to ENFORCING/PERMISSION but multiple unqualified registries are configured: [%s, localhost:5000]"
                            .formatted(this.registry.getRegistry()),
                    e.getMessage());
        });
    }

    @Test
    void shouldListRepositories() {

        // Setup
        Registry registry = Registry.builder()
                .insecure(this.registry.getRegistry(), "myuser", "mypass")
                .build();

        // Test
        List<String> repositories = registry.getRepositories().repositories();
        assertNotNull(repositories);
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void shouldListRepositoriesInsecure(@TempDir Path homeDir) throws Exception {

        // language=toml
        String config =
                """
            [[registry]]
            location = "%s"
            insecure = true
            """
                        .formatted(this.unsecureRegistry.getRegistry());
        TestUtils.createRegistriesConfFile(homeDir, config);

        TestUtils.withHome(homeDir, () -> {
            Registry registry = Registry.builder()
                    .defaults(this.unsecureRegistry.getRegistry())
                    .build();
            List<String> repositories = registry.getRepositories().repositories();
            assertNotNull(repositories);
        });
    }

    @Test
    void shouldFailToPushBlobForInvalidDigest() {
        Registry registry = Registry.builder()
                .insecure(this.registry.getRegistry(), "myuser", "mypass")
                .build();
        ContainerRef containerRef1 = ContainerRef.parse(
                "library/artifact-text@sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
        assertThrows(OrasException.class, () -> {
            registry.pushBlob(containerRef1, "invalid".getBytes());
        });
    }

    @Test
    void shouldFailToPushBlobWithMissingDigestViaStream() {
        Registry registry = Registry.builder()
                .insecure(this.registry.getRegistry(), "myuser", "mypass")
                .build();
        ContainerRef containerRef = ContainerRef.parse("library/artifact-text:latest");
        OrasException e = assertThrows(OrasException.class, () -> {
            registry.pushBlob(
                    containerRef,
                    10L,
                    () -> new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8)),
                    Map.of());
        });
        assertEquals("Digest is required to push blob with stream", e.getMessage());
    }

    @Test
    void shouldPushBlobWithDigestViaStream() {
        Registry registry = Registry.builder()
                .insecure(this.registry.getRegistry(), "myuser", "mypass")
                .build();
        byte[] content = "foo".getBytes(StandardCharsets.UTF_8);
        String digest = SupportedAlgorithm.SHA512.digest(content);
        long size = content.length;
        InputStream stream = new ByteArrayInputStream(content);
        ContainerRef containerRef =
                ContainerRef.parse("library/artifact-blob-stream").withDigest(digest);
        registry.pushBlob(containerRef, size, () -> stream, Map.of());
        registry.pushBlob(containerRef, size, () -> stream, Map.of());
    }

    @Test
    void shouldPushAndGetBlobThenDeleteWithSha256() {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withRegistry(this.registry.getRegistry())
                .withInsecure(true)
                .build();
        ContainerRef containerRef = ContainerRef.parse(
                "library/artifact-text@sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
        Layer layer = registry.pushBlob(containerRef, "hello".getBytes());
        assertEquals("sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", layer.getDigest());
        byte[] blob = registry.getBlob(
                containerRef.withDigest("sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"));
        assertEquals("hello", new String(blob));
        registry.pushBlob(containerRef, "hello".getBytes());
        registry.deleteBlob(
                containerRef.withDigest("sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"));

        // Ensure the blob is deleted
        assertThrows(OrasException.class, () -> {
            registry.getBlob(
                    containerRef.withDigest("sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"));
        });
    }

    @Test
    void shouldPushAndGetBlobThenDeleteWithSha512() {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef = ContainerRef.parse(
                "%s/library/artifact-text@sha512:9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca72323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043"
                        .formatted(this.registry.getRegistry()));
        Layer layer = registry.pushBlob(containerRef, "hello".getBytes());
        assertEquals(
                "sha512:9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca72323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043",
                layer.getDigest());
        byte[] blob = registry.getBlob(
                containerRef.withDigest(
                        "sha512:9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca72323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043"));
        assertEquals("hello", new String(blob));
        registry.pushBlob(containerRef, "hello".getBytes());
        registry.deleteBlob(
                containerRef.withDigest(
                        "sha512:9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca72323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043"));

        // Ensure the blob is deleted
        assertThrows(OrasException.class, () -> {
            registry.getBlob(
                    containerRef.withDigest(
                            "sha512:9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca72323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043"));
        });
    }

    @Test
    void shouldFailWithoutAuthentication() {
        Registry registry = Registry.Builder.builder().insecure().build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-text".formatted(this.registry.getRegistry()));
        assertThrows(OrasException.class, () -> {
            registry.pushBlob(containerRef, "hello".getBytes());
        });
    }

    @Test
    void shouldPushUnsecure() {
        Registry registry = Registry.Builder.builder().insecure().build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-text".formatted(this.unsecureRegistry.getRegistry()));
        registry.pushBlob(containerRef, "hello".getBytes());
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void shouldPushPullManifestsAndBlobsByUsingConfig(@TempDir Path homeDir) throws Exception {

        // language=toml
        String config =
                """
            [[registry]]
            location = "%s"
            insecure = true
            """
                        .formatted(this.unsecureRegistry.getRegistry());
        TestUtils.createRegistriesConfFile(homeDir, config);

        TestUtils.withHome(homeDir, () -> {
            Registry registry = Registry.Builder.builder().build(); // Use default
            ContainerRef containerRef = ContainerRef.parse(
                    "%s/library/artifact-text-manifest-blobs".formatted(this.unsecureRegistry.getRegistry()));

            registry.pushBlob(containerRef, "hello".getBytes());
            registry.pushBlob(containerRef, "other-hello".getBytes());

            String digest = SupportedAlgorithm.SHA256.digest("hello".getBytes());
            String otherDigest = SupportedAlgorithm.SHA256.digest("other-hello".getBytes());

            // Ensure we can fetch
            try (InputStream is = registry.fetchBlob(containerRef.withDigest(digest))) {
                assertEquals("hello", new String(is.readAllBytes()));
                Files.writeString(blobDir.resolve("hello.txt"), "hello");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            registry.pushBlob(containerRef, blobDir.resolve("hello.txt"));

            // Push a manifest with those blobs
            registry.fetchBlob(containerRef.withDigest(digest), extractDir.resolve("hello.txt"));
            registry.getBlob(containerRef.withDigest(digest));

            registry.pushArtifact(containerRef, LocalPath.of(extractDir.resolve("hello.txt")));
            registry.pullArtifact(containerRef, extractDir, true);

            // Checks
            registry.exists(containerRef.withDigest(digest));

            // Delete
            registry.deleteManifest(containerRef);
            registry.deleteBlob(containerRef.withDigest(otherDigest));
        });
    }

    @Test
    void shouldFailWithoutAuthenticationAndRegistry() {
        Registry registry =
                Registry.Builder.builder().insecure(this.registry.getRegistry()).build();
        ContainerRef containerRef = ContainerRef.parse("library/artifact-text");
        assertThrows(OrasException.class, () -> {
            registry.pushBlob(containerRef, "hello".getBytes());
        });
    }

    @Test
    void shouldFailWithSecureOnInsecure() {
        Registry registry =
                Registry.Builder.builder().defaults(this.registry.getRegistry()).build();
        ContainerRef containerRef = ContainerRef.parse("library/artifact-text");
        assertThrows(OrasException.class, () -> {
            registry.pushBlob(containerRef, "hello".getBytes());
        });
    }

    @Test
    void shouldFailWithoutExistingAuthentication() {
        Registry registry =
                Registry.Builder.builder().defaults().withInsecure(true).build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-text".formatted(this.registry.getRegistry()));
        assertThrows(
                OrasException.class,
                () -> {
                    registry.pushBlob(containerRef, "hello".getBytes());
                },
                "Response code: 401");
    }

    @Test
    void shouldUploadAndFetchBlobThenDelete() throws IOException {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-text".formatted(this.registry.getRegistry()));
        Files.createFile(blobDir.resolve("temp.txt"));
        Files.writeString(blobDir.resolve("temp.txt"), "hello");
        Layer layer = registry.pushBlob(containerRef, blobDir.resolve("temp.txt"));
        assertEquals("sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", layer.getDigest());

        registry.fetchBlob(
                containerRef.withDigest("sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"),
                blobDir.resolve("temp.txt"));

        Descriptor descriptor = registry.fetchBlobDescriptor(
                containerRef.withDigest("sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"));
        assertEquals(Const.DEFAULT_DESCRIPTOR_MEDIA_TYPE, descriptor.getMediaType());
        assertEquals(5, descriptor.getSize());
        assertEquals("sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", descriptor.getDigest());

        try (InputStream is = registry.fetchBlob(
                containerRef.withDigest("sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))) {
            assertEquals("hello", new String(is.readAllBytes()));
        }

        assertEquals("hello", Files.readString(blobDir.resolve("temp.txt")));
        registry.deleteBlob(
                containerRef.withDigest("sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"));

        // Ensure the blob is deleted
        assertThrows(OrasException.class, () -> {
            registry.getBlob(
                    containerRef.withDigest("sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"));
        });
    }

    @Test
    void shouldPushAndGetManifestThenDelete() {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();

        // Empty manifest
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/empty-manifest".formatted(this.registry.getRegistry()));
        Layer emptyLayer = registry.pushBlob(containerRef, Layer.empty().getDataBytes());
        Manifest emptyManifest = Manifest.empty().withLayers(List.of(Layer.fromDigest(emptyLayer.getDigest(), 2)));
        Manifest pushedManifest = registry.pushManifest(containerRef, emptyManifest);

        // Assert annotations
        assertEquals(1, pushedManifest.getAnnotations().size());
        assertNotNull(pushedManifest.getAnnotations().get(Const.ANNOTATION_CREATED), "Created annotation is missing");
        assertEquals(
                SupportedAlgorithm.SHA256,
                SupportedAlgorithm.fromDigest(pushedManifest.getDescriptor().getDigest()));
        Manifest manifest = registry.getManifest(containerRef);

        // Assert
        assertEquals(2, manifest.getSchemaVersion());
        assertEquals(Const.DEFAULT_MANIFEST_MEDIA_TYPE, manifest.getMediaType());
        assertEquals(Config.empty().getDigest(), manifest.getConfig().getDigest());
        assertEquals(1, manifest.getLayers().size()); // One empty layer
        Layer layer = manifest.getLayers().get(0);

        // An empty layer
        assertEquals(2, layer.getSize());
        assertEquals(Const.DEFAULT_EMPTY_MEDIA_TYPE, layer.getMediaType());

        assertEquals(Const.DEFAULT_EMPTY_MEDIA_TYPE, manifest.getArtifactType().getMediaType());

        // Push again
        registry.pushManifest(containerRef, manifest);

        // Delete manifest
        registry.deleteManifest(containerRef);
        // Ensure the blob is deleted
        assertThrows(OrasException.class, () -> {
            registry.getManifest(containerRef);
        });
    }

    @Test
    void shouldPushManifest() {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();

        // Empty manifest
        ContainerRef containerRef = ContainerRef.parse("%s/library/empty-index".formatted(this.registry.getRegistry()));
        Index emptyIndex = Index.fromManifests(List.of());
        Index pushIndex = registry.pushIndex(containerRef, emptyIndex);

        // Assert
        assertEquals(2, pushIndex.getSchemaVersion());
        assertEquals(Const.DEFAULT_INDEX_MEDIA_TYPE, pushIndex.getMediaType());
        assertEquals(0, pushIndex.getManifests().size());

        // Push again
        registry.pushIndex(containerRef, emptyIndex);

        // Delete index
        registry.deleteManifest(containerRef);
        // Ensure the blob is deleted
        assertThrows(OrasException.class, () -> {
            registry.getManifest(containerRef);
        });
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void shouldPushManifestWithRegistryConfig(@TempDir Path homeDir) throws Exception {

        // language=toml
        String config =
                """
            [[registry]]
            location = "%s"
            insecure = true
            """
                        .formatted(this.unsecureRegistry.getRegistry());
        TestUtils.createRegistriesConfFile(homeDir, config);

        TestUtils.withHome(homeDir, () -> {
            Registry registry = Registry.Builder.builder().defaults().build();

            // Empty manifest
            ContainerRef containerRef =
                    ContainerRef.parse("%s/library/empty-index".formatted(this.unsecureRegistry.getRegistry()));
            Index emptyIndex = Index.fromManifests(List.of());
            Index pushIndex = registry.pushIndex(containerRef, emptyIndex);

            // Assert
            assertEquals(2, pushIndex.getSchemaVersion());
            assertEquals(Const.DEFAULT_INDEX_MEDIA_TYPE, pushIndex.getMediaType());
            assertEquals(0, pushIndex.getManifests().size());

            // Push again
            registry.pushIndex(containerRef, emptyIndex);

            // Delete index
            registry.deleteManifest(containerRef);
            // Ensure the blob is deleted
            assertThrows(OrasException.class, () -> {
                registry.getManifest(containerRef);
            });
        });
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void shouldPushManifestWithAlias(@TempDir Path homeDir) throws Exception {

        // language=toml
        String config =
                """
            [[registry]]
            location = "%s"
            insecure = true

            [aliases]
            "my-library/my-namespace"="%s/test/bar"
            """
                        .formatted(this.unsecureRegistry.getRegistry(), this.unsecureRegistry.getRegistry());
        TestUtils.createRegistriesConfFile(homeDir, config);

        TestUtils.withHome(homeDir, () -> {
            Registry registry = Registry.Builder.builder().defaults().build();

            // Empty manifest
            ContainerRef containerRef = ContainerRef.parse("my-library/my-namespace");
            Index emptyIndex = Index.fromManifests(List.of());
            Index pushIndex = registry.pushIndex(containerRef, emptyIndex);

            // Assert
            assertEquals(2, pushIndex.getSchemaVersion());
            assertEquals(Const.DEFAULT_INDEX_MEDIA_TYPE, pushIndex.getMediaType());
            assertEquals(0, pushIndex.getManifests().size());

            // Push again
            registry.pushIndex(containerRef, emptyIndex);

            // Delete index
            registry.deleteManifest(containerRef);
            // Ensure the blob is deleted
            assertThrows(OrasException.class, () -> {
                registry.getManifest(containerRef);
            });
        });
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void shouldDetermineRegistryFromAlias(@TempDir Path homeDir) throws Exception {

        // language=toml
        String config = """
            [aliases]
            "my-library/my-namespace"="localhost/test"
            """
                .formatted(this.unsecureRegistry.getRegistry());
        TestUtils.createRegistriesConfFile(homeDir, config);

        TestUtils.withHome(homeDir, () -> {
            Registry registry = Registry.Builder.builder().defaults().build();
            ContainerRef ref = ContainerRef.parse("my-library/my-namespace:tag");
            assertEquals("localhost/test:tag", ref.forRegistry(registry).toString());
        });
    }

    @Test
    void shouldPushManifestWithRegistryUrl() {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withRegistry(this.registry.getRegistry())
                .withInsecure(true)
                .build();

        // Empty manifest
        ContainerRef containerRef = ContainerRef.parse("library/empty-index");
        Index emptyIndex = Index.fromManifests(List.of());
        Index pushIndex = registry.pushIndex(containerRef, emptyIndex);

        // Assert
        assertEquals(2, pushIndex.getSchemaVersion());
        assertEquals(Const.DEFAULT_INDEX_MEDIA_TYPE, pushIndex.getMediaType());
        assertEquals(0, pushIndex.getManifests().size());

        // Push again
        registry.pushIndex(containerRef, emptyIndex);

        // Delete index
        registry.deleteManifest(containerRef);
        // Ensure the blob is deleted
        assertThrows(OrasException.class, () -> {
            registry.getManifest(containerRef);
        });
    }

    @Test
    void pullArtifactWithoutLayer() {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef1 = ContainerRef.parse("%s/empty-layers".formatted(this.registry.getRegistry()));
        Config emptyConfig = Config.empty();
        Manifest manifest1 = Manifest.empty().withConfig(emptyConfig);
        registry.pushConfig(containerRef1, emptyConfig);
        registry.pushManifest(containerRef1, manifest1);
        assertDoesNotThrow(() -> {
            registry.pullArtifact(containerRef1, artifactDir, true);
        });
    }

    @Test
    void pullArtifactShouldPullLayerWithTitleOnly() {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef1 = ContainerRef.parse("%s/empty-layers-title".formatted(this.registry.getRegistry()));
        Config emptyConfig = Config.empty();
        Manifest manifest1 = Manifest.empty().withConfig(emptyConfig);
        Layer layer = registry.pushBlob(containerRef1, "hello".getBytes(StandardCharsets.UTF_8))
                .withAnnotations(Map.of(Const.ANNOTATION_TITLE, "file.txt"));
        Layer layerWithoutTitle = registry.pushBlob(containerRef1, "hello".getBytes(StandardCharsets.UTF_8));
        manifest1 = manifest1.withLayers(List.of(layer, layerWithoutTitle));
        registry.pushConfig(containerRef1, emptyConfig);
        registry.pushManifest(containerRef1, manifest1);
        assertDoesNotThrow(() -> {
            registry.pullArtifact(containerRef1, artifactDir, true);
        });
    }

    @Test
    void pullArtifactShouldPullLayerWithNoTitle() {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef1 = ContainerRef.parse("%s/no-layers-title".formatted(this.registry.getRegistry()));
        Config emptyConfig = Config.empty();
        Manifest manifest1 = Manifest.empty().withConfig(emptyConfig);
        Layer layer = registry.pushBlob(containerRef1, "hello".getBytes(StandardCharsets.UTF_8))
                .withAnnotations(Map.of());
        Layer layerWithoutTitle = registry.pushBlob(containerRef1, "hello".getBytes(StandardCharsets.UTF_8));
        manifest1 = manifest1.withLayers(List.of(layer, layerWithoutTitle));
        registry.pushConfig(containerRef1, emptyConfig);
        registry.pushManifest(containerRef1, manifest1);
        assertDoesNotThrow(() -> {
            registry.pullArtifact(containerRef1, artifactDir, true);
        });
    }

    @Test
    void shouldPushComplexArtifactWithConfigMediaType() throws IOException {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();

        // Manifest 1
        ContainerRef containerRef1 = ContainerRef.parse("%s/library/manifest1".formatted(this.registry.getRegistry()));
        ContainerRef containerRef2 = ContainerRef.parse("%s/library/manifest2".formatted(this.registry.getRegistry()));

        String content1 = "hello";
        String content2 = "world";

        // Push some blobs
        Layer layer11 = registry.pushBlob(containerRef1, content1.getBytes(StandardCharsets.UTF_8))
                .withAnnotations(Map.of(Const.ANNOTATION_TITLE, "file11.txt"));
        Layer layer12 = registry.pushBlob(containerRef1, content2.getBytes(StandardCharsets.UTF_8))
                .withAnnotations(Map.of(Const.ANNOTATION_TITLE, "file12.txt"));
        Layer layer21 = registry.pushBlob(containerRef2, content1.getBytes(StandardCharsets.UTF_8))
                .withAnnotations(Map.of(Const.ANNOTATION_TITLE, "file21.txt"));
        Layer layer22 = registry.pushBlob(containerRef2, content2.getBytes(StandardCharsets.UTF_8))
                .withAnnotations(Map.of(Const.ANNOTATION_TITLE, "file22.txt"));

        // Push 2 manifests
        Manifest manifest1 = Manifest.empty().withLayers(List.of(layer11, layer12));
        Manifest manifest2 = Manifest.empty().withLayers(List.of(layer21, layer22));

        // Push empty config
        Config config1 = registry.pushConfig(containerRef1, Config.empty().withMediaType("text/plain"));
        Config config2 = registry.pushConfig(containerRef2, Config.empty().withMediaType("text/plain"));
        manifest1 = manifest1.withConfig(config1);
        manifest2 = manifest2.withConfig(config2);

        registry.pushManifest(containerRef1, manifest1);
        registry.pushManifest(containerRef2, manifest2);

        registry.pullArtifact(containerRef1, artifactDir, true);
        registry.pullArtifact(containerRef2, artifactDir, true);

        // Assert all file exists and have correct content
        assertEquals(content1, Files.readString(artifactDir.resolve("file11.txt")));
        assertEquals(content2, Files.readString(artifactDir.resolve("file12.txt")));
        assertEquals(content1, Files.readString(artifactDir.resolve("file21.txt")));
        assertEquals(content2, Files.readString(artifactDir.resolve("file22.txt")));

        // pull manifest
        manifest1 = registry.getManifest(containerRef1);
        manifest2 = registry.getManifest(containerRef2);

        assertEquals("text/plain", manifest1.getArtifactType().getMediaType());
        assertEquals("text/plain", manifest2.getArtifactType().getMediaType());
        assertEquals("text/plain", manifest1.getConfig().getMediaType());
        assertEquals("text/plain", manifest2.getConfig().getMediaType());
    }

    @Test
    void shouldPushComplexArtifactWithConfigArtifactType() throws IOException {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();

        // Manifest 1
        ContainerRef containerRef1 = ContainerRef.parse("%s/library/manifest1".formatted(this.registry.getRegistry()));
        ContainerRef containerRef2 = ContainerRef.parse("%s/library/manifest2".formatted(this.registry.getRegistry()));

        String content1 = "hello";
        String content2 = "world";

        // Push some blobs
        Layer layer11 = registry.pushBlob(containerRef1, content1.getBytes(StandardCharsets.UTF_8))
                .withAnnotations(Map.of(Const.ANNOTATION_TITLE, "file11.txt"));
        Layer layer12 = registry.pushBlob(containerRef1, content2.getBytes(StandardCharsets.UTF_8))
                .withAnnotations(Map.of(Const.ANNOTATION_TITLE, "file12.txt"));
        Layer layer21 = registry.pushBlob(containerRef2, content1.getBytes(StandardCharsets.UTF_8))
                .withAnnotations(Map.of(Const.ANNOTATION_TITLE, "file21.txt"));
        Layer layer22 = registry.pushBlob(containerRef2, content2.getBytes(StandardCharsets.UTF_8))
                .withAnnotations(Map.of(Const.ANNOTATION_TITLE, "file22.txt"));

        // Push 2 manifests
        Manifest manifest1 = Manifest.empty()
                .withLayers(List.of(layer11, layer12))
                .withArtifactType(ArtifactType.from("text/plain"));
        Manifest manifest2 = Manifest.empty()
                .withLayers(List.of(layer21, layer22))
                .withArtifactType(ArtifactType.from("text/plain"));

        // Push empty config
        Config config1 = registry.pushConfig(containerRef1, Config.empty());
        Config config2 = registry.pushConfig(containerRef2, Config.empty());
        manifest1 = manifest1.withConfig(config1);
        manifest2 = manifest2.withConfig(config2);

        registry.pushManifest(containerRef1, manifest1);
        registry.pushManifest(containerRef2, manifest2);

        registry.pullArtifact(containerRef1, artifactDir, true);
        registry.pullArtifact(containerRef2, artifactDir, true);

        // Assert all file exists and have correct content
        assertEquals(content1, Files.readString(artifactDir.resolve("file11.txt")));
        assertEquals(content2, Files.readString(artifactDir.resolve("file12.txt")));
        assertEquals(content1, Files.readString(artifactDir.resolve("file21.txt")));
        assertEquals(content2, Files.readString(artifactDir.resolve("file22.txt")));

        // Assert media type
        assertEquals("text/plain", manifest1.getArtifactType().getMediaType());
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void shouldListReferrers(@TempDir Path homeDir) throws Exception {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();

        // Manifest 1
        ContainerRef containerRef1 =
                ContainerRef.parse("%s/library/manifest1:latest".formatted(this.registry.getRegistry()));

        String content1 = "hello";
        String content2 = "world";

        // Push some blobs
        Layer layer11 = registry.pushBlob(containerRef1, content1.getBytes(StandardCharsets.UTF_8))
                .withAnnotations(Map.of(Const.ANNOTATION_TITLE, "file1.txt"));
        Layer layer12 = registry.pushBlob(containerRef1, content2.getBytes(StandardCharsets.UTF_8))
                .withAnnotations(Map.of(Const.ANNOTATION_TITLE, "file2.txt"));

        // Push empty config
        Config config1 = registry.pushConfig(containerRef1, Config.empty());

        // Push manifest 1
        Manifest manifest1 = Manifest.empty()
                .withLayers(List.of(layer11, layer12))
                .withConfig(config1)
                .withArtifactType(ArtifactType.from("text/plain"));
        manifest1 = registry.pushManifest(containerRef1, manifest1);

        // Create manifest 2 with subject
        Manifest manifest2 = Manifest.empty()
                .withSubject(manifest1.getDescriptor().toSubject())
                .withAnnotations(Map.of(Const.ANNOTATION_CREATED, Const.currentTimestamp()))
                .withArtifactType(ArtifactType.from("text/plain"));

        // Push second manifest with its digest
        ContainerRef containerRef2 = ContainerRef.parse("%s/library/manifest1@%s"
                .formatted(
                        this.registry.getRegistry(),
                        SupportedAlgorithm.SHA256.digest(manifest2.toJson().getBytes(StandardCharsets.UTF_8))));
        registry.pushManifest(containerRef2, manifest2);

        // Pull via artifact 2
        registry.pullArtifact(containerRef2, artifactDir, true);

        // Assert files doesn't exist since we dont' follow subject
        assertFalse(Files.exists(artifactDir.resolve("file1.txt")), "file1.txt should not exist");
        assertFalse(Files.exists(artifactDir.resolve("file2.txt")), "file2.txt should not exist");

        // Pull via artifact 1
        registry.pullArtifact(containerRef1, artifactDir, true);

        // File should exists
        assertTrue(Files.exists(artifactDir.resolve("file1.txt")), "file1.txt should exist");
        assertTrue(Files.exists(artifactDir.resolve("file2.txt")), "file2.txt should exist");

        Referrers referrers = registry.getReferrers(
                containerRef1.withDigest(manifest1.getDescriptor().getDigest()), null);
        assertEquals(Const.DEFAULT_INDEX_MEDIA_TYPE, referrers.getMediaType());
        assertEquals(1, referrers.getManifests().size(), "Should have only 1 manifest referrer");

        // Ensure the referrer of manifest1 is manifest2
        ManifestDescriptor referedManifest = referrers.getManifests().get(0);
        manifest2 = registry.getManifest(containerRef2);
        assertEquals("text/plain", referedManifest.getArtifactType(), "Artifact type should match");
        assertEquals(manifest2.getDescriptor().getSize(), referedManifest.getSize(), "Manifest size should match");
        assertEquals(
                manifest2.getDescriptor().getDigest(), referedManifest.getDigest(), "Manifest digest should match");

        // Filter by artifact type
        referrers = registry.getReferrers(
                containerRef1.withDigest(manifest1.getDescriptor().getDigest()), null);
        assertEquals(Const.DEFAULT_INDEX_MEDIA_TYPE, referrers.getMediaType());
        assertEquals(1, referrers.getManifests().size(), "Should have only 1 manifest referrer");
        assertEquals("text/plain", referedManifest.getArtifactType(), "Artifact type should match");
        assertEquals(manifest2.getDescriptor().getSize(), referedManifest.getSize(), "Manifest size should match");
        assertEquals(
                manifest2.getDescriptor().getDigest(), referedManifest.getDigest(), "Manifest digest should match");

        // Assert JSON serialization
        assertEquals(Referrers.fromJson(referrers.toJson()).getMediaType(), referrers.getMediaType());

        // Use config

        // language=toml
        String config =
                """
            [[registry]]
            location = "%s"
            insecure = true
            """
                        .formatted(this.registry.getRegistry());
        TestUtils.createRegistriesConfFile(homeDir, config);

        TestUtils.withHome(homeDir, () -> {
            Registry newRegistry =
                    Registry.Builder.builder().defaults("myuser", "mypass").build();
            Manifest getManifest1 = newRegistry.getManifest(containerRef1);
            Referrers newReferrers = newRegistry.getReferrers(
                    containerRef1.withDigest(getManifest1.getDescriptor().getDigest()), null);
            assertNotNull(newReferrers);
        });
    }

    @Test
    void testShouldFailReferrerWithoutDigest() {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        assertThrows(
                OrasException.class,
                () -> {
                    registry.getReferrers(
                            ContainerRef.parse("%s/library/manifest1".formatted(this.registry.getRegistry())), null);
                },
                "Digest is required to get referrers");
    }

    @Test
    void testShouldCopySingleArtifactFromRegistryIntoRegistry() throws IOException {
        // Copy to same registry
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();

        ContainerRef containerSource =
                ContainerRef.parse("%s/library/artifact-source".formatted(this.registry.getRegistry()));
        Path file1 = blobDir.resolve("source.txt");
        Files.writeString(file1, "foobar");

        // Push
        Manifest manifest = registry.pushArtifact(containerSource, LocalPath.of(file1));
        assertNotNull(manifest);

        // Copy to other registry
        try (RegistryContainer otherRegistryContainer = new RegistryContainer()) {
            otherRegistryContainer.start();
            ContainerRef containerTarget =
                    ContainerRef.parse("%s/library/artifact-target".formatted(otherRegistryContainer.getRegistry()));
            CopyUtils.copy(registry, containerSource, registry, containerTarget, false);
            registry.pullArtifact(containerTarget, artifactDir, true);
            assertEquals("foobar", Files.readString(artifactDir.resolve("source.txt")));
        }
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void testShouldArtifactWithAlias(@TempDir Path homeDir) throws Exception {

        // language=toml
        String config =
                """
            [aliases]
            "the-target" = "%s/test/artifact-target"

            [[registry]]
            location = "%s"
            insecure = true
            """
                        .formatted(this.registry.getRegistry(), this.registry.getRegistry());
        TestUtils.createRegistriesConfFile(homeDir, config);

        // Copy to same registry
        TestUtils.withHome(homeDir, () -> {
            try {
                Registry registry =
                        Registry.Builder.builder().defaults("myuser", "mypass").build();

                ContainerRef originalRef = ContainerRef.parse("the-target");
                Path file1 = blobDir.resolve("source.txt");
                Files.writeString(file1, "foobar");

                // Push
                Manifest manifest = registry.pushArtifact(originalRef, LocalPath.of(file1));
                assertNotNull(manifest);

                // Pull
                registry.pullArtifact(originalRef, artifactDir, true);
                assertEquals("foobar", Files.readString(artifactDir.resolve("source.txt")));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void testShouldCopyFromAliasToAlias(@TempDir Path homeDir) throws Exception {

        try (RegistryContainer otherRegistryContainer = new RegistryContainer()) {

            otherRegistryContainer.start();

            // language=toml
            String config =
                    """
                [aliases]
                "the-source" = "%s/test/artifact-source"
                "the-target" = "%s/test/artifact-target"

                [[registry]]
                location = "%s"
                insecure = true

                [[registry]]
                location = "%s"
                insecure = true
                """
                            .formatted(
                                    this.registry.getRegistry(),
                                    otherRegistryContainer.getRegistry(),
                                    this.registry.getRegistry(),
                                    otherRegistryContainer.getRegistry());
            TestUtils.createRegistriesConfFile(homeDir, config);

            // Copy to same registry
            TestUtils.withHome(homeDir, () -> {
                try {
                    Registry registry = Registry.Builder.builder()
                            .defaults("myuser", "mypass")
                            .build();

                    ContainerRef containerSource = ContainerRef.parse("the-source");
                    Path file1 = blobDir.resolve("source.txt");
                    Files.writeString(file1, "foobar");

                    // Push
                    Manifest manifest = registry.pushArtifact(containerSource, LocalPath.of(file1));
                    assertNotNull(manifest);

                    // Copy to other registry
                    ContainerRef containerTarget = ContainerRef.parse("the-target");
                    CopyUtils.copy(registry, containerSource, registry, containerTarget, false);
                    registry.pullArtifact(containerTarget, artifactDir, true);
                    assertEquals("foobar", Files.readString(artifactDir.resolve("source.txt")));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    void testShouldCopyFromOciLayoutToRegistryNonRecursive() throws IOException {

        // Registry to copy
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef targetRef =
                ContainerRef.parse("%s/library/copied-from-oci-layout".formatted(this.registry.getRegistry()));

        LayoutRef layoutRef = LayoutRef.parse("src/test/resources/oci/subject:latest");
        OCILayout ociLayout =
                OCILayout.Builder.builder().defaults(layoutRef.getFolder()).build();

        CopyUtils.copy(ociLayout, layoutRef, registry, targetRef, false);

        // Pull
        Path extractPath = artifactDir.resolve("testShouldCopyFromOciLayoutToRegistryNonRecursive");
        Files.createDirectory(extractPath);
        registry.pullArtifact(targetRef, extractPath, true);

        assertTrue(Files.exists(extractPath.resolve("hi.txt")), "hi.txt should exist");

        // Cannot pull referrer due to shallow copy
        assertThrows(
                OrasException.class,
                () -> {
                    registry.pullArtifact(
                            targetRef.withDigest(
                                    "sha256:ccec2a2be7ce7c6aadc8ed0dc03df8f91cbd3534272dd1f8284226a8d3516dd6"),
                            extractPath,
                            true);
                },
                "Referrer should not be pulled");
    }

    @Test
    void testShouldCopyFromOciLayoutToRegistryRecursive() throws IOException {

        // Registry to copy
        Registry registry = Registry.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef targetRef = ContainerRef.parse(
                "%s/library/copied-from-oci-layout-recursive".formatted(this.registry.getRegistry()));

        LayoutRef layoutRef = LayoutRef.parse("src/test/resources/oci/subject:latest");
        OCILayout ociLayout =
                OCILayout.Builder.builder().defaults(layoutRef.getFolder()).build();

        CopyUtils.copy(ociLayout, layoutRef, registry, targetRef, true);

        // Pull
        Path extractPath = artifactDir.resolve("testShouldCopyFromOciLayoutToRegistryRecursive");
        Files.createDirectory(extractPath);
        registry.pullArtifact(targetRef, extractPath, true);

        assertTrue(Files.exists(extractPath.resolve("hi.txt")), "hi.txt should exist");

        // Assert referrer
        registry.pullArtifact(
                targetRef.withDigest("sha256:ccec2a2be7ce7c6aadc8ed0dc03df8f91cbd3534272dd1f8284226a8d3516dd6"),
                extractPath,
                true);
        assertTrue(Files.exists(extractPath.resolve("hi2.txt")), "hi2.txt should exist");
    }

    @Test
    void testNotFailToPullArtifactFromImage() {

        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();

        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-image-pull".formatted(this.registry.getRegistry()));

        Layer emptyLayer = registry.pushBlob(containerRef, Layer.empty().getDataBytes());

        Manifest emptyManifest = Manifest.empty().withLayers(List.of(Layer.fromDigest(emptyLayer.getDigest(), 2)));
        String manifestDigest =
                SupportedAlgorithm.SHA256.digest(emptyManifest.toJson().getBytes(StandardCharsets.UTF_8));
        String configDigest = Config.empty().getDigest();

        // Push config and manifest
        registry.pushConfig(containerRef.withDigest(configDigest), Config.empty());
        Manifest pushedManifest = registry.pushManifest(containerRef.withDigest(manifestDigest), emptyManifest);

        Index emptyIndex = Index.fromManifests(List.of(pushedManifest.getDescriptor()));
        Index pushIndex = registry.pushIndex(containerRef, emptyIndex);

        // Copy to oci layout
        registry.pullArtifact(containerRef, artifactDir, true);

        assertEquals(1, pushIndex.getManifests().size());
        assertEquals(2, pushIndex.getSchemaVersion());
        assertEquals(Const.DEFAULT_INDEX_MEDIA_TYPE, pushIndex.getMediaType());
    }

    @Test
    void testShouldPushAndPullMinimalArtifact() throws IOException {

        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-full".formatted(this.registry.getRegistry()));
        ContainerRef unknown =
                ContainerRef.parse("%s/library/artifact-full:unknown".formatted(this.registry.getRegistry()));

        Path file1 = blobDir.resolve("file1.txt");
        Files.writeString(file1, "foobar");

        // Upload
        Manifest manifest = registry.pushArtifact(containerRef, LocalPath.of(file1));
        assertEquals(1, manifest.getLayers().size());
        assertEquals(
                Const.DEFAULT_ARTIFACT_MEDIA_TYPE, manifest.getArtifactType().getMediaType());

        assertTrue(registry.exists(containerRef), "Artifact should exist");
        assertFalse(registry.exists(unknown), "Artifact should not exist");

        // Ensure one annotation (created by the SDK)
        Map<String, String> manifestAnnotations = manifest.getAnnotations();
        assertEquals(1, manifestAnnotations.size(), "Annotations size is incorrect");
        assertNotNull(manifestAnnotations.get(Const.ANNOTATION_CREATED), "Created annotation is missing");

        // Assert config
        Config config = manifest.getConfig();
        assertEquals(Const.DEFAULT_EMPTY_MEDIA_TYPE, config.getMediaType());
        assertEquals("sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a", config.getDigest());
        assertEquals(2, config.getSize());
        assertEquals("{}", new String(config.getDataBytes()));

        // Null annotations
        assertNull(config.getAnnotations(), "Annotations should be null");

        Layer layer = manifest.getLayers().get(0);

        // A test file layer
        assertEquals(6, layer.getSize());
        assertEquals(Const.DEFAULT_BLOB_MEDIA_TYPE, layer.getMediaType());
        assertEquals("sha256:c3ab8ff13720e8ad9047dd39466b3c8974e592c2fa383d4a3960714caef0c4f2", layer.getDigest());

        Map<String, String> annotations = layer.getAnnotations();

        // Assert annotations of the layer
        assertEquals(1, annotations.size());
        assertEquals("file1.txt", annotations.get(Const.ANNOTATION_TITLE));

        // Pull
        registry.pullArtifact(containerRef, artifactDir, true);
        assertEquals("foobar", Files.readString(artifactDir.resolve("file1.txt")));
        FileTime before = Files.getLastModifiedTime(artifactDir.resolve("file1.txt"));

        // Pull, but don't overwrite
        registry.pullArtifact(containerRef, artifactDir, false);
        assertEquals("foobar", Files.readString(artifactDir.resolve("file1.txt")));
        FileTime after = Files.getLastModifiedTime(artifactDir.resolve("file1.txt"));
        assertEquals(before, after, "File should not be modified");

        // Pull again with overwrite
        registry.pullArtifact(containerRef, artifactDir, true);
        assertEquals("foobar", Files.readString(artifactDir.resolve("file1.txt")));
        after = Files.getLastModifiedTime(artifactDir.resolve("file1.txt"));
        assertNotEquals(before, after, "File should be modified");
    }

    @Test
    void testShouldPushMinimalArtifactThenAttachArtifactToManifest() throws IOException {

        String artifactType = "application/vnd.maven+type";

        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();

        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-maven".formatted(this.registry.getRegistry()));

        Path pomFile = blobDir.resolve("pom.xml");
        Files.writeString(pomFile, "my pom file");

        // Push the main OCI artifact
        assertNotNull(registry.pushArtifact(
                containerRef, ArtifactType.from(artifactType), LocalPath.of(pomFile, "application/xml")));

        // Create fake signature
        Path signedPomFile = blobDir.resolve("pom.xml.asc");
        Files.writeString(signedPomFile, "my signed pom file");

        // Attach artifact
        Manifest signedPomFileManifest =
                registry.attachArtifact(containerRef, ArtifactType.from(artifactType), LocalPath.of(signedPomFile));

        assertEquals(1, signedPomFileManifest.getLayers().size());
        assertEquals(1, signedPomFileManifest.getAnnotations().size());
        assertNotNull(signedPomFileManifest.getAnnotations().get(Const.ANNOTATION_CREATED));

        // No created annotation
        signedPomFileManifest = registry.attachArtifact(
                containerRef, ArtifactType.from(artifactType), Annotations.empty(), LocalPath.of(signedPomFile));

        assertEquals(1, signedPomFileManifest.getLayers().size());
        assertEquals(1, signedPomFileManifest.getAnnotations().size());
        assertNotNull(signedPomFileManifest.getAnnotations().get(Const.ANNOTATION_CREATED));
    }

    @Test
    void testShouldPushMinimalArtifactThenAttachArtifactToIndex() throws IOException {

        String artifactType = "application/vnd.maven+type";

        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();

        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-maven".formatted(this.registry.getRegistry()));

        Path pomFile = blobDir.resolve("pom.xml");
        Files.writeString(pomFile, "my pom file");

        Manifest manifest = registry.pushArtifact(
                containerRef, ArtifactType.from(artifactType), LocalPath.of(pomFile, "application/xml"));
        assertNotNull(manifest);

        Index index = Index.fromManifests(List.of(manifest.getDescriptor()));
        String indexDigest =
                SupportedAlgorithm.getDefault().digest(index.toJson().getBytes(StandardCharsets.UTF_8));
        assertNotNull(manifest);
        index = registry.pushIndex(containerRef.withDigest(indexDigest), index);

        assertNotNull(indexDigest);

        // Create fake signature
        Path signedPomFile = blobDir.resolve("pom.xml.asc");
        Files.writeString(signedPomFile, "my signed pom file");

        // Attach artifact to index
        Manifest signedPomFileManifest = registry.attachArtifact(
                containerRef.withDigest(indexDigest), ArtifactType.from(artifactType), LocalPath.of(signedPomFile));

        assertEquals(1, signedPomFileManifest.getLayers().size());
        assertEquals(1, signedPomFileManifest.getAnnotations().size());
        assertNotNull(signedPomFileManifest.getAnnotations().get(Const.ANNOTATION_CREATED));

        // No created annotation
        signedPomFileManifest = registry.attachArtifact(
                containerRef, ArtifactType.from(artifactType), Annotations.empty(), LocalPath.of(signedPomFile));

        assertEquals(1, signedPomFileManifest.getLayers().size());
        assertEquals(1, signedPomFileManifest.getAnnotations().size());
        assertNotNull(signedPomFileManifest.getAnnotations().get(Const.ANNOTATION_CREATED));
    }

    @Test
    void testShouldArtifactWithAnnotations() throws IOException {

        String artifactType = "application/vnd.maven+type";

        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();

        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-maven".formatted(this.registry.getRegistry()));

        Path pomFile = blobDir.resolve("pom.xml");
        Files.writeString(pomFile, "my pom file");

        // Push the main OCI artifact
        Annotations annotations =
                Annotations.ofManifest(Map.of("foo", "bar")).withFileAnnotations("jenkins.png", Map.of("foo", "bar"));

        // Add image (without title (so it's not unpack) and specific annotation)
        Manifest manifest = registry.pushArtifact(
                containerRef,
                ArtifactType.from(artifactType),
                annotations,
                LocalPath.of(pomFile, "application/xml"),
                LocalPath.of(Path.of("src/test/resources/img/jenkins.png"), "image/png"));

        // Check annotations (manifest)
        assertEquals(2, manifest.getAnnotations().size());
        assertEquals("bar", manifest.getAnnotations().get("foo"));
        assertNotNull(manifest.getAnnotations().get(Const.ANNOTATION_CREATED));

        // Check annotations (layer 0)
        Layer layer = manifest.getLayers().get(0);
        assertEquals(1, layer.getAnnotations().size());
        assertEquals(
                "pom.xml", layer.getAnnotations().get(Const.ANNOTATION_TITLE), "Title annotation should be pom.xml");

        // Check annotation (layer 1)
        Layer layer2 = manifest.getLayers().get(1);
        assertEquals(1, layer2.getAnnotations().size());
        assertNull(layer2.getAnnotations().get(Const.ANNOTATION_TITLE), "Title should not be added");
        assertEquals("bar", layer2.getAnnotations().get("foo"), "Custom annotation should be preserved");
    }

    @Test
    void testShouldPushAndPullCompressedZipDirectoryButNotUnpackIt() throws IOException {

        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-zip".formatted(this.registry.getRegistry()));

        Path file1 = blobDir.resolve("file1.txt");
        Path file2 = blobDir.resolve("file2.txt");
        Path file3 = blobDir.resolve("file3.txt");
        Files.writeString(file1, "foobar");
        Files.writeString(file2, "test1234");
        Files.writeString(file3, "barfoo");

        // Upload blob dir
        Manifest manifest = registry.pushArtifact(containerRef, LocalPath.of(blobDir, Const.ZIP_MEDIA_TYPE));
        assertEquals(1, manifest.getLayers().size());

        Layer layer = manifest.getLayers().get(0);

        // A compressed directory file as zip
        assertEquals(Const.ZIP_MEDIA_TYPE, layer.getMediaType());
        Map<String, String> annotations = layer.getAnnotations();

        // Assert annotations of the layer (no unpack here)
        assertEquals(2, annotations.size());
        assertEquals("false", annotations.get(Const.ANNOTATION_ORAS_UNPACK));

        // Title must container filename
        assertEquals(
                "%s.%s".formatted(blobDir.getFileName().toString(), SupportedCompression.ZIP.getFileExtension()),
                annotations.get(Const.ANNOTATION_TITLE));

        // Pull
        registry.pullArtifact(containerRef, extractDirZip, true);

        // Assert extracted files
        Path zipFile = extractDirZip.resolve(annotations.get(Const.ANNOTATION_TITLE));
        assertTrue(Files.exists(zipFile), "Extracted zip file should exist");
        assertTrue(Files.isRegularFile(zipFile), "Extracted zip file should be a file");
        assertFalse(Files.isRegularFile(extractDirZip.resolve("file1.txt")), "file1.txt should not be extracted");
    }

    @Test
    void testShouldPushAndPullCompressedTarGzDirectory() throws IOException {

        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-full".formatted(this.registry.getRegistry()));

        Path file1 = blobDir.resolve("file1.txt");
        Path file2 = blobDir.resolve("file2.txt");
        Path file3 = blobDir.resolve("file3.txt");
        Files.writeString(file1, "foobar");
        Files.writeString(file2, "test1234");
        Files.writeString(file3, "barfoo");

        // Upload blob dir
        Manifest manifest = registry.pushArtifact(containerRef, LocalPath.of(blobDir));
        assertEquals(1, manifest.getLayers().size());

        Layer layer = manifest.getLayers().get(0);

        // A compressed directory file
        assertEquals(Const.DEFAULT_BLOB_DIR_MEDIA_TYPE, layer.getMediaType());
        Map<String, String> annotations = layer.getAnnotations();

        // Assert annotations of the layer
        assertEquals(3, annotations.size());
        assertEquals(blobDir.getFileName().toString(), annotations.get(Const.ANNOTATION_TITLE));
        assertEquals("true", annotations.get(Const.ANNOTATION_ORAS_UNPACK));
        assertEquals(
                SupportedAlgorithm.SHA256,
                SupportedAlgorithm.fromDigest(annotations.get(Const.ANNOTATION_ORAS_CONTENT_DIGEST)));

        // Pull
        registry.pullArtifact(containerRef, extractDir, true);

        // Assert extracted files
        Path extractedDir = extractDir.resolve(blobDir.getFileName());
        assertEquals("foobar", Files.readString(extractedDir.resolve("file1.txt")));
        assertEquals("test1234", Files.readString(extractedDir.resolve("file2.txt")));
        assertEquals("barfoo", Files.readString(extractedDir.resolve("file3.txt")));
    }

    @Test
    void testShouldPushAndPullUncompressedTarDirectoryWithAbsolutePath() throws IOException {

        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-not-compressed".formatted(this.registry.getRegistry()));

        Path file1 = blobDir.resolve("file1.txt");
        Path file2 = blobDir.resolve("file2.txt");
        Path file3 = blobDir.resolve("file3.txt");
        Files.writeString(file1, "foobar");
        Files.writeString(file2, "test1234");
        Files.writeString(file3, "barfoo");

        // Upload blob dir
        Manifest manifest = registry.pushArtifact(containerRef, LocalPath.of(blobDir, Const.DEFAULT_BLOB_MEDIA_TYPE));
        assertEquals(1, manifest.getLayers().size());

        Layer layer = manifest.getLayers().get(0);

        // A compressed directory file
        assertEquals(Const.DEFAULT_BLOB_MEDIA_TYPE, layer.getMediaType());
        Map<String, String> annotations = layer.getAnnotations();

        // Assert annotations of the layer
        assertEquals(3, annotations.size());
        assertEquals(blobDir.getFileName().toString(), annotations.get(Const.ANNOTATION_TITLE));
        assertEquals("true", annotations.get(Const.ANNOTATION_ORAS_UNPACK));
        assertEquals(
                SupportedAlgorithm.SHA256,
                SupportedAlgorithm.fromDigest(annotations.get(Const.ANNOTATION_ORAS_CONTENT_DIGEST)));

        // Pull
        registry.pullArtifact(containerRef, extractDir, true);

        // Assert extracted files
        Path extractedDir = this.extractDir.resolve(blobDir.getFileName());
        assertEquals("foobar", Files.readString(extractedDir.resolve("file1.txt")));
        assertEquals("test1234", Files.readString(extractedDir.resolve("file2.txt")));
        assertEquals("barfoo", Files.readString(extractedDir.resolve("file3.txt")));
    }

    @Test
    void testShouldPushAndPullUncompressedTarDirectoryWithRelativePath() throws IOException {

        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-relative-path".formatted(this.registry.getRegistry()));

        // Source
        Manifest manifest = registry.pushArtifact(
                containerRef, LocalPath.of(Path.of("src/main/java"), Const.DEFAULT_BLOB_MEDIA_TYPE));
        assertEquals(1, manifest.getLayers().size());

        Layer layer = manifest.getLayers().get(0);

        // A compressed directory file
        assertEquals(Const.DEFAULT_BLOB_MEDIA_TYPE, layer.getMediaType());
        Map<String, String> annotations = layer.getAnnotations();

        // Assert annotations of the layer
        assertEquals(3, annotations.size());
        assertEquals("src/main/java", annotations.get(Const.ANNOTATION_TITLE)); // Keep relative path
        assertEquals("true", annotations.get(Const.ANNOTATION_ORAS_UNPACK));
        assertEquals(
                SupportedAlgorithm.SHA256,
                SupportedAlgorithm.fromDigest(annotations.get(Const.ANNOTATION_ORAS_CONTENT_DIGEST)));

        // Pull
        registry.pullArtifact(containerRef, extractDir, false);

        // Assert files under src/main/java
        Path extractedDir = this.extractDir.resolve("src/main/java");
        assertTrue(Files.exists(extractedDir.resolve("land/oras/Config.java")), "Config.java should exist");
    }

    @Test
    void testShouldPushAndPullCompressedZstdDirectory() throws IOException {

        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-full".formatted(this.registry.getRegistry()));

        Path file1 = blobDir.resolve("file1.txt");
        Path file2 = blobDir.resolve("file2.txt");
        Path file3 = blobDir.resolve("file3.txt");
        Files.writeString(file1, "foobar");
        Files.writeString(file2, "test1234");
        Files.writeString(file3, "barfoo");

        // Upload blob dir with the zstd compression
        Manifest manifest = registry.pushArtifact(containerRef, LocalPath.of(blobDir, Const.BLOB_DIR_ZSTD_MEDIA_TYPE));
        assertEquals(1, manifest.getLayers().size());

        Layer layer = manifest.getLayers().get(0);

        // A compressed directory file
        assertEquals(Const.BLOB_DIR_ZSTD_MEDIA_TYPE, layer.getMediaType());
        Map<String, String> annotations = layer.getAnnotations();

        // Assert annotations of the layer
        assertEquals(3, annotations.size());
        assertEquals(blobDir.getFileName().toString(), annotations.get(Const.ANNOTATION_TITLE));
        assertEquals("true", annotations.get(Const.ANNOTATION_ORAS_UNPACK));
        assertEquals(
                SupportedAlgorithm.SHA256,
                SupportedAlgorithm.fromDigest(annotations.get(Const.ANNOTATION_ORAS_CONTENT_DIGEST)));

        // Pull
        registry.pullArtifact(containerRef, extractDir, true);

        // Assert extracted files
        Path extractedDir = extractDir.resolve(blobDir.getFileName());
        assertEquals("foobar", Files.readString(extractedDir.resolve("file1.txt")));
        assertEquals("test1234", Files.readString(extractedDir.resolve("file2.txt")));
        assertEquals("barfoo", Files.readString(extractedDir.resolve("file3.txt")));
    }

    @Test
    void shouldFailToPushDirectoryWithInvalidCompression() throws IOException {

        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-full".formatted(this.registry.getRegistry()));

        assertThrows(
                OrasException.class,
                () -> {
                    registry.pushArtifact(containerRef, LocalPath.of(blobDir, "invalid-compression"));
                },
                "Invalid compression format");
    }

    @Test
    void shouldPushAndGetBlobStreamSha256() throws IOException {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-stream".formatted(this.registry.getRegistry()));

        // Create a file with test data to get accurate stream size
        Path testFile = Files.createTempFile("test-data-", ".tmp");
        String testData = "Hello World Stream Test";
        Files.writeString(testFile, testData);
        long fileSize = Files.size(testFile);

        // Test pushBlobStream using file input stream
        Layer layer;
        try (InputStream inputStream = Files.newInputStream(testFile)) {
            layer = registry.pushBlob(containerRef, inputStream);

            // Verify the digest matches SHA-256 of content
            assertEquals(SupportedAlgorithm.SHA256, containerRef.getAlgorithm());
            assertEquals(containerRef.getAlgorithm().digest(testFile), layer.getDigest());
            assertEquals(fileSize, layer.getSize());
        }

        // Test getBlobStream
        try (InputStream resultStream = registry.getBlobStream(containerRef.withDigest(layer.getDigest()))) {
            String result = new String(resultStream.readAllBytes());
            assertEquals(testData, result);
        }

        // Clean up
        Files.delete(testFile);
        registry.deleteBlob(containerRef.withDigest(layer.getDigest()));
    }

    @Test
    void shouldPushAndGetBlobStreamWithSha512() throws IOException {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef = ContainerRef.parse(
                "%s/library/artifact-stream-sha512@sha512:ea0d8750d01f5fbd0da5d020d981b377fa2177874751063cb3da2117e481720774c0d985845a56c32ee6dde144901d92b2bdc8d0cb02373da141241aa2409859"
                        .formatted(this.registry.getRegistry()));

        // Create a file with test data to get accurate stream size
        Path testFile = Files.createTempFile("test-data-", ".tmp");
        String testData = "Hello World Stream Test";
        Files.writeString(testFile, testData);
        long fileSize = Files.size(testFile);

        // Test pushBlobStream using file input stream
        Layer layer;
        try (InputStream inputStream = Files.newInputStream(testFile)) {
            layer = registry.pushBlob(containerRef, inputStream);

            // Verify the digest matches SHA-512 of content
            assertEquals(SupportedAlgorithm.SHA512, containerRef.getAlgorithm());
            assertEquals(containerRef.getAlgorithm().digest(testFile), layer.getDigest());
            assertEquals(fileSize, layer.getSize());
        }

        // Test getBlobStream
        try (InputStream resultStream = registry.getBlobStream(containerRef.withDigest(layer.getDigest()))) {
            String result = new String(resultStream.readAllBytes());
            assertEquals(testData, result);
        }

        // Clean up
        Files.delete(testFile);
        registry.deleteBlob(containerRef.withDigest(layer.getDigest()));
    }

    @Test
    void shouldHandleExistingBlobInStreamPush() throws IOException {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-stream".formatted(this.registry.getRegistry()));

        // Create test file
        Path testFile = Files.createTempFile("test-data-", ".tmp");
        Files.writeString(testFile, "Test Content");
        long fileSize = Files.size(testFile);
        String expectedDigest = containerRef.getAlgorithm().digest(testFile);

        // First push
        Layer firstLayer;
        try (InputStream inputStream = Files.newInputStream(testFile)) {
            firstLayer = registry.pushBlob(containerRef, inputStream);
        }

        // Second push of same content should detect existing blob
        Layer secondLayer;
        try (InputStream inputStream = Files.newInputStream(testFile)) {
            secondLayer = registry.pushBlob(containerRef, inputStream);
        }

        // Verify both operations return same digest
        assertEquals(expectedDigest, firstLayer.getDigest());
        assertEquals(expectedDigest, secondLayer.getDigest());
        assertEquals(firstLayer.getSize(), secondLayer.getSize());

        // Clean up
        Files.delete(testFile);
        registry.deleteBlob(containerRef.withDigest(firstLayer.getDigest()));
    }

    @Test
    void shouldHandleIOExceptionInStreamPush() throws IOException {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-stream".formatted(this.registry.getRegistry()));

        // Create a failing input stream
        InputStream failingStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Simulated IO failure");
            }
        };

        // Verify exception is wrapped in OrasException
        OrasException exception =
                assertThrows(OrasException.class, () -> registry.pushBlob(containerRef, failingStream));
        assertEquals("Failed to push blob", exception.getMessage());
        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    void shouldHandleNonExistentBlobInGetStreamSha256() {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-stream".formatted(this.registry.getRegistry()));

        // Try to get non-existent blob
        String nonExistentDigest = "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        // Verify it throws OrasException
        assertThrows(OrasException.class, () -> registry.getBlobStream(containerRef.withDigest(nonExistentDigest)));
    }

    @Test
    void shouldHandleNonExistentBlobInGetStreamSha512() {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-stream".formatted(this.registry.getRegistry()));

        // Try to get non-existent blob
        String nonExistentDigest =
                "sha512:0a50261ebd1a390fed2bf326f2673c145582a6342d523204973d0219337f81616a8069b012587cf5635f6925f1b56c360230c19b273500ee013e030601bf2425";

        // Verify it throws OrasException
        assertThrows(OrasException.class, () -> registry.getBlobStream(containerRef.withDigest(nonExistentDigest)));
    }

    @Test
    void shouldHandleLargeStreamContent() throws IOException {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-stream".formatted(this.registry.getRegistry()));

        // Create temp file with 5MB of random data
        Path largeFile = Files.createTempFile("large-test-", ".tmp");
        byte[] largeData = new byte[5 * 1024 * 1024];
        new Random().nextBytes(largeData);
        Files.write(largeFile, largeData);

        // Push large content
        Layer layer;
        try (InputStream inputStream = Files.newInputStream(largeFile)) {
            layer = registry.pushBlob(containerRef, inputStream);
        }

        // Verify content with stream
        try (InputStream resultStream = registry.getBlobStream(containerRef.withDigest(layer.getDigest()))) {
            byte[] result = resultStream.readAllBytes();
            Assertions.assertArrayEquals(largeData, result);
        }

        // Clean up
        Files.delete(largeFile);
        registry.deleteBlob(containerRef.withDigest(layer.getDigest()));
    }
}
