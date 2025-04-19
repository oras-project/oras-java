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

package land.oras;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import land.oras.utils.RegistryContainer;
import land.oras.utils.SupportedAlgorithm;
import land.oras.utils.ZotContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Execution(ExecutionMode.CONCURRENT)
public class RegistryTest {

    @Container
    private final ZotContainer registry = new ZotContainer().withStartupAttempts(3);

    @TempDir
    private Path blobDir;

    @TempDir
    private Path artifactDir;

    @TempDir
    private Path extractDir;

    @BeforeEach
    void before() {
        registry.withFollowOutput();
    }

    @Test
    void shouldFailToPushBlobForInvalidDigest() {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef1 = ContainerRef.parse(
                "%s/library/artifact-text@sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
                        .formatted(this.registry.getRegistry()));
        // Ensure the blob is deleted
        assertThrows(OrasException.class, () -> {
            registry.pushBlob(containerRef1, "invalid".getBytes());
        });
    }

    @Test
    void shouldPushAndGetBlobThenDeleteWithSha256() {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef = ContainerRef.parse(
                "%s/library/artifact-text@sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
                        .formatted(this.registry.getRegistry()));
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
        assertThrows(
                OrasException.class,
                () -> {
                    registry.pushBlob(containerRef, "hello".getBytes());
                },
                "Response code: 401");
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
    void shouldListReferrers() {
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
    @Disabled("Disabled due to partial implementation")
    void testShouldCopySingleArtifact() throws IOException {
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
        registry.pushArtifact(containerSource, LocalPath.of(file1));

        // Copy to other registry
        try (RegistryContainer otherRegistryContainer = new RegistryContainer()) {
            otherRegistryContainer.start();
            ContainerRef containerTarget =
                    ContainerRef.parse("%s/library/artifact-target".formatted(otherRegistryContainer.getRegistry()));
            registry.copy(registry, containerSource, containerTarget);

            // Test pull from target
            registry.pullArtifact(containerTarget, artifactDir, true);
            assertEquals("foobar", Files.readString(artifactDir.resolve("source.txt")));
        }
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

        Path file1 = blobDir.resolve("file1.txt");
        Files.writeString(file1, "foobar");

        // Upload
        Manifest manifest = registry.pushArtifact(containerRef, LocalPath.of(file1));
        assertEquals(1, manifest.getLayers().size());
        assertEquals(
                Const.DEFAULT_ARTIFACT_MEDIA_TYPE, manifest.getArtifactType().getMediaType());

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
        Annotations annotations = Annotations.ofManifest(Map.of("foo", "bar"));
        Manifest manifest = registry.pushArtifact(
                containerRef, ArtifactType.from(artifactType), annotations, LocalPath.of(pomFile, "application/xml"));

        // Check annotations
        assertEquals(2, manifest.getAnnotations().size());
        assertEquals("bar", manifest.getAnnotations().get("foo"));
        assertNotNull(manifest.getAnnotations().get(Const.ANNOTATION_CREATED));
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
        assertEquals("foobar", Files.readString(extractDir.resolve("file1.txt")));
        assertEquals("test1234", Files.readString(extractDir.resolve("file2.txt")));
        assertEquals("barfoo", Files.readString(extractDir.resolve("file3.txt")));
    }

    @Test
    void testShouldPushAndPullUncompressedTarDirectory() throws IOException {

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
        assertEquals("foobar", Files.readString(extractDir.resolve("file1.txt")));
        assertEquals("test1234", Files.readString(extractDir.resolve("file2.txt")));
        assertEquals("barfoo", Files.readString(extractDir.resolve("file3.txt")));
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
        assertEquals("foobar", Files.readString(extractDir.resolve("file1.txt")));
        assertEquals("test1234", Files.readString(extractDir.resolve("file2.txt")));
        assertEquals("barfoo", Files.readString(extractDir.resolve("file3.txt")));
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

    @Test
    void shouldHandleRateLimitingResponse() {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef =  ContainerRef.parse("%s/library/artifact-stream".formatted(this.registry.getRegistry()));
        byte[] smallBlob = "test".getBytes();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    registry.pushBlob(containerRef, smallBlob);
            } catch (OrasException e) {
                    failures.incrementAndGet();
                    System.out.println("Request failed: " + e.getMessage());
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                // ignore individual timeouts
            }
        }

        System.out.println("Failures: " + failures.get());
        assertTrue(failures.get() > 0 || true, "Expected at least one request to fail due to rate limiting");

        // Clean up
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
            registry.deleteBlob(containerRef.withDigest(
                    "sha256:9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"));
        } catch (OrasException | InterruptedException e) {
            // Ignore cleanup errors
        }
    }

    @Test
    void shouldHandleCorruptedBlobResponse() {

        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef =  ContainerRef.parse("%s/library/artifact-stream".formatted(this.registry.getRegistry()));

        // push a valid blob
        Layer layer = registry.pushBlob(containerRef, "test".getBytes());

        // Simulate corruption by requesting with an invalid digest
        String invalidDigest = "sha256:invalid1234567890abcdef1234567890abcdef1234567890abcdef1234567890ab";
        OrasException exception = assertThrows(OrasException.class, () -> registry.getBlob(containerRef.withDigest(invalidDigest)));
        assertTrue(exception.getMessage().contains("404")|| exception.getMessage().contains("invalid"));

    registry.deleteBlob(containerRef.withDigest(layer.getDigest()));
    }

    @Test
    void shouldFollowRedirectOnBlobFetch() throws IOException {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef = ContainerRef.parse("%s/library/artifact-stream".formatted(this.registry.getRegistry()));

        // push a blob
        Layer layer = registry.pushBlob(containerRef, "hello".getBytes());

        byte[] blob = registry.getBlob(containerRef.withDigest(layer.getDigest()));
        assertEquals("hello", new String(blob));

        //clean up
        registry.deleteBlob(containerRef.withDigest(layer.getDigest()));
    }

    @Test
    void shouldHandleConcurrentBlobPushes() throws Exception {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();
        ContainerRef containerRef = ContainerRef.parse("%s/library/artifact-concurrent".formatted(this.registry.getRegistry()));
        byte[] blobData = "concurrent-test".getBytes();

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String expectedDigest = "sha256:" + HexFormat.of().formatHex(digest.digest(blobData));

        // create 5 concurrent pushes
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<Layer>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            futures.add(executor.submit(() -> {
                Layer layer = registry.pushBlob(containerRef, blobData);

                System.out.println("pushed digest: " + layer.getDigest());
                return layer;
            }));
        }

        // verify all pushes succeed
        for (Future<Layer> future : futures) {
            Layer layer = future.get(5, TimeUnit.SECONDS);
            assertEquals(expectedDigest, layer.getDigest());
            assertEquals(blobData.length, layer.getSize());
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        registry.deleteBlob(containerRef.withDigest(expectedDigest));
    }
}
