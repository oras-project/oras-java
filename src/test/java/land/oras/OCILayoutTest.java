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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import land.oras.utils.DigestUtils;
import land.oras.utils.JsonUtils;
import land.oras.utils.SupportedAlgorithm;
import land.oras.utils.ZotContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Execution(ExecutionMode.CONCURRENT)
public class OCILayoutTest {

    @TempDir
    private Path extractDir;

    @TempDir
    private Path newArtifactsDir;

    @TempDir
    private Path blobDir;

    @TempDir
    private Path layoutPath;

    @TempDir
    private Path layoutPathIndex;

    @Container
    private final ZotContainer registry = new ZotContainer().withStartupAttempts(3);

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
    void shouldPullFromOciLayout() throws IOException {
        LayoutRef layoutRef = LayoutRef.parse("src/test/resources/oci/artifact:latest");
        OCILayout ociLayout =
                OCILayout.Builder.builder().defaults(layoutRef.getFolder()).build();
        ociLayout.pullArtifact(layoutRef, extractDir, false);

        // Check file exists
        assertTrue(Files.exists(extractDir.resolve("hi.txt")));

        // Fetch the manifest
        byte[] blob = ociLayout.getBlob(layoutRef);
        Manifest manifest = Manifest.fromJson(new String(blob, StandardCharsets.UTF_8));
        assertEquals(1, manifest.getLayers().size());
        ociLayout.fetchBlob(layoutRef, extractDir.resolve("manifest.json"));

        // By digest
        LayoutRef layoutRefDigest = LayoutRef.parse(
                "src/test/resources/oci/artifact@sha256:98ea6e4f216f2fb4b69fff9b3a44842c38686ca685f3f55dc48c5d3fb1107be4");
        ociLayout.fetchBlob(layoutRefDigest, extractDir.resolve("new_hi.txt"));

        // Ensure file exists
        assertTrue(Files.exists(extractDir.resolve("manifest.json")));
        assertTrue(Files.exists(extractDir.resolve("new_hi.txt")));

        // Assert content
        assertEquals(
                Files.readString(
                        Path.of(
                                "src/test/resources/oci/artifact/blobs/sha256/98ea6e4f216f2fb4b69fff9b3a44842c38686ca685f3f55dc48c5d3fb1107be4")),
                Files.readString(extractDir.resolve("new_hi.txt")));
        assertEquals(
                Files.readString(
                        Path.of(
                                "src/test/resources/oci/artifact/blobs/sha256/cb1d49baba271af2c56d493d66dddb112ecf1c2c52f47e6f45f3617bb2155d34")),
                Files.readString(extractDir.resolve("manifest.json")));
    }

    @Test
    void shouldPushArtifact() throws IOException {
        LayoutRef layoutRef = LayoutRef.parse("%s:foo".formatted(newArtifactsDir.toString()));
        OCILayout ociLayout =
                OCILayout.Builder.builder().defaults(newArtifactsDir).build();
        // Not implemented
        assertThrows(OrasException.class, () -> {
            ociLayout.pushArtifact(layoutRef, LocalPath.of(Path.of("test")));
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

        // Cannot copy if directory doesn't exists
        assertThrows(
                OrasException.class,
                () -> {
                    OCILayout.Builder.builder()
                            .defaults(layoutPath.resolve("not exists"))
                            .build()
                            .copy(registry, containerRef);
                },
                "Directory not found");

        // Copy to oci layout
        ociLayout.copy(registry, containerRef, false);

        assertTrue(Files.exists(layoutPath.resolve("oci-layout")));

        OCILayout layoutFile = JsonUtils.fromJson(layoutPath.resolve("oci-layout"), OCILayout.class);
        assertEquals("1.0.0", layoutFile.getImageLayoutVersion());

        // Assert the empty config
        assertEquals(
                "{}",
                Files.readString(layoutPath
                        .resolve("blobs")
                        .resolve("sha256")
                        .resolve(SupportedAlgorithm.getDigest(Config.empty().getDigest()))));

        // Check index exists
        assertTrue(Files.exists(layoutPath.resolve("index.json")));
        Index index = JsonUtils.fromJson(layoutPath.resolve("index.json"), Index.class);
        assertEquals(2, index.getSchemaVersion());
        assertEquals(1, index.getManifests().size());
        assertEquals(Const.DEFAULT_INDEX_MEDIA_TYPE, index.getMediaType());
        assertEquals(
                manifest.getDescriptor().getSize(), index.getManifests().get(0).getSize());

        // Assert blobs and their content
        assertEquals(
                "artifact-oci-layout",
                Files.readString(layoutPath
                        .resolve("blobs")
                        .resolve("sha256")
                        .resolve(SupportedAlgorithm.getDigest(DigestUtils.digest("sha256", file1)))));
        assertFalse(
                Files.exists(layoutPath
                        .resolve("blobs")
                        .resolve("sha256")
                        .resolve(SupportedAlgorithm.getDigest(DigestUtils.digest("sha256", file2)))),
                "Expect not to copy attached artifact");
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

        assertTrue(Files.exists(layoutPath.resolve("oci-layout")));

        OCILayout layoutFile = JsonUtils.fromJson(layoutPath.resolve("oci-layout"), OCILayout.class);
        assertEquals("1.0.0", layoutFile.getImageLayoutVersion());

        // Assert the empty config
        assertEquals(
                "{}",
                Files.readString(layoutPath
                        .resolve("blobs")
                        .resolve("sha256")
                        .resolve(SupportedAlgorithm.getDigest(Config.empty().getDigest()))));

        // Check index exists
        assertTrue(Files.exists(layoutPath.resolve("index.json")));
        Index index = JsonUtils.fromJson(layoutPath.resolve("index.json"), Index.class);
        assertEquals(2, index.getSchemaVersion());
        assertEquals(1, index.getManifests().size());
        assertEquals(Const.DEFAULT_INDEX_MEDIA_TYPE, index.getMediaType());
        assertEquals(
                manifest.getDescriptor().getSize(), index.getManifests().get(0).getSize());

        // Assert blobs and their content
        assertEquals(
                "artifact-oci-layout",
                Files.readString(layoutPath
                        .resolve("blobs")
                        .resolve("sha256")
                        .resolve(SupportedAlgorithm.getDigest(DigestUtils.digest("sha256", file1)))));
        assertEquals(
                "linked-file",
                Files.readString(layoutPath
                        .resolve("blobs")
                        .resolve("sha256")
                        .resolve(SupportedAlgorithm.getDigest(DigestUtils.digest("sha256", file2)))));
        assertEquals(
                "linked-file2",
                Files.readString(layoutPath
                        .resolve("blobs")
                        .resolve("sha256")
                        .resolve(SupportedAlgorithm.getDigest(DigestUtils.digest("sha256", file3)))));
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
        String manifestDigest =
                SupportedAlgorithm.SHA256.digest(emptyManifest.toJson().getBytes(StandardCharsets.UTF_8));
        String configDigest =
                SupportedAlgorithm.SHA256.digest(Config.empty().toJson().getBytes(StandardCharsets.UTF_8));

        // Push config and manifest
        registry.pushConfig(containerRef.withDigest(configDigest), Config.empty());
        Manifest pushedManifest = registry.pushManifest(containerRef, emptyManifest);

        // Copy to oci layout
        ociLayout.copy(registry, containerRef);

        assertTrue(Files.exists(layoutPath.resolve("oci-layout")));

        OCILayout layoutFile = JsonUtils.fromJson(layoutPath.resolve("oci-layout"), OCILayout.class);
        assertEquals("1.0.0", layoutFile.getImageLayoutVersion());

        // Check index exists
        assertTrue(Files.exists(layoutPath.resolve("index.json")));
        JsonUtils.fromJson(layoutPath.resolve("index.json"), Index.class);

        // Check manifest exists
        assertTrue(Files.exists(layoutPath
                .resolve("blobs")
                .resolve("sha256")
                .resolve(SupportedAlgorithm.getDigest(
                        pushedManifest.getDescriptor().getDigest()))));

        // Ensure manifest serialized correctly (check sha256)
        String computedManifestDigest = DigestUtils.digest(
                "sha256",
                layoutPath
                        .resolve("blobs")
                        .resolve("sha256")
                        .resolve(SupportedAlgorithm.getDigest(
                                pushedManifest.getDescriptor().getDigest())));
        assertEquals(
                SupportedAlgorithm.getDigest(pushedManifest.getDescriptor().getDigest()),
                SupportedAlgorithm.getDigest(computedManifestDigest),
                "Manifest digest should match");

        // Ensure layer1 is copied
        assertTrue(Files.exists(layoutPath
                .resolve("blobs")
                .resolve("sha256")
                .resolve(SupportedAlgorithm.getDigest(layer1.getDigest()))));
        // Ensure layer2 is copied
        assertTrue(Files.exists(layoutPath
                .resolve("blobs")
                .resolve("sha256")
                .resolve(SupportedAlgorithm.getDigest(layer2.getDigest()))));

        // Copy to oci layout again
        ociLayout.copy(registry, containerRef);

        // Check manifest exists
        assertTrue(Files.exists(layoutPath
                .resolve("blobs")
                .resolve("sha256")
                .resolve(SupportedAlgorithm.getDigest(
                        pushedManifest.getDescriptor().getDigest()))));
    }

    @Test
    void testShouldCopyImageIntoOciLayoutWithIndex() {

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
        String configDigest =
                SupportedAlgorithm.SHA256.digest(Config.empty().toJson().getBytes(StandardCharsets.UTF_8));

        // Push config and manifest
        registry.pushConfig(containerRef.withDigest(configDigest), Config.empty());
        Manifest pushedManifest = registry.pushManifest(containerRef.withDigest(manifestDigest), emptyManifest);
        Index index = registry.pushIndex(containerRef, Index.fromManifests(List.of(pushedManifest.getDescriptor())));

        // Copy to oci layout
        ociLayout.copy(registry, containerRef);

        assertTrue(Files.exists(layoutPathIndex.resolve(Const.OCI_LAYOUT_FILE)), "Expect oci-layout file to exist");

        OCILayout layoutFile = JsonUtils.fromJson(layoutPathIndex.resolve("oci-layout"), OCILayout.class);
        assertEquals("1.0.0", layoutFile.getImageLayoutVersion());

        // Check index exists
        assertTrue(Files.exists(layoutPathIndex.resolve("index.json")));
        JsonUtils.fromJson(layoutPathIndex.resolve("index.json"), Index.class);
        assertEquals(2, index.getSchemaVersion());
        assertEquals(1, index.getManifests().size());
        assertEquals(Const.DEFAULT_INDEX_MEDIA_TYPE, index.getMediaType());

        // Check manifest exists
        assertTrue(Files.exists(layoutPathIndex
                .resolve("blobs")
                .resolve("sha256")
                .resolve(SupportedAlgorithm.getDigest(
                        pushedManifest.getDescriptor().getDigest()))));

        // Ensure manifest serialized correctly (check sha256)
        String computedManifestDigest = DigestUtils.digest(
                "sha256",
                layoutPathIndex
                        .resolve("blobs")
                        .resolve("sha256")
                        .resolve(SupportedAlgorithm.getDigest(
                                pushedManifest.getDescriptor().getDigest())));
        assertEquals(
                SupportedAlgorithm.getDigest(pushedManifest.getDescriptor().getDigest()),
                SupportedAlgorithm.getDigest(computedManifestDigest),
                "Manifest digest should match");

        // Ensure layer1 is copied
        assertTrue(Files.exists(layoutPathIndex
                .resolve("blobs")
                .resolve("sha256")
                .resolve(SupportedAlgorithm.getDigest(layer1.getDigest()))));
        // Ensure layer2 is copied
        assertTrue(Files.exists(layoutPathIndex
                .resolve("blobs")
                .resolve("sha256")
                .resolve(SupportedAlgorithm.getDigest(layer2.getDigest()))));
        // Ensure index is also copied as blob
        assertTrue(Files.exists(layoutPathIndex
                .resolve("blobs")
                .resolve("sha256")
                .resolve(SupportedAlgorithm.getDigest(index.getDescriptor().getDigest()))));

        // Copy to oci layout again
        ociLayout.copy(registry, containerRef);

        // Check manifest exists
        assertTrue(Files.exists(layoutPathIndex
                .resolve("blobs")
                .resolve("sha256")
                .resolve(SupportedAlgorithm.getDigest(
                        pushedManifest.getDescriptor().getDigest()))));
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

        assertTrue(Files.exists(layoutPath.resolve("oci-layout")));

        OCILayout layoutFile = JsonUtils.fromJson(layoutPath.resolve("oci-layout"), OCILayout.class);
        assertEquals("1.0.0", layoutFile.getImageLayoutVersion());

        // Assert the config
        assertEquals(
                "foobartest",
                Files.readString(layoutPath
                        .resolve("blobs")
                        .resolve("sha256")
                        .resolve(SupportedAlgorithm.getDigest(layer.getDigest()))));

        // Check index exists
        assertTrue(Files.exists(layoutPath.resolve("index.json")));
        Index index = JsonUtils.fromJson(layoutPath.resolve("index.json"), Index.class);
        assertEquals(2, index.getSchemaVersion());
        assertEquals(1, index.getManifests().size());
        assertEquals(Const.DEFAULT_INDEX_MEDIA_TYPE, index.getMediaType());
        assertEquals(
                manifest.getDescriptor().getSize(), index.getManifests().get(0).getSize());
    }
}
