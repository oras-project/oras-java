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

import land.oras.exception.OrasException;
import land.oras.utils.SupportedAlgorithm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class ContainerRefTest {

    @Test
    void shouldParseImageWithAllParts() {
        ContainerRef containerRef =
                ContainerRef.parse("docker.io/library/foo/hello-world:latest@sha256:1234567890abcdef");
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("registry-1.docker.io", containerRef.getApiRegistry());
        assertEquals("library/foo", containerRef.getNamespace());
        assertEquals("hello-world", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertEquals(SupportedAlgorithm.SHA256, containerRef.getAlgorithm());
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());
    }

    @Test
    void shouldFailWithUnSupportedAlgorithm() {
        assertThrows(
                OrasException.class,
                () -> ContainerRef.parse("docker.io/library/foo/hello-world:latest@test:1234567890abcdef"),
                "Unsupported algorithm: test");
    }

    @Test
    void shouldParseImageWithNoNamespace() {
        ContainerRef containerRef = ContainerRef.parse("docker.io/hello-world:latest@sha256:1234567890abcdef");
        assertEquals("docker.io", containerRef.getRegistry());
        assertNull(containerRef.getNamespace());
        assertEquals("hello-world", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());
    }

    @Test
    void shouldParseImageWithNoTag() {
        ContainerRef containerRef = ContainerRef.parse("docker.io/hello-world@sha256:1234567890abcdef");
        assertEquals("docker.io", containerRef.getRegistry());
        assertNull(containerRef.getNamespace());
        assertEquals("hello-world", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());
    }

    @Test
    void shouldParseImageWithNoDigest() {
        ContainerRef containerRef = ContainerRef.parse("docker.io/hello-world:latest");
        assertEquals("docker.io", containerRef.getRegistry());
        assertNull(containerRef.getNamespace());
        assertEquals("hello-world", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertNull(containerRef.getDigest());
    }

    @Test
    void shouldParseImageWithNoRegistry() {
        ContainerRef containerRef = ContainerRef.parse("hello-world:latest");
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("registry-1.docker.io", containerRef.getApiRegistry());
        assertNull(containerRef.getNamespace());
        assertEquals("hello-world", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertNull(containerRef.getDigest());
    }

    @Test
    void shouldParseImageWithNoTagAndNoRegistry() {
        ContainerRef containerRef = ContainerRef.parse("hello-world");
        assertEquals("docker.io", containerRef.getRegistry());
        assertNull(containerRef.getNamespace());
        assertEquals("hello-world", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertNull(containerRef.getDigest());
    }

    @Test
    void shouldGetTagsPath() {
        ContainerRef containerRef =
                ContainerRef.parse("docker.io/library/foo/hello-world:latest@sha256:1234567890abcdef");
        assertEquals("docker.io/v2/library/foo/hello-world/tags/list", containerRef.getTagsPath());
    }

    @Test
    void shouldGetReferenceFromUrl() {
        ContainerRef containerRef = ContainerRef.fromUrl("http://docker.io/foo/bar/test/api");
        assertEquals("docker.io", containerRef.getRegistry());
    }
}
