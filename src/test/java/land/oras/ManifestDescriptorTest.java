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

import java.util.Map;
import land.oras.utils.Const;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class ManifestDescriptorTest {

    @Test
    void shouldSetAnnotations() {
        Manifest manifest = Manifest.empty();
        ManifestDescriptor descriptor = manifest.getDescriptor();
        descriptor = descriptor.withAnnotations(Map.of(Const.ANNOTATION_REF, "latest"));
        assertEquals("latest", descriptor.getAnnotations().get(Const.ANNOTATION_REF));
    }

    @Test
    void shouldReadFromJson() {
        ManifestDescriptor descriptor = ManifestDescriptor.fromJson(descriptor());
        assertEquals("application/vnd.oci.image.manifest.v1+json", descriptor.getMediaType());
        assertEquals("sha256:09c8ec8bf0d43a250ba7fed2eb6f242935b2987be5ed921ee06c93008558f980", descriptor.getDigest());
        assertEquals(838, descriptor.getSize());
        assertEquals("riscv64", descriptor.getAnnotations().get("com.docker.official-images.bashbrew.arch"));
        assertEquals(
                "sha256:1de5eb4a9a6735adb46b2c9c88674c0cfba3444dd4ac2341b3babf1261700529",
                descriptor.getAnnotations().get("vnd.docker.reference.digest"));
        assertEquals("attestation-manifest", descriptor.getAnnotations().get("vnd.docker.reference.type"));
        assertEquals("unknown", descriptor.getPlatform().get("architecture"));
        assertEquals("unknown", descriptor.getPlatform().get("os"));
        descriptor.toJson();
    }

    @Test
    void testEqualsAndHashCode() {
        ManifestDescriptor descriptor1 = ManifestDescriptor.fromJson(descriptor());
        ManifestDescriptor descriptor2 = ManifestDescriptor.fromJson(descriptor());
        assertEquals(descriptor1, descriptor2);
        assertEquals(descriptor1.hashCode(), descriptor2.hashCode());
    }

    @Test
    void testToString() {
        ManifestDescriptor descriptor = ManifestDescriptor.fromJson(descriptor());
        String expected =
                "{\"mediaType\":\"application/vnd.oci.image.manifest.v1+json\",\"digest\":\"sha256:09c8ec8bf0d43a250ba7fed2eb6f242935b2987be5ed921ee06c93008558f980\",\"size\":838,\"platform\":{\"architecture\":\"unknown\",\"os\":\"unknown\"},\"annotations\":{\"com.docker.official-images.bashbrew.arch\":\"riscv64\",\"vnd.docker.reference.digest\":\"sha256:1de5eb4a9a6735adb46b2c9c88674c0cfba3444dd4ac2341b3babf1261700529\",\"vnd.docker.reference.type\":\"attestation-manifest\"}}";
        assertEquals(expected, descriptor.toString());
    }

    private String descriptor() {
        // language=json
        return """
            {
              "mediaType": "application/vnd.oci.image.manifest.v1+json",
              "digest": "sha256:09c8ec8bf0d43a250ba7fed2eb6f242935b2987be5ed921ee06c93008558f980",
              "size": 838,
              "annotations": {
                "com.docker.official-images.bashbrew.arch": "riscv64",
                "vnd.docker.reference.digest": "sha256:1de5eb4a9a6735adb46b2c9c88674c0cfba3444dd4ac2341b3babf1261700529",
                "vnd.docker.reference.type": "attestation-manifest"
              },
              "platform": {
                "architecture": "unknown",
                "os": "unknown"
              }
            }
            """;
    }
}
