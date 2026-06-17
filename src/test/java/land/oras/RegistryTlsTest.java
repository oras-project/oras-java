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
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLHandshakeException;
import land.oras.exception.OrasException;
import land.oras.utils.SupportedAlgorithm;
import land.oras.utils.TlsUtils;
import land.oras.utils.ZotTlsContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@Execution(ExecutionMode.CONCURRENT)
class RegistryTlsTest {

    private static final String UNRELATED_CA_PEM;

    static {
        KeyPair keyPair = TlsUtils.generateKeyPair();
        UNRELATED_CA_PEM = TlsUtils.toPem(TlsUtils.generateCaCertificate("unrelated-ca", keyPair));
    }

    @Container
    private final ZotTlsContainer tlsRegistry = new ZotTlsContainer().withStartupAttempts(3);

    @BeforeEach
    void before() {
        tlsRegistry.withFollowOutput();
    }

    @Test
    void shouldConnectWithCaFile() {
        Registry registry = Registry.builder()
                .withRegistry(tlsRegistry.getRegistry())
                .withCaFile(tlsRegistry.getCaCertPath())
                .build();

        List<String> repositories = registry.getRepositories().repositories();
        assertNotNull(repositories);
    }

    @Test
    void shouldConnectWithCaContent() {
        Registry registry = Registry.builder()
                .withRegistry(tlsRegistry.getRegistry())
                .withCaContent(tlsRegistry.getCaCertContent())
                .build();

        List<String> repositories = registry.getRepositories().repositories();
        assertNotNull(repositories);
    }

    @Test
    void shouldFailWithoutCaCertificate() {
        Registry registry =
                Registry.builder().withRegistry(tlsRegistry.getRegistry()).build();

        OrasException exception = assertThrows(OrasException.class, registry::getRepositories);
        assertInstanceOf(SSLHandshakeException.class, exception.getCause());
    }

    @Test
    void shouldFailWithWrongCaCertificate() {
        Registry registry = Registry.builder()
                .withRegistry(tlsRegistry.getRegistry())
                .withCaContent(UNRELATED_CA_PEM)
                .build();

        OrasException exception = assertThrows(OrasException.class, registry::getRepositories);
        assertInstanceOf(SSLHandshakeException.class, exception.getCause());
    }

    @Test
    void shouldConnectWithSkipTlsVerify() {
        Registry registry = Registry.builder()
                .withRegistry(tlsRegistry.getRegistry())
                .withSkipTlsVerify(true)
                .build();

        List<String> repositories = registry.getRepositories().repositories();
        assertNotNull(repositories);
    }

    @Test
    void shouldBeSecureAfterAsSecure() {
        // Start with a registry that skips TLS verification
        Registry skipVerifyRegistry = Registry.builder()
                .withRegistry(tlsRegistry.getRegistry())
                .withSkipTlsVerify(true)
                .build();
        assertNotNull(skipVerifyRegistry.getRepositories().repositories());

        // asSecure() resets both insecure and skipTlsVerify — now proper TLS is enforced
        Registry secureRegistry = skipVerifyRegistry.asSecure();
        OrasException e = assertThrows(OrasException.class, secureRegistry::getRepositories);
        assertInstanceOf(SSLHandshakeException.class, e.getCause());

        // Verify the same host works when the CA cert is provided
        Registry secureWithCa = Registry.builder()
                .withRegistry(tlsRegistry.getRegistry())
                .withCaContent(tlsRegistry.getCaCertContent())
                .build();
        assertNotNull(secureWithCa.getRepositories().repositories());
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void shouldDowngradeToSecureWhenConfigExplicitlyNotInsecure(@TempDir Path homeDir) throws Exception {
        // Build a registry as insecure (HTTP) but with the CA cert preserved so that
        // after asSecure() the TLS handshake can succeed.
        Registry insecureWithCa = Registry.builder()
                .withRegistry(tlsRegistry.getRegistry())
                .withInsecure(true)
                .withCaContent(tlsRegistry.getCaCertContent())
                .build();

        // Config entry with insecure=false makes the config authoritative over the parent's
        // insecure flag — ContainerRef.isInsecure() returns false, triggering the downgrade
        // guard which calls asSecure() and routes the request over HTTPS.
        // language=toml
        String registriesConf =
                """
                [[registry]]
                location = "%s"
                insecure = false
                """
                        .formatted(tlsRegistry.getRegistry());

        TestUtils.createRegistriesConfFile(homeDir, registriesConf);

        TestUtils.withHome(homeDir, () -> {
            // Rebuild inside withHome so RegistriesConf is loaded from the temp dir
            Registry registry = Registry.builder()
                    .withRegistry(tlsRegistry.getRegistry())
                    .withInsecure(true)
                    .withCaContent(tlsRegistry.getCaCertContent())
                    .build();
            List<String> repositories = registry.getRepositories().repositories();
            assertNotNull(repositories, "Should connect via HTTPS after downgrade from insecure registry");
        });
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void shouldUpgradeToSecureAllOperationsToSecureWhenConfigDefaultsToSecure(
            @TempDir Path homeDir, @TempDir Path pullDir, @TempDir Path blobDir) throws Exception {

        // Registry is set to secure by default
        // language=toml
        String registriesConf = """
                [[registry]]
                location = "%s"
                """
                .formatted(tlsRegistry.getRegistry());
        TestUtils.createRegistriesConfFile(homeDir, registriesConf);

        Path testFile = blobDir.resolve("downgrade1.txt");
        Files.writeString(testFile, "tls-downgrade-content");

        Path testFile2 = blobDir.resolve("downgrade2.txt");
        Files.writeString(testFile2, "other-content");

        TestUtils.withHome(homeDir, () -> {

            // We create a registry with insecure flag (Set CA certifcates to ensure they are kept during copy)
            Registry registry = Registry.builder()
                    .withRegistry(tlsRegistry.getRegistry())
                    .withInsecure(true)
                    .withCaContent(tlsRegistry.getCaCertContent())
                    .build();

            ContainerRef ref = ContainerRef.parse(tlsRegistry.getRegistry() + "/test/upgrade:v1");
            assertFalse(ref.isInsecure(registry), "Resolved ref should be secure");

            // Push artifact
            Manifest manifest = registry.pushArtifact(ref, LocalPath.of(testFile));
            assertNotNull(manifest);

            // Push index
            ContainerRef indexRef = ContainerRef.parse(tlsRegistry.getRegistry() + "/test/upgrade:v2");
            Index emptyIndex = Index.fromManifests(List.of());
            Index index = registry.pushIndex(indexRef, emptyIndex);
            assertNotNull(index);

            // Push blob
            byte[] streamBytes = "stream-blob".getBytes(StandardCharsets.UTF_8);
            String streamDigest = SupportedAlgorithm.SHA256.digest(streamBytes);
            ContainerRef streamRef = ref.withDigest(streamDigest);
            registry.pushBlob(streamRef, streamBytes.length, () -> new ByteArrayInputStream(streamBytes), Map.of());

            // Push chunked
            registry.pushBlobChunked(ref, testFile, 8 * 1024);

            // Get repositories
            assertFalse(registry.getRepositories().repositories().isEmpty());

            // Get manifest
            Manifest fetched = registry.getManifest(ref);
            assertNotNull(fetched);

            // Probe descriptor
            assertNotNull(registry.probeDescriptor(ref));

            // Tags
            Tags tags = registry.getTags(ref);
            assertEquals(2, tags.tags().size(), "Only one tag is present");
            assertEquals("v1", tags.tags().get(0), "Tag should be v1");
            assertEquals("v2", tags.tags().get(1), "Tag should be v2");

            // Exists
            assertTrue(registry.exists(ref), "Ref must exists");

            String layerDigest = manifest.getLayers().get(0).getDigest();
            assertNotNull(layerDigest, "Layer digest must not be null");
            ContainerRef blobRef = ref.withDigest(layerDigest);

            // Head blob
            assertNotNull(registry.fetchBlobDescriptor(blobRef));

            // Get blob
            assertNotNull(registry.getBlob(blobRef));

            // Fetch blob
            Path dest = blobDir.resolve("fetched.bin");
            registry.fetchBlob(blobRef, dest);
            assertTrue(Files.exists(dest));

            // Fetch blob stream
            try (InputStream is = registry.fetchBlob(blobRef)) {
                assertTrue(is.readAllBytes().length > 0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Get referrers
            assertNotNull(manifest.getDigest(), "Manifest index must not be null");
            Referrers referrers = registry.getReferrers(ref.withDigest(manifest.getDigest()), null);
            assertNotNull(referrers);

            // Pull artifact
            registry.pullArtifact(ref, pullDir, true);

            // Delete manifest
            registry.deleteManifest(ref);

            // Delete blob
            Layer layerToDelete = registry.pushBlob(ref.withTag("v3"), testFile2);
            assertNotNull(layerToDelete);
            assertNotNull(layerToDelete.getDigest(), "Layer digest must not be null");
            registry.deleteBlob(blobRef.withDigest(layerToDelete.getDigest()));
        });
    }
}
