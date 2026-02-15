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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import land.oras.ContainerRef;
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

        // With null
        RegistriesConf.RegistryConfig registry = new RegistriesConf.RegistryConfig(null, "localhost:5000", null, null);
        assertEquals("localhost:5000", registry.location());
        assertNull(registry.blocked(), "Blocked should be null when not set");
        assertNull(registry.insecure(), "Insecure should be null when not set");
        assertFalse(registry.isBlocked(), "Registry should not be blocked when blocked is null");
        assertFalse(registry.isInsecure(), "Registry should not be insecure when insecure is null");

        // Check some ref
        RegistriesConf conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5001/library/test:latest")));

        // With blocked true
        registry = new RegistriesConf.RegistryConfig(null, "localhost:5000", true, null);
        assertTrue(registry.isBlocked(), "Registry should be blocked when blocked is true");
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));
        assertTrue(conf.isBlocked(ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5001/library/test:latest")));

        // With insecure true
        registry = new RegistriesConf.RegistryConfig(null, "localhost:5000", null, true);
        assertTrue(registry.isInsecure(), "Registry should be insecure when insecure is true");
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));
        assertTrue(conf.isInsecure(ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isInsecure(ContainerRef.parse("localhost:5001/library/test:latest")));

        // With blocked false
        registry = new RegistriesConf.RegistryConfig(null, "localhost:5000", false, null);
        assertFalse(registry.isBlocked(), "Registry should not be blocked when blocked is false");
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5001/library/test:latest")));

        // With insecure false
        registry = new RegistriesConf.RegistryConfig(null, "localhost:5000", null, false);
        assertFalse(registry.isInsecure(), "Registry should be insecure when insecure is false");
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));
        assertFalse(conf.isInsecure(ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isInsecure(ContainerRef.parse("localhost:5001/library/test:latest")));
    }

    @Test
    void shouldCheckRegistryStatusWithPrefixExactMatch() {

        // With null
        RegistriesConf.RegistryConfig registry = new RegistriesConf.RegistryConfig("localhost:5000", null, null, null);
        assertEquals("localhost:5000", registry.prefix());
        assertNull(registry.blocked(), "Blocked should be null when not set");
        assertNull(registry.insecure(), "Insecure should be null when not set");
        assertFalse(registry.isBlocked(), "Registry should not be blocked when blocked is null");
        assertFalse(registry.isInsecure(), "Registry should not be insecure when insecure is null");

        // Check some ref
        RegistriesConf conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5001/library/test:latest")));

        // With blocked true
        registry = new RegistriesConf.RegistryConfig("localhost:5000", null, true, null);
        assertTrue(registry.isBlocked(), "Registry should be blocked when blocked is true");
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));
        assertTrue(conf.isBlocked(ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5001/library/test:latest")));

        // With insecure true
        registry = new RegistriesConf.RegistryConfig("localhost:5000", null, null, true);
        assertTrue(registry.isInsecure(), "Registry should be insecure when insecure is true");
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));
        assertTrue(conf.isInsecure(ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isInsecure(ContainerRef.parse("localhost:5001/library/test:latest")));

        // With blocked false
        registry = new RegistriesConf.RegistryConfig("localhost:5000", null, false, null);
        assertFalse(registry.isBlocked(), "Registry should not be blocked when blocked is false");
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5001/library/test:latest")));

        // With insecure false
        registry = new RegistriesConf.RegistryConfig("localhost:5000", null, null, false);
        assertFalse(registry.isInsecure(), "Registry should be secure when insecure is false");
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));
        assertFalse(conf.isInsecure(ContainerRef.parse("localhost:5000/library/test:latest")));
        assertFalse(conf.isInsecure(ContainerRef.parse("localhost:5001/library/test:latest")));
    }

    @Test
    void checkWithHostNamePrefix() {
        RegistriesConf.RegistryConfig registry = new RegistriesConf.RegistryConfig("*.example.com", null, true, null);
        RegistriesConf conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));
        assertTrue(conf.isBlocked(ContainerRef.parse("registry.example.com/library/test:latest")));
        assertTrue(conf.isBlocked(ContainerRef.parse("foobar.example.com/library/test:latest")));
        assertFalse(conf.isBlocked(ContainerRef.parse("localhost:5000/library/test:latest")));
    }

    @Test
    void checkMultipleSettings() {
        RegistriesConf.RegistryConfig registry1 =
                new RegistriesConf.RegistryConfig("*.internal.local", null, false, true);
        RegistriesConf.RegistryConfig registry2 =
                new RegistriesConf.RegistryConfig("*.internal.local/public", null, true, null);
        RegistriesConf conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry1, registry2)));
        assertTrue(conf.isInsecure(ContainerRef.parse("registry.internal.local/library/test:latest")));
        assertFalse(conf.isInsecure(ContainerRef.parse(
                "registry.internal.local/public/test:latest"))); // Match second rule because longest match wins
        assertFalse(conf.isBlocked(ContainerRef.parse("registry.internal.local/private/test:latest")));
        assertTrue(conf.isBlocked(ContainerRef.parse("registry.internal.local/public/test:latest")));
    }

    @Test
    void shouldRewriteContainerRef() {
        // Just the domain
        RegistriesConf.RegistryConfig registry =
                new RegistriesConf.RegistryConfig("localhost:5000", "registry.example.com", null, null);
        RegistriesConf conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));
        ContainerRef originalRef = ContainerRef.parse("localhost:5000/library/test:latest");
        ContainerRef rewrittenRef = conf.rewrite(originalRef);
        assertEquals("registry.example.com/library/test:latest", rewrittenRef.toString());

        // With
        // prefix = "example.com/foo"
        // location = "internal-registry-for-example.com/bar"
        registry = new RegistriesConf.RegistryConfig(
                "example.com/foo", "internal-registry-for-example.com/bar", null, null);
        conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));
        originalRef = ContainerRef.parse("example.com/foo/library/test:latest");
        rewrittenRef = conf.rewrite(originalRef);
        assertEquals("internal-registry-for-example.com/bar/library/test:latest", rewrittenRef.toString());

        // With tag only
        // registry = new RegistriesConf.RegistryConfig("example.com/foo:latest", "example.com/foo:othertag", null,
        // null);
        // conf = new RegistriesConf(new RegistriesConf.Config(List.of(registry)));
        // originalRef = ContainerRef.parse("example.com/foo:latest");
        // rewrittenRef = conf.rewrite(originalRef);
        // assertEquals("example.com/foo:othertag", rewrittenRef.toString());
    }
}
