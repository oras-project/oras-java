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
    void shouldReturnWithDigest() {

        // Explicit
        ContainerRef containerRef1 = ContainerRef.parse("docker.io/library/foo");
        ContainerRef withDigest1 = containerRef1.withDigest("sha256@12344");
        assertEquals("sha256@12344", withDigest1.getDigest());
        assertEquals("library", withDigest1.getNamespace());
        assertEquals(
                "library",
                withDigest1.getNamespace(
                        Registry.builder().withRegistry("foo.io").build()));

        // Implicit
        ContainerRef containerRef2 = ContainerRef.parse("foo");
        ContainerRef withDigest2 = containerRef2.withDigest("sha256@12344");
        assertEquals("sha256@12344", withDigest2.getDigest());
        assertEquals("library", withDigest2.getNamespace());
        assertEquals(
                null,
                withDigest2.getNamespace(
                        Registry.builder().withRegistry("foo.io").build()));
    }

    @Test
    void shouldParseImageWithAllParts() {
        ContainerRef containerRef = ContainerRef.parse("docker.io/library/foo/alpine:latest@sha256:1234567890abcdef");
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("registry-1.docker.io", containerRef.getApiRegistry());
        assertEquals("library/foo", containerRef.getNamespace());
        assertEquals("library/foo", containerRef.getNamespace(Registry.builder().build()));
        assertEquals("alpine", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertEquals(SupportedAlgorithm.SHA256, containerRef.getAlgorithm());
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());
    }

    @Test
    void shouldFailWithUnSupportedAlgorithm() {
        assertThrows(
                OrasException.class,
                () -> ContainerRef.parse("docker.io/library/foo/alpine:latest@test:1234567890abcdef"),
                "Unsupported algorithm: test");
        assertThrows(
                OrasException.class,
                () -> ContainerRef.parse("docker.io/library/foo/alpine:latest@sha256:sha256:1234567890abcdef"),
                "Unsupported algorithm: test");
    }

    @Test
    void shouldParseImageWithNoNamespace() {
        ContainerRef containerRef = ContainerRef.parse("docker.io/alpine:latest@sha256:1234567890abcdef");
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("library", containerRef.getNamespace());
        assertEquals("library", containerRef.getNamespace(Registry.builder().build()));
        assertEquals("alpine", containerRef.getRepository());
        assertEquals(
                "registry-1.docker.io/v2/library/alpine/manifests/sha256:1234567890abcdef",
                containerRef.getManifestsPath());
        assertEquals("latest", containerRef.getTag());
        assertEquals(
                "registry-1.docker.io/v2/library/alpine/referrers/sha256:1234567890abcdef?artifactType=test%2Fbar",
                containerRef.getReferrersPath(ArtifactType.from("test/bar")));
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());
        assertEquals(
                "registry-1.docker.io/v2/library/alpine/manifests/sha256:1234567890abcdef",
                containerRef.getManifestsPath());
        containerRef = ContainerRef.parse("demo.goharbor.com/alpine:latest@sha256:1234567890abcdef");
        assertEquals("demo.goharbor.com", containerRef.getRegistry());
        assertEquals("demo.goharbor.com/v2/alpine/tags/list", containerRef.getTagsPath());
        assertNull(containerRef.getNamespace());
        assertEquals("alpine", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());
    }

    @Test
    void shouldParseImageWithNoTag() {
        ContainerRef containerRef = ContainerRef.parse("docker.io/alpine@sha256:1234567890abcdef");
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("registry-1.docker.io/v2/library/alpine/tags/list", containerRef.getTagsPath());
        assertEquals("library", containerRef.getNamespace());
        assertEquals("library", containerRef.getNamespace(Registry.builder().build()));
        assertEquals("alpine", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertEquals(
                "registry-1.docker.io/v2/library/alpine/blobs/sha256:1234567890abcdef",
                containerRef.getBlobsPath(null));
        assertEquals(
                "foo.io/v2/alpine/blobs/sha256:1234567890abcdef",
                containerRef.getBlobsPath(
                        Registry.builder().withRegistry("foo.io").build()));
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());

        containerRef = ContainerRef.parse("docker.io/foobar/alpine@sha256:1234567890abcdef");
        assertEquals("registry-1.docker.io/v2/foobar/alpine/tags/list", containerRef.getTagsPath());
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("foobar", containerRef.getNamespace());
        assertEquals("alpine", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertEquals(
                "registry-1.docker.io/v2/foobar/alpine/blobs/sha256:1234567890abcdef", containerRef.getBlobsPath(null));
        assertEquals(
                "foo.io/v2/foobar/alpine/blobs/sha256:1234567890abcdef",
                containerRef.getBlobsPath(
                        Registry.builder().withRegistry("foo.io").build()));
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());
    }

    @Test
    void shouldParseImageWithNoDigest() {
        ContainerRef containerRef = ContainerRef.parse("docker.io/alpine:latest");
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("library", containerRef.getNamespace());
        assertEquals("alpine", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertNull(containerRef.getDigest());
    }

    @Test
    void shouldParseImageWithNoRegistry() {
        ContainerRef containerRef = ContainerRef.parse("alpine:latest");
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("registry-1.docker.io", containerRef.getApiRegistry());
        assertEquals(
                "foo.io",
                containerRef.getApiRegistry(
                        Registry.builder().withRegistry("foo.io").build()));
        assertEquals("registry-1.docker.io/v2/library/alpine/tags/list", containerRef.getTagsPath());
        assertEquals("library", containerRef.getNamespace());
        assertEquals("alpine", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertNull(containerRef.getDigest());
    }

    @Test
    void shouldParseImageWithNoTagAndNoRegistry() {
        ContainerRef containerRef = ContainerRef.parse("alpine");
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("library", containerRef.getNamespace());
        assertEquals("library", containerRef.getNamespace(Registry.builder().build()));
        assertEquals(
                "library",
                containerRef.getNamespace(
                        Registry.builder().withRegistry("docker.io").build()));
        assertNull(
                containerRef.getNamespace(
                        Registry.builder().withRegistry("test").build()),
                "Default library must be null when changing registry");
        assertEquals("alpine", containerRef.getRepository());
        assertEquals("library", containerRef.getNamespace());
        assertEquals("latest", containerRef.getTag());
        assertNull(containerRef.getDigest());

        containerRef = ContainerRef.parse("foobar/alpine");
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("foobar", containerRef.getNamespace());
        assertEquals("foobar", containerRef.getNamespace(Registry.builder().build()));
        assertEquals(
                "foobar",
                containerRef.getNamespace(
                        Registry.builder().withRegistry("docker.io").build()));
        assertEquals(
                "foobar",
                containerRef.getNamespace(
                        Registry.builder().withRegistry("test").build()));
        assertEquals("alpine", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertNull(containerRef.getDigest());
    }

    @Test
    void shouldGetTagsPathDockerIo() {
        ContainerRef containerRef = ContainerRef.parse("docker.io/library/foo/alpine:latest@sha256:1234567890abcdef");
        assertEquals("registry-1.docker.io/v2/library/foo/alpine/tags/list", containerRef.getTagsPath());
    }

    @Test
    void shouldBuildNewRefForRegistrx() {
        Registry registry =
                Registry.builder().defaults().withRegistry("my-registry.io").build();
        ContainerRef containerRef = ContainerRef.parse("library/foo/alpine:latest@sha256:1234567890abcdef")
                .forRegistry(registry);
        assertEquals("my-registry.io/v2/library/foo/alpine/tags/list", containerRef.getTagsPath());
    }

    @Test
    void shouldGetTagsPathOtherRegistry() {
        ContainerRef containerRef =
                ContainerRef.parse("demo.goharbor.io/library/foo/alpine:latest@sha256:1234567890abcdef");
        assertEquals("demo.goharbor.io/v2/library/foo/alpine/tags/list", containerRef.getTagsPath());
        assertEquals(
                "demo.goharbor.io/v2/library/foo/alpine/blobs/sha256:1234567890abcdef",
                containerRef.getBlobsPath(null));
        assertEquals(
                "foo.io/v2/library/foo/alpine/blobs/sha256:1234567890abcdef",
                containerRef.getBlobsPath(
                        Registry.builder().withRegistry("foo.io").build()));
    }

    @Test
    void shouldGetReferenceFromUrl() {
        ContainerRef containerRef = ContainerRef.fromUrl("http://docker.io/foo/bar/test/api");
        assertEquals("docker.io", containerRef.getRegistry());
    }
}
