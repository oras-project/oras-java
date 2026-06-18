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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import land.oras.utils.ZotUnsecureContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for [[registry.mirror]] support in registries.conf.
 * Uses one real mirror container and one unreachable mirror address to verify
 * that the SDK tries mirrors in order and falls back gracefully.
 */
@Testcontainers(disabledWithoutDocker = true)
@Execution(ExecutionMode.SAME_THREAD)
class RegistryMirrorTest {

    /** Working mirror — contains the test artifact. */
    @Container
    private final ZotUnsecureContainer mirrorUp = new ZotUnsecureContainer().withStartupAttempts(3);

    @TempDir
    private Path homeDir;

    @Test
    void shouldFetchManifestViaMirrorWhenOriginalIsDown(@TempDir Path blobDir) throws Exception {

        // Push a test artifact to the working mirror
        String mirrorRegistry = mirrorUp.getRegistry();
        Registry setupRegistry = Registry.builder().insecure(mirrorRegistry).build();
        Path testFile = createTestFile(blobDir, "mirror-test.txt", "mirror content");
        ContainerRef mirrorArtifact = ContainerRef.parse(mirrorRegistry + "/test/mirror-artifact:v1");
        setupRegistry.pushArtifact(mirrorArtifact, LocalPath.of(testFile));

        // Build registries.conf: "original" registry (down) with 2 mirrors —
        //   mirror 1: localhost:59999 (down, connection refused)
        //   mirror 2: the running mirrorUp container
        // language=toml
        String registriesConf =
                """
                [[registry]]
                prefix = "localhost:59998"
                location = "localhost:59998"

                [[registry.mirror]]
                location = "localhost:59999"
                insecure = true

                [[registry.mirror]]
                location = "%s"
                insecure = true
                """
                        .formatted(mirrorRegistry);

        TestUtils.createRegistriesConfFile(homeDir, registriesConf);

        TestUtils.withHome(homeDir, () -> {
            Registry registry = Registry.builder().insecure().defaults().build();
            ContainerRef ref = ContainerRef.parse("localhost:59998/test/mirror-artifact:v1");
            Manifest manifest = registry.getManifest(ref);
            assertNotNull(manifest, "Manifest should be fetched via the working mirror");
        });
    }

    @Test
    void shouldPullArtifactViaMirrorWhenOriginalIsDown(@TempDir Path blobDir, @TempDir Path pullDir) throws Exception {

        // Push a test artifact (with a file layer) to the working mirror
        String mirrorRegistry = mirrorUp.getRegistry();
        Registry setupRegistry = Registry.builder().insecure(mirrorRegistry).build();
        Path testFile = createTestFile(blobDir, "mirror-artifact.txt", "mirror artifact content");
        ContainerRef mirrorArtifact = ContainerRef.parse(mirrorRegistry + "/test/mirror-pull:v1");
        setupRegistry.pushArtifact(mirrorArtifact, LocalPath.of(testFile));

        // language=toml
        String registriesConf =
                """
                [[registry]]
                prefix = "localhost:59998"
                location = "localhost:59998"

                [[registry.mirror]]
                location = "localhost:59999"
                insecure = true

                [[registry.mirror]]
                location = "%s"
                insecure = true
                """
                        .formatted(mirrorRegistry);

        TestUtils.createRegistriesConfFile(homeDir, registriesConf);

        TestUtils.withHome(homeDir, () -> {
            Registry registry = Registry.builder().insecure().defaults().build();
            ContainerRef ref = ContainerRef.parse("localhost:59998/test/mirror-pull:v1");
            registry.pullArtifact(ref, pullDir, true);
            assertTrue(
                    Files.exists(pullDir.resolve("mirror-artifact.txt")),
                    "Artifact file should be pulled via the working mirror");
        });
    }

    @Test
    void shouldFetchManifestViaInsecurePathPrefixedMirror(@TempDir Path blobDir) throws Exception {

        // Push the artifact under a path-prefixed location in the mirror
        String mirrorRegistry = mirrorUp.getRegistry();
        Registry setupRegistry = Registry.builder().insecure(mirrorRegistry).build();
        Path testFile = createTestFile(blobDir, "mirror-prefix.txt", "path-prefix content");
        ContainerRef mirrorArtifact = ContainerRef.parse(mirrorRegistry + "/prefix/test/mirror-prefix:v1");
        setupRegistry.pushArtifact(mirrorArtifact, LocalPath.of(testFile));

        // Mirror location includes a path prefix → covers mirrorLocation.contains("/") branch.
        // language=toml
        String registriesConf =
                """
                [[registry]]
                prefix = "localhost:59998"
                location = "localhost:59998"

                [[registry.mirror]]
                location = "%s/prefix"
                insecure = true
                """
                        .formatted(mirrorRegistry);

        TestUtils.createRegistriesConfFile(homeDir, registriesConf);

        TestUtils.withHome(homeDir, () -> {
            // Build WITHOUT insecure() so this.isInsecure() = false.
            // mirror.isInsecure()=true && !isInsecure()=true → covers mirrorRegistry.asInsecure() branch.
            Registry registry = Registry.builder().defaults().build();
            ContainerRef ref = ContainerRef.parse("localhost:59998/test/mirror-prefix:v1");
            Manifest manifest = registry.getManifest(ref);
            assertNotNull(manifest, "Manifest should be fetched via path-prefixed insecure mirror");
        });
    }

    @Test
    void shouldFetchBlobViaMirrorWhenOriginalIsDown(@TempDir Path blobDir, @TempDir Path fetchDir) throws Exception {

        // Push a blob to the working mirror
        String mirrorRegistry = mirrorUp.getRegistry();
        Registry setupRegistry = Registry.builder().insecure(mirrorRegistry).build();
        byte[] blobContent = "blob via mirror".getBytes(StandardCharsets.UTF_8);
        ContainerRef mirrorRef = ContainerRef.parse(mirrorRegistry + "/test/mirror-blob:v1");
        Layer layer = setupRegistry.pushBlob(mirrorRef, blobContent);

        // language=toml
        String registriesConf =
                """
                [[registry]]
                prefix = "localhost:59998"
                location = "localhost:59998"

                [[registry.mirror]]
                location = "localhost:59999"
                insecure = true

                [[registry.mirror]]
                location = "%s"
                insecure = true
                """
                        .formatted(mirrorRegistry);

        TestUtils.createRegistriesConfFile(homeDir, registriesConf);

        ContainerRef originalRef =
                ContainerRef.parse("localhost:59998/test/mirror-blob:v1").withDigest(layer.getDigest());

        TestUtils.withHome(homeDir, () -> {
            Registry registry = Registry.builder().insecure().defaults().build();

            // getBlob
            byte[] fetched = registry.getBlob(originalRef);
            assertArrayEquals(blobContent, fetched, "getBlob should return blob content via mirror");

            // fetchBlob(path)
            Path dest = fetchDir.resolve("blob.bin");
            registry.fetchBlob(originalRef, dest);
            try {
                assertArrayEquals(
                        blobContent, Files.readAllBytes(dest), "fetchBlob(path) should write blob via mirror");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // fetchBlob() / getBlobStream
            try (InputStream is = registry.fetchBlob(originalRef)) {
                assertArrayEquals(blobContent, is.readAllBytes(), "fetchBlob() stream should return blob via mirror");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void shouldFetchManifestViaUnqualifiedReference(@TempDir Path blobDir) throws Exception {

        // Push to the mirror under the "library" namespace so it matches an unqualified pull
        String mirrorRegistry = mirrorUp.getRegistry();
        Registry setupRegistry = Registry.builder().insecure(mirrorRegistry).build();
        Path testFile = createTestFile(blobDir, "mirror-unqualified.txt", "unqualified mirror content");
        ContainerRef mirrorArtifact = ContainerRef.parse(mirrorRegistry + "/library/mirror-unqualified:v1");
        setupRegistry.pushArtifact(mirrorArtifact, LocalPath.of(testFile));

        // Configure docker.io with a mirror — unqualified refs resolve to docker.io by default
        // language=toml
        String registriesConf =
                """
                [[registry]]
                prefix = "docker.io"
                location = "docker.io"

                [[registry.mirror]]
                location = "%s"
                insecure = true
                """
                        .formatted(mirrorRegistry);

        TestUtils.createRegistriesConfFile(homeDir, registriesConf);

        TestUtils.withHome(homeDir, () -> {
            Registry registry = Registry.builder().defaults().build();
            // "library/mirror-unqualified:v1" is unqualified — registry defaults to docker.io,
            // so rewriteForMirror must use the component-based path (not toString() substring)
            ContainerRef ref = ContainerRef.parse("library/mirror-unqualified:v1");
            assertTrue(ref.isUnqualified());
            Manifest manifest = registry.getManifest(ref);
            assertNotNull(manifest, "Manifest should be fetched via mirror for unqualified reference");
        });
    }

    @Test
    void shouldSkipDigestOnlyMirrorWhenPullingByTag(@TempDir Path blobDir) throws Exception {

        // Push artifact to the working mirror
        String mirrorRegistry = mirrorUp.getRegistry();
        Registry setupRegistry = Registry.builder().insecure(mirrorRegistry).build();
        Path testFile = createTestFile(blobDir, "digest-only.txt", "digest-only mirror content");
        ContainerRef mirrorArtifact = ContainerRef.parse(mirrorRegistry + "/test/digest-only-mirror:v1");
        setupRegistry.pushArtifact(mirrorArtifact, LocalPath.of(testFile));

        // Mirror configured as digest-only — a tag-based pull must NOT use it
        // language=toml
        String registriesConf =
                """
                [[registry]]
                prefix = "localhost:59998"
                location = "localhost:59998"

                [[registry.mirror]]
                location = "%s"
                insecure = true
                pull-from-mirror = "digest-only"
                """
                        .formatted(mirrorRegistry);

        TestUtils.createRegistriesConfFile(homeDir, registriesConf);

        TestUtils.withHome(homeDir, () -> {
            Registry registry = Registry.builder().insecure().defaults().build();
            // Pull by tag: digest-only mirror is skipped; fallback to "original" (also down) → must fail
            ContainerRef ref = ContainerRef.parse("localhost:59998/test/digest-only-mirror:v1");
            assertThrows(
                    land.oras.exception.OrasException.class,
                    () -> registry.getManifest(ref),
                    "digest-only mirror must be skipped for a tag-based pull");
        });
    }

    @Test
    void shouldUseDigestOnlyMirrorWhenPullingByDigest(@TempDir Path blobDir) throws Exception {

        // Push artifact to the working mirror
        String mirrorRegistry = mirrorUp.getRegistry();
        Registry setupRegistry = Registry.builder().insecure(mirrorRegistry).build();
        Path testFile = createTestFile(blobDir, "digest-pull.txt", "digest mirror content");
        ContainerRef mirrorArtifact = ContainerRef.parse(mirrorRegistry + "/test/digest-pull-mirror:v1");
        setupRegistry.pushArtifact(mirrorArtifact, LocalPath.of(testFile));

        // Resolve the digest so we can pull by digest
        Manifest pushed = setupRegistry.getManifest(mirrorArtifact);
        assertNotNull(pushed);
        String digest = pushed.getDescriptor().getDigest();

        // language=toml
        String registriesConf =
                """
                [[registry]]
                prefix = "localhost:59998"
                location = "localhost:59998"

                [[registry.mirror]]
                location = "%s"
                insecure = true
                pull-from-mirror = "digest-only"
                """
                        .formatted(mirrorRegistry);

        TestUtils.createRegistriesConfFile(homeDir, registriesConf);

        TestUtils.withHome(homeDir, () -> {
            Registry registry = Registry.builder().insecure().defaults().build();
            // Pull by digest: digest-only mirror must be used
            ContainerRef ref = ContainerRef.parse("localhost:59998/test/digest-pull-mirror:v1")
                    .withDigest(digest);
            Manifest manifest = registry.getManifest(ref);
            assertNotNull(manifest, "digest-only mirror must be used for a digest-based pull");
        });
    }

    @Test
    void shouldApplyMirrorByDigestOnly(@TempDir Path blobDir) throws Exception {

        // Push artifact to the working mirror
        String mirrorRegistry = mirrorUp.getRegistry();
        Registry setupRegistry = Registry.builder().insecure(mirrorRegistry).build();
        Path testFile = createTestFile(blobDir, "mbd.txt", "mirror-by-digest-only content");
        ContainerRef mirrorArtifact = ContainerRef.parse(mirrorRegistry + "/test/mbd-mirror:v1");
        setupRegistry.pushArtifact(mirrorArtifact, LocalPath.of(testFile));

        Manifest pushed = setupRegistry.getManifest(mirrorArtifact);
        assertNotNull(pushed);
        String digest = pushed.getDescriptor().getDigest();

        // language=toml
        String registriesConf =
                """
                [[registry]]
                prefix = "localhost:59998"
                location = "localhost:59998"
                mirror-by-digest-only = true

                [[registry.mirror]]
                location = "%s"
                insecure = true
                """
                        .formatted(mirrorRegistry);

        TestUtils.createRegistriesConfFile(homeDir, registriesConf);

        TestUtils.withHome(homeDir, () -> {
            Registry registry = Registry.builder().insecure().defaults().build();

            // Tag pull → mirror-by-digest-only skips all mirrors → fails with original down
            ContainerRef tagRef = ContainerRef.parse("localhost:59998/test/mbd-mirror:v1");
            assertThrows(
                    land.oras.exception.OrasException.class,
                    () -> registry.getManifest(tagRef),
                    "mirror-by-digest-only must skip mirrors for tag-based pulls");

            // Digest pull → mirror is used
            ContainerRef digestRef =
                    ContainerRef.parse("localhost:59998/test/mbd-mirror:v1").withDigest(digest);
            Manifest manifest = registry.getManifest(digestRef);
            assertNotNull(manifest, "mirror-by-digest-only must allow mirrors for digest-based pulls");
        });
    }

    private Path createTestFile(Path dir, String name, String content) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, content);
        return file;
    }
}
