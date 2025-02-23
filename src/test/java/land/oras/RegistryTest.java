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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import land.oras.utils.RegistryContainer;
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
public class RegistryTest {

    @Container
    private final RegistryContainer registry = new RegistryContainer().withStartupAttempts(3);

    /**
     * Blob temporary dir
     */
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
        Layer layer = registry.uploadBlob(containerRef, blobDir.resolve("temp.txt"));
        assertEquals("sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", layer.getDigest());

        registry.fetchBlob(
                containerRef.withDigest("sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"),
                blobDir.resolve("temp.txt"));

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
        String location = registry.pushManifest(containerRef, emptyManifest);
        assertEquals(
                "http://%s/v2/library/empty-manifest/manifests/sha256:8c9c89ba64282b316bf526d0ea9b803ed5a555e160d924d4830d7dc8e2df25f9"
                        .formatted(this.registry.getRegistry()),
                location);
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

        assertNull(manifest.getArtifactType());
        assertTrue(manifest.getAnnotations().isEmpty());

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
        registry.pushArtifact(containerSource, file1);

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
        Manifest manifest = registry.pushArtifact(containerRef, file1);
        assertEquals(1, manifest.getLayers().size());
        assertEquals(Const.DEFAULT_ARTIFACT_MEDIA_TYPE, manifest.getArtifactType());

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
    void testShouldPushAndPullCompressedDirectory() throws IOException {

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
        Manifest manifest = registry.pushArtifact(containerRef, blobDir);
        assertEquals(1, manifest.getLayers().size());

        Layer layer = manifest.getLayers().get(0);

        // A compressed directory file
        assertEquals(Const.DEFAULT_BLOB_DIR_MEDIA_TYPE, layer.getMediaType());
        Map<String, String> annotations = layer.getAnnotations();

        // Assert annotations of the layer
        assertEquals(2, annotations.size());
        assertEquals(blobDir.getFileName().toString(), annotations.get(Const.ANNOTATION_TITLE));
        assertEquals("true", annotations.get(Const.ANNOTATION_ORAS_UNPACK));

        // Pull
        registry.pullArtifact(containerRef, extractDir, true);

        // Assert extracted files
        assertEquals("foobar", Files.readString(extractDir.resolve("file1.txt")));
        assertEquals("test1234", Files.readString(extractDir.resolve("file2.txt")));
        assertEquals("barfoo", Files.readString(extractDir.resolve("file3.txt")));
    }

    @Test
    void shouldPushAndGetBlobStream() throws IOException {
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
            layer = registry.pushBlobStream(containerRef, inputStream, fileSize);

            // Verify the digest matches SHA-256 of content
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
            firstLayer = registry.pushBlobStream(containerRef, inputStream, fileSize);
        }

        // Second push of same content should detect existing blob
        Layer secondLayer;
        try (InputStream inputStream = Files.newInputStream(testFile)) {
            secondLayer = registry.pushBlobStream(containerRef, inputStream, fileSize);
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
                assertThrows(OrasException.class, () -> registry.pushBlobStream(containerRef, failingStream, 100));
        assertEquals("Failed to push blob stream", exception.getMessage());
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
        long fileSize = Files.size(largeFile);

        // Push large content
        Layer layer;
        try (InputStream inputStream = Files.newInputStream(largeFile)) {
            layer = registry.pushBlobStream(containerRef, inputStream, fileSize);
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
