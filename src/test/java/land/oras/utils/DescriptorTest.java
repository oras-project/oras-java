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

package land.oras.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;
import land.oras.Descriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class DescriptorTest {

    @Test
    void shouldBuildMinimalDescriptor() {
        Descriptor descriptor =
                Descriptor.of("sha256:cb1d49baba271af2c56d493d66dddb112ecf1c2c52f47e6f45f3617bb2155d34", 556L);
        assertEquals("sha256:cb1d49baba271af2c56d493d66dddb112ecf1c2c52f47e6f45f3617bb2155d34", descriptor.getDigest());
        assertEquals(556L, descriptor.getSize());
        assertEquals(Const.DEFAULT_DESCRIPTOR_MEDIA_TYPE, descriptor.getMediaType());
        assertNull(descriptor.getArtifactType());
        assertTrue(descriptor.getAnnotations().isEmpty());
    }

    @Test
    void shouldBuildWithMediaType() {
        Descriptor descriptor = Descriptor.of(
                "sha256:cb1d49baba271af2c56d493d66dddb112ecf1c2c52f47e6f45f3617bb2155d34",
                556L,
                Const.DEFAULT_MANIFEST_MEDIA_TYPE);
        assertEquals("sha256:cb1d49baba271af2c56d493d66dddb112ecf1c2c52f47e6f45f3617bb2155d34", descriptor.getDigest());
        assertEquals(556L, descriptor.getSize());
        assertEquals(Const.DEFAULT_MANIFEST_MEDIA_TYPE, descriptor.getMediaType());
        assertNull(descriptor.getArtifactType());
        assertTrue(descriptor.getAnnotations().isEmpty());
    }

    @Test
    void shouldBuildComplete() {
        Descriptor descriptor = Descriptor.of(
                "sha256:cb1d49baba271af2c56d493d66dddb112ecf1c2c52f47e6f45f3617bb2155d34",
                556L,
                Const.DEFAULT_MANIFEST_MEDIA_TYPE,
                Map.of("foo", "bar"),
                "foo/bar");
        assertEquals("sha256:cb1d49baba271af2c56d493d66dddb112ecf1c2c52f47e6f45f3617bb2155d34", descriptor.getDigest());
        assertEquals(556L, descriptor.getSize());
        assertEquals(Const.DEFAULT_MANIFEST_MEDIA_TYPE, descriptor.getMediaType());
        assertEquals("bar", descriptor.getAnnotations().get("foo"));
        assertNotNull(descriptor.getArtifactType());
        assertEquals("foo/bar", descriptor.getArtifactType().getMediaType());
    }

    @Test
    void testEqualsAndHashCode() {
        Descriptor descriptor1 = Descriptor.of(
                "sha256:cb1d49baba271af2c56d493d66dddb112ecf1c2c52f47e6f45f3617bb2155d34",
                556L,
                Const.DEFAULT_MANIFEST_MEDIA_TYPE);
        Descriptor descriptor2 = Descriptor.of(
                "sha256:cb1d49baba271af2c56d493d66dddb112ecf1c2c52f47e6f45f3617bb2155d34",
                556L,
                Const.DEFAULT_MANIFEST_MEDIA_TYPE);
        assertEquals(descriptor1, descriptor2);
        assertEquals(descriptor1.hashCode(), descriptor2.hashCode());

        // Not equals
        assertNotEquals("foo", descriptor1);
        assertNotEquals(null, descriptor1);
    }

    @Test
    void testToString() {
        Descriptor descriptor = Descriptor.of(
                "sha256:cb1d49baba271af2c56d493d66dddb112ecf1c2c52f47e6f45f3617bb2155d34",
                556L,
                Const.DEFAULT_MANIFEST_MEDIA_TYPE);
        String expected =
                "{\"mediaType\":\"application/vnd.oci.image.manifest.v1+json\",\"digest\":\"sha256:cb1d49baba271af2c56d493d66dddb112ecf1c2c52f47e6f45f3617bb2155d34\",\"size\":556}";
        assertEquals(expected, descriptor.toString());
    }
}
