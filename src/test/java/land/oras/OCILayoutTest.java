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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import land.oras.utils.SupportedAlgorithm;
import land.oras.utils.ZotContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Execution(ExecutionMode.CONCURRENT)
public class OCILayoutTest {

    private static final Logger LOG = LoggerFactory.getLogger(OCILayoutTest.class);

    @TempDir
    private Path extractDir;

    @TempDir
    private Path blobDir;

    @TempDir
    private Path layoutPath;

    @Container
    private final ZotContainer registry = new ZotContainer().withStartupAttempts(3);

    @Test
    void shouldPushEmptyManifest() {
        Path path = layoutPath.resolve("shouldPushManifest");
        LayoutRef layoutRef = LayoutRef.parse("%s".formatted(path.toString()));
        OCILayout ociLayout = OCILayout.Builder.builder().defaults(path).build();
        Manifest manifest = Manifest.empty().withConfig(Config.empty());
        manifest = ociLayout.pushManifest(layoutRef, manifest);

        // Assertion
        assertOciLayout(path);
        assertIndex(path, manifest);
        assertBlobExists(path, manifest.getDescriptor().getDigest());
        assertEquals(425, manifest.getDescriptor().getSize());

        // One element in the index
        Index index = JsonUtils.fromJson(path.resolve(Const.OCI_LAYOUT_INDEX), Index.class);
        assertEquals(1, index.getManifests().size());

        // Ensure one layer for compatibility
        assertEquals(1, manifest.getLayers().size(), "Should have at least one layer");
        assertLayerExists(path, manifest.getLayers().get(0));
        assertEquals("e30=", manifest.getLayers().get(0).getData());

        // Copy again
        manifest = ociLayout.pushManifest(layoutRef, manifest);
        assertEquals(425, manifest.getDescriptor().getSize());

        // Same manifest
        index = JsonUtils.fromJson(path.resolve(Const.OCI_LAYOUT_INDEX), Index.class);
        assertEquals(1, index.getManifests().size());

        // Add an other manifest with different digest
        Manifest manifest2 = Manifest.empty().withConfig(Config.empty()).withAnnotations(Map.of("foo", "bar"));
        ociLayout.pushManifest(layoutRef, manifest2);

        // Two elements in the index
        index = JsonUtils.fromJson(path.resolve(Const.OCI_LAYOUT_INDEX), Index.class);
        assertEquals(2, index.getManifests().size());

        // None have any annotations
        index.getManifests().forEach(m -> assertNull(m.getAnnotations()));
    }

    @Test
    void shouldPushManifestFromFile() {

        Path path = layoutPath.resolve("shouldPushManifetFromFile");
        LayoutRef layoutRef = LayoutRef.parse("%s".formatted(path.toString()));
        OCILayout ociLayout = OCILayout.Builder.builder().defaults(path).build();

        Manifest manifest = Manifest.fromPath(
                Path.of(
                        "src/test/resources/oci/artifact/blobs/sha256/cb1d49baba271af2c56d493d66dddb112ecf1c2c52f47e6f45f3617bb2155d34"));

        manifest = ociLayout.pushManifest(layoutRef, manifest);

        // Assertion
        assertOciLayout(path);
        assertIndex(path, manifest);
        assertBlobExists(path, manifest.getDescriptor().getDigest());
        assertEquals(556, manifest.getDescriptor().getSize());

        // One element in the index
        Index index = JsonUtils.fromJson(path.resolve(Const.OCI_LAYOUT_INDEX), Index.class);
        assertEquals(1, index.getManifests().size());

        // Assert the manifest
        assertNull(manifest.getLayers().get(0).getData());

        // Copy again
        manifest = ociLayout.pushManifest(layoutRef, manifest);
        assertEquals(556, manifest.getDescriptor().getSize());

        // Same manifest
        index = JsonUtils.fromJson(path.resolve(Const.OCI_LAYOUT_INDEX), Index.class);
        assertEquals(1, index.getManifests().size());

        // Two elements in the index
        index = JsonUtils.fromJson(path.resolve(Const.OCI_LAYOUT_INDEX), Index.class);
        assertEquals(1, index.getManifests().size());
    }

    @Test
    void shouldPushEmptyManifestWithRef() {
        Path path = layoutPath.resolve("shouldPushManifest");
        LayoutRef layoutRef = LayoutRef.parse("%s:latest".formatted(path.toString()));
        OCILayout ociLayout = OCILayout.Builder.builder().defaults(path).build();
        Manifest manifest = Manifest.empty().withConfig(Config.empty());
        manifest = ociLayout.pushManifest(layoutRef, manifest);

        // Assertion
        assertOciLayout(path);
        assertIndex(path, manifest);
        assertBlobExists(path, manifest.getDescriptor().getDigest());
        assertEquals(425, manifest.getDescriptor().getSize());

        // One element in the index
        Index index = JsonUtils.fromJson(path.resolve(Const.OCI_LAYOUT_INDEX), Index.class);
        assertEquals(1, index.getManifests().size());

        // Check latest tag
        assertEquals("latest", index.getManifests().get(0).getAnnotations().get(Const.ANNOTATION_REF));

        // Ensure one layer for compatibility
        assertEquals(1, manifest.getLayers().size(), "Should have at least one layer");
        assertLayerExists(path, manifest.getLayers().get(0));
        assertEquals("e30=", manifest.getLayers().get(0).getData());

        // Copy again
        manifest = ociLayout.pushManifest(layoutRef, manifest);
        assertEquals(425, manifest.getDescriptor().getSize());

        index = JsonUtils.fromJson(path.resolve(Const.OCI_LAYOUT_INDEX), Index.class);
        assertEquals(1, index.getManifests().size());

        // Add an other manifest with different digest
        Manifest manifest2 = Manifest.empty().withConfig(Config.empty()).withAnnotations(Map.of("foo", "bar"));
        ociLayout.pushManifest(layoutRef, manifest2);

        // Two elements in the index
        index = JsonUtils.fromJson(path.resolve(Const.OCI_LAYOUT_INDEX), Index.class);
        assertEquals(2, index.getManifests().size());

        // Ensure manifest1 doesn't have any annotations
        assertNull(index.getManifests().get(0).getAnnotations());

        // Ref was moved to manifest2
        assertEquals("latest", index.getManifests().get(1).getAnnotations().get(Const.ANNOTATION_REF));
    }

    @Test
    void shouldEnforceTagWhenPullArtifact() throws IOException {
        LayoutRef layoutRef = LayoutRef.parse("src/test/resources/oci/artifact");
        OCILayout ociLayout =
                OCILayout.Builder.builder().defaults(layoutRef.getFolder()).build();
        assertThrows(OrasException.class, () -> {
            ociLayout.pullArtifact(layoutRef, extractDir, false);
        });
    }

    @Test
    void failToCreateLayoutIfFileExists() throws IOException {
        Path path = layoutPath.resolve("failToCreateLayoutIfFileExists");
        Files.createFile(path);
        assertThrows(OrasException.class, () -> {
            LayoutRef layoutRef = LayoutRef.parse("%s".formatted(path.toString()));
            OCILayout.Builder.builder().defaults(layoutRef.getFolder()).build();
        });
    }

    @Test
    void shouldPushToOciLayoutWihoutTag() throws IOException {

        Path ociLayoutPath = layoutPath.resolve("shouldPushToOciLayoutWihoutTag");
        Path artifactPath = blobDir.resolve("shouldPushToOciLayoutWihoutTag.txt");
        Files.writeString(artifactPath, "hi");

        LayoutRef layoutRef = LayoutRef.parse("%s".formatted(ociLayoutPath.toString()));
        OCILayout ociLayout =
                OCILayout.Builder.builder().defaults(ociLayoutPath).build();

        Manifest manifest = ociLayout.pushArtifact(layoutRef, LocalPath.of(artifactPath, "text/plain"));

        assertOciLayout(ociLayoutPath);

        // Assert the empty config
        assertBlobContent(ociLayoutPath, Config.empty().getDigest(), "{}");

        // Check index exists
        assertIndex(ociLayoutPath, manifest);

        // Assert blobs and their content
        assertBlobExists(ociLayoutPath, SupportedAlgorithm.SHA256.digest(artifactPath));
        assertBlobContent(ociLayoutPath, SupportedAlgorithm.SHA256.digest(artifactPath), "hi");

        // Push again
        Manifest manifest1 = ociLayout.pushArtifact(layoutRef, LocalPath.of(artifactPath, "text/plain"));

        // Check index exists
        assertIndex(ociLayoutPath, manifest1);

        Index index = JsonUtils.fromJson(ociLayoutPath.resolve(Const.OCI_LAYOUT_INDEX), Index.class);

        // No annotation
        assertNull(index.getManifests().get(0).getAnnotations(), "Some annotations should be null");
    }

    @Test
    void shouldPushToOciLayoutWithTag() throws IOException {

        Path ociLayoutPath = layoutPath.resolve("shouldPushToOciLayoutWithTag");
        Path artifactPath = blobDir.resolve("shouldPushToOciLayoutWithTag.txt");
        Files.writeString(artifactPath, "hi");

        LayoutRef layoutRef = LayoutRef.parse("%s:latest".formatted(ociLayoutPath.toString()));
        OCILayout ociLayout =
                OCILayout.Builder.builder().defaults(ociLayoutPath).build();

        Manifest manifest = ociLayout.pushArtifact(layoutRef, LocalPath.of(artifactPath, "text/plain"));

        assertOciLayout(ociLayoutPath);

        // Assert the empty config
        assertBlobContent(ociLayoutPath, Config.empty().getDigest(), "{}");

        // Check index exists
        assertIndex(ociLayoutPath, manifest);

        // Assert blobs and their content
        assertBlobExists(ociLayoutPath, SupportedAlgorithm.SHA256.digest(artifactPath));
        assertBlobContent(ociLayoutPath, SupportedAlgorithm.SHA256.digest(artifactPath), "hi");

        // Push again
        Manifest manifest1 = ociLayout.pushArtifact(layoutRef, LocalPath.of(artifactPath, "text/plain"));

        // Check index exists
        assertIndex(ociLayoutPath, manifest1);

        Index index = JsonUtils.fromJson(ociLayoutPath.resolve(Const.OCI_LAYOUT_INDEX), Index.class);

        // No annotation
        assertNotNull(index.getManifests().get(0).getAnnotations(), "Some annotations should not be null");
        assertEquals(1, index.getManifests().get(0).getAnnotations().size());
    }

    @Test
    void shouldPullViaTagFromOciLayout() throws IOException {

        Path extractDir1 = extractDir.resolve("shouldPullViaTagFromOciLayout");
        Files.createDirectory(extractDir1);

        LayoutRef layoutRef = LayoutRef.parse("src/test/resources/oci/artifact:latest");
        OCILayout ociLayout =
                OCILayout.Builder.builder().defaults(layoutRef.getFolder()).build();
        ociLayout.pullArtifact(layoutRef, extractDir1, false);

        // Check file exists
        assertTrue(Files.exists(extractDir1.resolve("hi.txt")));

        // Fetch the manifest
        byte[] blob = ociLayout.getBlob(layoutRef);
        Manifest manifest = Manifest.fromJson(new String(blob, StandardCharsets.UTF_8));
        assertEquals(1, manifest.getLayers().size());
        ociLayout.fetchBlob(layoutRef, extractDir1.resolve("manifest.json"));

        // By digest
        LayoutRef layoutRefDigest = LayoutRef.parse(
                "src/test/resources/oci/artifact@sha256:98ea6e4f216f2fb4b69fff9b3a44842c38686ca685f3f55dc48c5d3fb1107be4");
        ociLayout.fetchBlob(layoutRefDigest, extractDir1.resolve("new_hi.txt"));

        // Ensure file exists
        assertTrue(Files.exists(extractDir1.resolve("manifest.json")));
        assertTrue(Files.exists(extractDir1.resolve("new_hi.txt")));

        // Assert content
        assertEquals(
                Files.readString(
                        Path.of(
                                "src/test/resources/oci/artifact/blobs/sha256/98ea6e4f216f2fb4b69fff9b3a44842c38686ca685f3f55dc48c5d3fb1107be4")),
                Files.readString(extractDir1.resolve("new_hi.txt")));
        assertEquals(
                Files.readString(
                        Path.of(
                                "src/test/resources/oci/artifact/blobs/sha256/cb1d49baba271af2c56d493d66dddb112ecf1c2c52f47e6f45f3617bb2155d34")),
                Files.readString(extractDir1.resolve("manifest.json")));
    }

    @Test
    void shouldPullViaDigestFromOciLayout() throws IOException {

        Path extractDir1 = extractDir.resolve("shouldPullViaDigestFromOciLayout");
        Files.createDirectory(extractDir1);

        LayoutRef layoutRef = LayoutRef.parse(
                "src/test/resources/oci/artifact@sha256:cb1d49baba271af2c56d493d66dddb112ecf1c2c52f47e6f45f3617bb2155d34");
        OCILayout ociLayout =
                OCILayout.Builder.builder().defaults(layoutRef.getFolder()).build();
        ociLayout.pullArtifact(layoutRef, extractDir1, false);

        // Check file exists
        assertTrue(Files.exists(extractDir1.resolve("hi.txt")));
    }

    @Test
    void shouldPushBlob() throws IOException {

        Path path = layoutPath.resolve("shouldPushBlob");

        byte[] content = "hi".getBytes(StandardCharsets.UTF_8);
        String digest = SupportedAlgorithm.SHA256.digest(content);

        LayoutRef layoutRef = LayoutRef.parse("%s@%s".formatted(path.toString(), digest));
        OCILayout ociLayout = OCILayout.Builder.builder().defaults(path).build();

        // Push more blobs
        ociLayout.pushBlob(layoutRef, "hi".getBytes(StandardCharsets.UTF_8));

        // Assert file exists
        assertBlobExists(path, digest);
        assertBlobContent(path, digest, "hi");

        // Push again
        ociLayout.pushBlob(layoutRef, "hi".getBytes(StandardCharsets.UTF_8));

        assertBlobExists(path, digest);
        assertBlobContent(path, digest, "hi");
    }

    @Test
    void cannotPushBlobWithoutTagOrDigest() throws IOException {

        Path invalidBlobPushDir = layoutPath.resolve("shouldPushArtifact");

        LayoutRef noTagLayout = LayoutRef.parse("%s".formatted(invalidBlobPushDir.toString()));
        LayoutRef noDigestLayout = LayoutRef.parse("%s:latest".formatted(invalidBlobPushDir.toString()));
        OCILayout ociLayout =
                OCILayout.Builder.builder().defaults(invalidBlobPushDir).build();

        // Push more blobs
        assertThrows(OrasException.class, () -> {
            ociLayout.pushBlob(noTagLayout, "hi".getBytes(StandardCharsets.UTF_8));
        });
        assertThrows(OrasException.class, () -> {
            ociLayout.pushBlob(noDigestLayout, "hi".getBytes(StandardCharsets.UTF_8));
        });
    }

    @Test
    void cannotPushWithInvalidDigest() {
        Path invalidBlobPushDir = layoutPath.resolve("cannotPushWithInvalidDigest");

        LayoutRef wrongDigest1 = LayoutRef.parse("%s@sha234:1234".formatted(invalidBlobPushDir.toString()));
        OCILayout ociLayout =
                OCILayout.Builder.builder().defaults(invalidBlobPushDir).build();

        // Push more blobs
        assertThrows(OrasException.class, () -> {
            ociLayout.pushBlob(wrongDigest1, "hi".getBytes(StandardCharsets.UTF_8));
        });

        LayoutRef wrongDigest2 = LayoutRef.parse("%s@sha256:1234".formatted(invalidBlobPushDir.toString()));

        // Push more blobs
        assertThrows(OrasException.class, () -> {
            ociLayout.pushBlob(wrongDigest2, "hi".getBytes(StandardCharsets.UTF_8));
        });
    }

    @Test
    void testShouldCopyArtifactFromRegistryIntoOciLayout() throws IOException {

        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();

        OCILayout ociLayout = OCILayout.Builder.builder().defaults(layoutPath).build();

        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-oci-layout".formatted(this.registry.getRegistry()));
        Path file1 = blobDir.resolve("artifact-oci-layout.txt");
        Path file2 = blobDir.resolve("artifact-recursive-oci-attached.txt");
        Files.writeString(file1, "artifact-oci-layout");
        Files.writeString(file2, "reference");

        // Push
        Manifest manifest = registry.pushArtifact(containerRef, LocalPath.of(file1));
        registry.attachArtifact(containerRef, ArtifactType.from("application/foo"), LocalPath.of(file2));

        // Copy to oci layout
        ociLayout.copy(registry, containerRef, false);

        assertOciLayout(layoutPath);

        // Assert the empty config
        assertBlobContent(layoutPath, Config.empty().getDigest(), "{}");

        // Check index exists
        assertIndex(layoutPath, manifest);

        // Assert blobs and their content
        assertBlobExists(layoutPath, SupportedAlgorithm.SHA256.digest(file1));
        assertBlobContent(layoutPath, SupportedAlgorithm.SHA256.digest(file1), "artifact-oci-layout");

        // Blob is absent
        assertBlobAbsent(layoutPath, SupportedAlgorithm.SHA256.digest(file2));
    }

    @Test
    void testShouldCopyRecursivelyArtifactFromRegistryIntoOciLayout() throws IOException {

        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();

        OCILayout ociLayout = OCILayout.Builder.builder().defaults(layoutPath).build();

        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-recursive-oci-layout".formatted(this.registry.getRegistry()));
        Path file1 = blobDir.resolve("artifact-recursive-oci-layout.txt");
        Path file2 = blobDir.resolve("artifact-recursive-oci-attached.txt");
        Path file3 = blobDir.resolve("artifact-recursive-oci-attached2.txt");

        Files.writeString(file1, "artifact-oci-layout");
        Files.writeString(file2, "linked-file");
        Files.writeString(file3, "linked-file2");

        // Push
        Manifest manifest = registry.pushArtifact(containerRef, LocalPath.of(file1));
        Manifest attached =
                registry.attachArtifact(containerRef, ArtifactType.from("application/foo"), LocalPath.of(file2));
        registry.attachArtifact(
                containerRef.withDigest(attached.getDescriptor().getDigest()),
                ArtifactType.from("application/bar"),
                LocalPath.of(file3));

        // Copy to oci layout
        ociLayout.copy(registry, containerRef, true);

        assertOciLayout(layoutPath);

        // Assert the empty config
        assertBlobExists(layoutPath, Config.empty().getDigest());
        assertBlobContent(layoutPath, Config.empty().getDigest(), "{}");

        // Check index exists
        assertIndex(layoutPath, manifest);

        // Assert blobs and their content
        assertBlobExists(layoutPath, SupportedAlgorithm.SHA256.digest(file1));
        assertBlobContent(layoutPath, SupportedAlgorithm.SHA256.digest(file1), "artifact-oci-layout");
        assertBlobExists(layoutPath, SupportedAlgorithm.SHA256.digest(file2));
        assertBlobContent(layoutPath, SupportedAlgorithm.SHA256.digest(file2), "linked-file");
        assertBlobExists(layoutPath, SupportedAlgorithm.SHA256.digest(file3));
        assertBlobContent(layoutPath, SupportedAlgorithm.SHA256.digest(file3), "linked-file2");
    }

    @Test
    void testShouldCopyImageIntoOciLayoutWithoutIndex() {

        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();

        OCILayout ociLayout = OCILayout.Builder.builder().defaults(layoutPath).build();

        ContainerRef containerRef =
                ContainerRef.parse("%s/library/image-no-index".formatted(this.registry.getRegistry()));

        Layer layer1 = registry.pushBlob(containerRef, Layer.empty().getDataBytes());
        Layer layer2 = registry.pushBlob(containerRef, "foobar".getBytes());

        Manifest emptyManifest = Manifest.empty()
                .withLayers(List.of(Layer.fromDigest(layer1.getDigest(), 2), Layer.fromDigest(layer2.getDigest(), 6)));
        String configDigest = Config.empty().getDigest();

        // Push config and manifest
        registry.pushConfig(containerRef.withDigest(configDigest), Config.empty());
        Manifest pushedManifest = registry.pushManifest(containerRef, emptyManifest);

        // Copy to oci layout
        ociLayout.copy(registry, containerRef);

        assertOciLayout(layoutPath);

        // Check index exists
        assertIndex(layoutPath, pushedManifest);

        // Check manifest exists
        assertTrue(Files.exists(layoutPath
                .resolve("blobs")
                .resolve("sha256")
                .resolve(SupportedAlgorithm.getDigest(
                        pushedManifest.getDescriptor().getDigest()))));

        // Ensure manifest serialized correctly (check sha256)
        String computedManifestDigest = SupportedAlgorithm.SHA256.digest(layoutPath
                .resolve("blobs")
                .resolve("sha256")
                .resolve(SupportedAlgorithm.getDigest(
                        pushedManifest.getDescriptor().getDigest())));
        assertEquals(
                SupportedAlgorithm.getDigest(pushedManifest.getDescriptor().getDigest()),
                SupportedAlgorithm.getDigest(computedManifestDigest),
                "Manifest digest should match");

        // Asser layers
        assertLayerExists(layoutPath, layer1);
        assertLayerExists(layoutPath, layer2);

        // Copy to oci layout again
        ociLayout.copy(registry, containerRef);

        // Check manifest exists
        assertBlobExists(layoutPath, pushedManifest.getDescriptor().getDigest());
    }

    @Test
    void testShouldCopyImageIntoOciLayoutWithIndex() throws IOException {

        Path layoutPathIndex = layoutPath.resolve("testShouldCopyImageIntoOciLayoutWithIndex");

        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();

        OCILayout ociLayout =
                OCILayout.Builder.builder().defaults(layoutPathIndex).build();

        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-image-pull".formatted(this.registry.getRegistry()));

        Layer layer1 = registry.pushBlob(containerRef, Layer.empty().getDataBytes());
        Layer layer2 = registry.pushBlob(containerRef, "foobar".getBytes());

        Manifest emptyManifest = Manifest.empty()
                .withLayers(List.of(Layer.fromDigest(layer1.getDigest(), 2), Layer.fromDigest(layer2.getDigest(), 6)));
        String manifestDigest =
                SupportedAlgorithm.SHA256.digest(emptyManifest.toJson().getBytes(StandardCharsets.UTF_8));
        String configDigest = Config.empty().getDigest();

        // Push config and manifest
        registry.pushConfig(containerRef.withDigest(configDigest), Config.empty());
        Manifest pushedManifest = registry.pushManifest(containerRef.withDigest(manifestDigest), emptyManifest);
        Index index = registry.pushIndex(containerRef, Index.fromManifests(List.of(pushedManifest.getDescriptor())));

        // Copy to oci layout
        ociLayout.copy(registry, containerRef);

        assertOciLayout(layoutPathIndex);

        // Check index exists
        assertIndex(layoutPathIndex, pushedManifest);

        // Check manifest exists
        assertBlobExists(layoutPathIndex, pushedManifest.getDescriptor().getDigest());

        // Ensure manifest serialized correctly (check sha256)
        String computedManifestDigest = SupportedAlgorithm.SHA256.digest(layoutPathIndex
                .resolve("blobs")
                .resolve("sha256")
                .resolve(SupportedAlgorithm.getDigest(
                        pushedManifest.getDescriptor().getDigest())));

        assertEquals(
                SupportedAlgorithm.getDigest(pushedManifest.getDescriptor().getDigest()),
                SupportedAlgorithm.getDigest(computedManifestDigest),
                "Manifest digest should match");

        // Assert blobs
        assertLayerExists(layoutPathIndex, layer1);
        assertLayerExists(layoutPathIndex, layer1);
        assertBlobExists(layoutPathIndex, index.getDescriptor().getDigest());
        assertBlobExists(layoutPathIndex, pushedManifest.getDescriptor().getDigest());

        // Copy to oci layout again
        ociLayout.copy(registry, containerRef);

        // Check manifest exists
        assertLayerExists(layoutPathIndex, layer1);
        assertLayerExists(layoutPathIndex, layer1);
        assertBlobExists(layoutPathIndex, index.getDescriptor().getDigest());
        assertBlobExists(layoutPathIndex, pushedManifest.getDescriptor().getDigest());
    }

    @Test
    void testShouldCopyIntoOciLayoutWithBlobConfig() throws IOException {
        Registry registry = Registry.Builder.builder()
                .defaults("myuser", "mypass")
                .withInsecure(true)
                .build();

        OCILayout ociLayout = OCILayout.Builder.builder().defaults(layoutPath).build();

        ContainerRef containerRef =
                ContainerRef.parse("%s/library/artifact-oci-layout".formatted(this.registry.getRegistry()));
        Path file1 = blobDir.resolve("artifact-oci-layout.txt");
        Files.writeString(file1, "artifact-oci-layout");

        // Push
        Layer layer = registry.pushBlob(containerRef, "foobartest".getBytes(StandardCharsets.UTF_8));
        Config config = Config.fromBlob("text/plain", layer);
        Manifest manifest = registry.pushArtifact(
                containerRef, ArtifactType.from("my/artifact"), Annotations.empty(), config, LocalPath.of(file1));

        // Copy to oci layout
        ociLayout.copy(registry, containerRef);

        assertOciLayout(layoutPath);

        // Assert the config
        assertLayerExists(layoutPath, layer);
        assertBlobContent(layoutPath, layer.getDigest(), "foobartest");

        // Check index exists
        assertIndex(layoutPath, manifest);
    }

    private void assertOciLayout(Path layoutPath) {
        assertTrue(Files.exists(layoutPath.resolve(Const.OCI_LAYOUT_FILE)));
        OCILayout layoutFile = JsonUtils.fromJson(layoutPath.resolve(Const.OCI_LAYOUT_FILE), OCILayout.class);
        assertEquals("1.0.0", layoutFile.getImageLayoutVersion());
    }

    private void assertIndex(Path ociLayoutPath, Manifest manifest) {
        assertTrue(Files.exists(ociLayoutPath.resolve(Const.OCI_LAYOUT_INDEX)));
        Index index = JsonUtils.fromJson(ociLayoutPath.resolve(Const.OCI_LAYOUT_INDEX), Index.class);
        LOG.debug("Index is {}", index.toJson());
        assertEquals(2, index.getSchemaVersion());
        assertEquals(1, index.getManifests().size());
        assertEquals(Const.DEFAULT_INDEX_MEDIA_TYPE, index.getMediaType());
        assertEquals(
                manifest.getDescriptor().getSize(), index.getManifests().get(0).getSize());
    }

    private void assertLayerExists(Path ociLayoutPath, Layer layer) {
        if (layer.getData() == null) {
            assertTrue(
                    Files.exists(ociLayoutPath
                            .resolve("blobs")
                            .resolve("sha256")
                            .resolve(SupportedAlgorithm.getDigest(layer.getDigest()))),
                    "Expect layer to exist");
        }
    }

    private void assertBlobExists(Path ociLayoutPath, String digest) {
        assertTrue(
                Files.exists(ociLayoutPath
                        .resolve("blobs")
                        .resolve(SupportedAlgorithm.fromDigest(digest).getPrefix())
                        .resolve(SupportedAlgorithm.getDigest(digest))),
                "Expect blob to exist");
    }

    private void assertBlobAbsent(Path ociLayoutPath, String digest) {
        assertFalse(
                Files.exists(ociLayoutPath
                        .resolve("blobs")
                        .resolve(SupportedAlgorithm.fromDigest(digest).getPrefix())
                        .resolve(SupportedAlgorithm.getDigest(digest))),
                "Expect blob to be absent");
    }

    private void assertBlobContent(Path ociLayoutPath, String digest, String content) throws IOException {
        assertEquals(
                content,
                Files.readString(ociLayoutPath
                        .resolve("blobs")
                        .resolve(SupportedAlgorithm.fromDigest(digest).getPrefix())
                        .resolve(SupportedAlgorithm.getDigest(digest))),
                "Expect blob content to match");
    }
}
