package land.oras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;

import land.oras.utils.Const;
import land.oras.utils.DigestUtils;
import land.oras.utils.JsonUtils;
import land.oras.utils.RegistryContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@WireMockTest
public class RegistryTest {

    private static final Logger LOG = LoggerFactory.getLogger(RegistryTest.class);

    @Container
    private final RegistryContainer registry = new RegistryContainer().withStartupAttempts(3);

    /**
     * Blob temporary dir
     */
    @TempDir
    private Path blobDir;

    @TempDir
    private Path artifactDir;

    @BeforeEach
    void before() {
        registry.withFollowOutput();
    }

    @Test
    void shouldListTags(WireMockRuntimeInfo wmRuntimeInfo) {

        // Return data from wiremock
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/v2/library/artifact-text/tags/list"))
                .willReturn(WireMock.okJson(JsonUtils.toJson(new Tags("artifact-text", List.of("latest", "0.1.1"))))));

        // Insecure registry
        Registry registry = Registry.Builder.builder().withInsecure(true).build();

        // Test
        List<String> tags = registry.getTags(ContainerRef.parse("%s/library/artifact-text"
                .formatted(wmRuntimeInfo.getHttpBaseUrl().replace("http://", ""))));

        // Assert
        assertEquals(2, tags.size());
        assertEquals("latest", tags.get(0));
        assertEquals("0.1.1", tags.get(1));
    }

    @Test
    void shouldPushAndGetBlobThenDelete() {
        Registry registry = Registry.Builder.builder()
                .withInsecure(true)
                .withSkipTlsVerify(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-text".formatted(this.registry.getRegistry()));
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
    void shouldUploadAndFetchBlobThenDelete() throws IOException {
        Registry registry = Registry.Builder.builder()
                .withInsecure(true)
                .withSkipTlsVerify(true)
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
                .withInsecure(true)
                .withSkipTlsVerify(true)
                .build();

        // Empty manifest
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/empty-manifest".formatted(this.registry.getRegistry()));
        Layer emptyLayer = registry.pushBlob(containerRef, Layer.empty().getDataBytes());
        Manifest emptyManifest = Manifest.empty().withLayers(List.of(Layer.fromDigest(emptyLayer.getDigest(), 2)));
        String location = registry.pushManifest(containerRef, emptyManifest);
        assertEquals(
                "http://%s/v2/library/empty-manifest/manifests/sha256:f570eb29564f04e73d15cc2a2bb4153d488b9e8428c7f5108b895baa379750bd"
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
    void testShouldPushAndPullMinimalArtifact() throws IOException {

        Registry registry = Registry.Builder.builder()
                .withInsecure(true)
                .withSkipTlsVerify(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-full".formatted(this.registry.getRegistry()));

        Path file1 = blobDir.resolve("file1.txt");
        Files.writeString(file1, "foobar");

        // Upload
        Manifest manifest = registry.pushArtifact(containerRef, file1);
        assertEquals(1, manifest.getLayers().size());

        Layer layer = manifest.getLayers().get(0);

        // A test file layer
        assertEquals(6, layer.getSize());
        assertEquals("text/plain", layer.getMediaType());
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
    void testShouldPushCompressedDirectory() throws IOException {

        Registry registry = Registry.Builder.builder()
                .withInsecure(true)
                .withSkipTlsVerify(true)
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
    }

    // Push blob - successfull
    // Push blob - failed - when blob already exists
    // Push blob - Handles io exception
    // Handle large stream content
    @Test
    void shouldPushAndGetBlobStream() throws IOException {
        Registry registry = Registry.Builder.builder()
                .withInsecure(true)
                .withSkipTlsVerify(true)
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
            assertEquals(DigestUtils.sha256(testFile), layer.getDigest());
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
                .withInsecure(true)
                .withSkipTlsVerify(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-stream".formatted(this.registry.getRegistry()));

        // Create test file
        Path testFile = Files.createTempFile("test-data-", ".tmp");
        Files.writeString(testFile, "Test Content");
        long fileSize = Files.size(testFile);
        String expectedDigest = DigestUtils.sha256(testFile);

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
                .withInsecure(true)
                .withSkipTlsVerify(true)
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
        OrasException exception = assertThrows(OrasException.class, () ->
                registry.pushBlobStream(containerRef, failingStream, 100)
        );
        assertEquals("Failed to push blob stream", exception.getMessage());
        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    void shouldHandleNonExistentBlobInGetStream() {
        Registry registry = Registry.Builder.builder()
                .withInsecure(true)
                .withSkipTlsVerify(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-stream".formatted(this.registry.getRegistry()));

        // Try to get non-existent blob
        String nonExistentDigest = "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        // Verify it throws OrasException
        assertThrows(OrasException.class, () ->
                registry.getBlobStream(containerRef.withDigest(nonExistentDigest))
        );
    }

    @Test
    void shouldHandleLargeStreamContent() throws IOException {
        Registry registry = Registry.Builder.builder()
                .withInsecure(true)
                .withSkipTlsVerify(true)
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
