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

package land.oras.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import land.oras.ContainerRef;
import land.oras.Registry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class RegistryConfTest {

    @TempDir
    private static Path homeDir;

    @BeforeAll
    static void init() throws Exception {
        Files.createDirectory(homeDir.resolve(".config"));
        Files.createDirectory(homeDir.resolve(".config").resolve("containers"));
    }

    @Test
    void shouldCheckRegistryStatusWithLocationOnly() {

        Registry registry = Registry.builder().build();

        // With null
        RegistriesConf.RegistryConfig registryConfig =
                new RegistriesConf.RegistryConfig(null, "localhost:5000", null, null, null);
        assertEquals("localhost:5000", registryConfig.location());
        assertNull(registryConfig.blocked(), "Blocked should be null when not set");
        assertNull(registryConfig.insecure(), "Insecure should be null when not set");
        assertFalse(registryConfig.isBlocked(), "Registry should not be blocked when blocked is null");
        assertFalse(registryConfig.isInsecure(), "Registry should not be insecure when insecure is null");

        // Check some ref
        RegistriesConf conf = new RegistriesConf(new RegistriesConf.Config(List.of(registryConfig)));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5001/library/test:latest")));
        assertDoesNotThrow(conf::enforceShortNameMode);

        // With blocked true
        registryConfig = new RegistriesConf.RegistryConfig(null, "localhost:5000", true, null, null);
        assertTrue(registryConfig.isBlocked(), "Registry should be blocked when blocked is true");
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registryConfig)));
        assertTrue(conf.isBlocked(ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5001/library/test:latest")));
        assertDoesNotThrow(conf::enforceShortNameMode);

        // With insecure true
        registryConfig = new RegistriesConf.RegistryConfig(null, "localhost:5000", null, true, null);
        assertTrue(registryConfig.isInsecure(), "Registry should be insecure when insecure is true");
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registryConfig)));
        assertTrue(conf.isInsecure(registry, ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isInsecure(registry, ContainerRef.parse("localhost:5001/library/test:latest")));
        assertDoesNotThrow(conf::enforceShortNameMode);

        // With blocked false
        registryConfig = new RegistriesConf.RegistryConfig(null, "localhost:5000", false, null, null);
        assertFalse(registryConfig.isBlocked(), "Registry should not be blocked when blocked is false");
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registryConfig)));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5001/library/test:latest")));
        assertDoesNotThrow(conf::enforceShortNameMode);

        // With insecure false
        registryConfig = new RegistriesConf.RegistryConfig(null, "localhost:5000", null, false, null);
        assertFalse(registryConfig.isInsecure(), "Registry should be insecure when insecure is false");
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registryConfig)));
        assertFalse(conf.isInsecure(registry, ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isInsecure(registry, ContainerRef.parse("localhost:5001/library/test:latest")));
        assertDoesNotThrow(conf::enforceShortNameMode);
    }

    @Test
    void shouldCheckRegistryStatusWithPrefixExactMatch() {

        Registry registry = Registry.builder().build();

        // With null
        RegistriesConf.RegistryConfig registryConfig =
                new RegistriesConf.RegistryConfig("localhost:5000", null, null, null, null);
        assertEquals("localhost:5000", registryConfig.prefix());
        assertNull(registryConfig.blocked(), "Blocked should be null when not set");
        assertNull(registryConfig.insecure(), "Insecure should be null when not set");
        assertFalse(registryConfig.isBlocked(), "Registry should not be blocked when blocked is null");
        assertFalse(registryConfig.isInsecure(), "Registry should not be insecure when insecure is null");

        // Check some ref
        RegistriesConf conf = new RegistriesConf(new RegistriesConf.Config(List.of(registryConfig)));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5001/library/test:latest")));
        assertDoesNotThrow(conf::enforceShortNameMode);

        // With blocked true
        registryConfig = new RegistriesConf.RegistryConfig("localhost:5000", null, true, null, null);
        assertTrue(registryConfig.isBlocked(), "Registry should be blocked when blocked is true");
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registryConfig)));
        assertTrue(conf.isBlocked(ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5001/library/test:latest")));

        // With insecure true
        registryConfig = new RegistriesConf.RegistryConfig("localhost:5000", null, null, true, null);
        assertTrue(registryConfig.isInsecure(), "Registry should be insecure when insecure is true");
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registryConfig)));
        assertTrue(conf.isInsecure(registry, ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isInsecure(registry, ContainerRef.parse("localhost:5001/library/test:latest")));

        // With blocked false
        registryConfig = new RegistriesConf.RegistryConfig("localhost:5000", null, false, null, null);
        assertFalse(registryConfig.isBlocked(), "Registry should not be blocked when blocked is false");
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registryConfig)));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5001/library/test:latest")));

        // With insecure false
        registryConfig = new RegistriesConf.RegistryConfig("localhost:5000", null, null, false, null);
        assertFalse(registryConfig.isInsecure(), "Registry should be secure when insecure is false");
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registryConfig)));
        assertFalse(conf.isInsecure(registry, ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isInsecure(registry, ContainerRef.parse("localhost:5001/library/test:latest")));
    }

    @Test
    void checkWithHostNamePrefix() {
        RegistriesConf.RegistryConfig registry =
                new RegistriesConf.RegistryConfig("*.example.com", null, true, null, null);
        RegistriesConf conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));
        assertTrue(conf.isBlocked(ContainerRef.parse("registry.example.com/library/test:latest")));
        assertTrue(conf.isBlocked(ContainerRef.parse("foobar.example.com/library/test:latest")));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5000/library/test:latest")));
    }

    @Test
    void checkMultipleSettings() {

        Registry registry = Registry.builder().build();

        RegistriesConf.RegistryConfig registry1 =
                new RegistriesConf.RegistryConfig("*.internal.local", null, false, true, null);
        RegistriesConf.RegistryConfig registry2 =
                new RegistriesConf.RegistryConfig("*.internal.local/public", null, true, null, null);
        RegistriesConf conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry1, registry2)));
        assertTrue(conf.isInsecure(registry, ContainerRef.parse("registry.internal.local/library/test:latest")));
        assertFalse(conf.isInsecure(
                registry,
                ContainerRef.parse(
                        "registry.internal.local/public/test:latest"))); // Match second rule because longest match wins
        assertFalse(conf.isBlocked(ContainerRef.parse("registry.internal.local/private/test:latest")));
        assertTrue(conf.isBlocked(ContainerRef.parse("registry.internal.local/public/test:latest")));
    }

    @Test
    void shouldRewriteContainerRef() {
        // Just the domain
        RegistriesConf.RegistryConfig registry =
                new RegistriesConf.RegistryConfig("localhost:5000", "registry.example.com", null, null, null);
        RegistriesConf conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));
        ContainerRef originalRef = ContainerRef.parse("localhost:5000/library/test:latest");
        ContainerRef rewrittenRef = conf.rewrite(originalRef);
        assertEquals("registry.example.com/library/test:latest", rewrittenRef.toString());

        // With
        // prefix = "example.com/foo"
        // location = "internal-registry-for-example.com/bar"
        registry = new RegistriesConf.RegistryConfig(
                "example.com/foo", "internal-registry-for-example.com/bar", null, null, null);
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));
        originalRef = ContainerRef.parse("example.com/foo/library/test:latest");
        rewrittenRef = conf.rewrite(originalRef);
        assertEquals("internal-registry-for-example.com/bar/library/test:latest", rewrittenRef.toString());
    }

    @Test
    void shouldRewriteUnqualifiedContainerRef() {

        RegistriesConf.RegistryConfig registry =
                new RegistriesConf.RegistryConfig("docker.io", "internal-registry-for-example.com", null, null, null);
        RegistriesConf conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));

        // Without tag
        ContainerRef originalRef = ContainerRef.parse("alpine");
        ContainerRef rewrittenRef = conf.rewrite(originalRef);
        assertEquals("internal-registry-for-example.com/library/alpine:latest", rewrittenRef.toString());

        // Ensure to keep tag
        originalRef = ContainerRef.parse("alpine:1.0.0");
        rewrittenRef = conf.rewrite(originalRef);
        assertEquals("internal-registry-for-example.com/library/alpine:1.0.0", rewrittenRef.toString());
    }

    @Test
    void shouldParseMirrorsFromToml() {
        // language=toml
        String toml =
                """
                [[registry]]
                prefix = "docker.io"
                location = "docker.io"

                [[registry.mirror]]
                location = "mirror1.example.com"
                insecure = true

                [[registry.mirror]]
                location = "mirror2.example.com"
                insecure = false
                """;

        RegistriesConf conf = RegistriesConf.newConf(List.of(writeTempToml(toml)));

        ContainerRef ref = ContainerRef.parse("docker.io/library/alpine:latest");
        List<RegistriesConf.MirrorConfig> mirrors = conf.getMirrors(ref);

        assertEquals(2, mirrors.size());
        assertEquals("mirror1.example.com", mirrors.get(0).location());
        assertTrue(mirrors.get(0).isInsecure());
        assertEquals("mirror2.example.com", mirrors.get(1).location());
        assertFalse(mirrors.get(1).isInsecure());
    }

    @Test
    void shouldReturnEmptyMirrorsWhenNoneConfigured() {
        RegistriesConf.RegistryConfig registry =
                new RegistriesConf.RegistryConfig("docker.io", "docker.io", null, null, null);
        RegistriesConf conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));

        ContainerRef ref = ContainerRef.parse("docker.io/library/alpine:latest");
        assertTrue(conf.getMirrors(ref).isEmpty());
    }

    @Test
    void shouldReturnEmptyMirrorsWhenNoMatchingRegistry() {
        RegistriesConf.RegistryConfig registry = new RegistriesConf.RegistryConfig(
                "other-registry.example.com",
                "other-registry.example.com",
                null,
                null,
                List.of(new RegistriesConf.MirrorConfig("mirror.example.com", false)));
        RegistriesConf conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));

        ContainerRef ref = ContainerRef.parse("docker.io/library/alpine:latest");
        assertTrue(conf.getMirrors(ref).isEmpty());
    }

    @Test
    void shouldDefaultToSecureWhenInsecureNotSpecified() {

        Registry registry = Registry.builder().build();

        ContainerRef ref = ContainerRef.parse("localhost:5000/library/test:latest");

        // Secure by default
        RegistriesConf.RegistryConfig noInsecureField =
                new RegistriesConf.RegistryConfig("localhost:5000", null, null, null, null);
        RegistriesConf conf = new RegistriesConf(new RegistriesConf.Config(List.of(noInsecureField)));
        assertFalse(conf.isInsecure(registry, ref), "No insecure field means secure by default");

        // Secure
        RegistriesConf.RegistryConfig explicitFalse =
                new RegistriesConf.RegistryConfig("localhost:5000", null, null, false, null);
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(explicitFalse)));
        assertFalse(conf.isInsecure(registry, ref), "Registry must be secure by default");

        // Insecure
        RegistriesConf.RegistryConfig explicitTrue =
                new RegistriesConf.RegistryConfig("localhost:5000", null, null, true, null);
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(explicitTrue)));
        assertTrue(conf.isInsecure(registry, ref), "Registry is insecure");

        // No matching entry → empty (no opinion, fall back to registry flag)
        RegistriesConf empty = new RegistriesConf(new RegistriesConf.Config(List.of()));
        assertFalse(empty.isInsecure(registry, ref), "Secure by default");
    }

    @Test
    void shouldRewriteForMirror() {
        RegistriesConf.MirrorConfig mirror = new RegistriesConf.MirrorConfig("mirror.example.com", false);
        RegistriesConf conf = new RegistriesConf(new RegistriesConf.Config(List.of()));

        // Qualified ref
        ContainerRef original = ContainerRef.parse("docker.io/library/alpine:latest");
        ContainerRef rewritten = conf.rewriteForMirror(original, mirror);
        assertEquals("mirror.example.com/library/alpine:latest", rewritten.toString());

        // Unqualified ref — toString() omits the registry, component-based rewrite must be used
        ContainerRef unqualified = ContainerRef.parse("library/alpine:latest");
        assertTrue(unqualified.isUnqualified());
        ContainerRef rewrittenUnqualified = conf.rewriteForMirror(unqualified, mirror);
        assertFalse(rewrittenUnqualified.isUnqualified());
        assertEquals("mirror.example.com/library/alpine:latest", rewrittenUnqualified.toString());

        // Unqualified ref without explicit namespace
        ContainerRef noNamespace = ContainerRef.parse("alpine:latest");
        assertTrue(noNamespace.isUnqualified());
        ContainerRef rewrittenNoNamespace = conf.rewriteForMirror(noNamespace, mirror);
        assertFalse(rewrittenNoNamespace.isUnqualified());
        assertEquals("mirror.example.com/library/alpine:latest", rewrittenNoNamespace.toString());

        // Digest-only ref (withDigest sets tag to null) — must not produce ":null"
        ContainerRef digestOnly = ContainerRef.parse("docker.io/library/alpine:latest")
                .withDigest("sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a");
        assertNull(digestOnly.getTag());
        ContainerRef rewrittenDigest = conf.rewriteForMirror(digestOnly, mirror);
        assertFalse(rewrittenDigest.toString().contains(":null"), "Tag must not appear as ':null'");
        assertTrue(rewrittenDigest.toString().contains("sha256:"), "Digest must be preserved in rewritten ref");
        // ContainerRef.parse() defaults the tag to "latest" when omitted, so the rewritten ref
        // carries both the digest and the default tag — the digest still drives resolution.
        assertEquals(
                "mirror.example.com/library/alpine:latest@sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a",
                rewrittenDigest.toString());
    }

    @Test
    void shouldRewriteForMirrorWithTrailingSlash() {
        RegistriesConf conf = new RegistriesConf(new RegistriesConf.Config(List.of()));
        ContainerRef original = ContainerRef.parse("docker.io/library/alpine:latest");

        // Single trailing slash
        RegistriesConf.MirrorConfig trailingSlash = new RegistriesConf.MirrorConfig("mirror.example.com/", false);
        assertEquals(
                "mirror.example.com/library/alpine:latest",
                conf.rewriteForMirror(original, trailingSlash).toString());

        // Multiple trailing slashes
        RegistriesConf.MirrorConfig multiTrailingSlash =
                new RegistriesConf.MirrorConfig("mirror.example.com/prefix//", false);
        assertEquals(
                "mirror.example.com/prefix/library/alpine:latest",
                conf.rewriteForMirror(original, multiTrailingSlash).toString());
    }

    private Path writeTempToml(String content) {
        try {
            Path temp = Files.createTempFile("registries", ".conf");
            temp.toFile().deleteOnExit();
            Files.writeString(temp, content);
            return temp;
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
