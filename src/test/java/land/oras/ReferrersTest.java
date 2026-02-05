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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class ReferrersTest {

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

    @Test
    void testEquals() {
        Referrers referrers1 = Referrers.from(List.of(ManifestDescriptor.fromJson(descriptor())));
        Referrers referrers2 = Referrers.from(List.of(ManifestDescriptor.fromJson(descriptor())));

        assertEquals(referrers1, referrers2);
        assertEquals(referrers1.hashCode(), referrers2.hashCode());

        // Not equals
        assertNotEquals("foo", referrers1);
        assertNotEquals(null, referrers1);
    }

    @Test
    void testToString() {
        Referrers referrers = Referrers.from(List.of(ManifestDescriptor.fromJson(descriptor())));
        assertEquals(
                "{\"mediaType\":\"application/vnd.oci.image.index.v1+json\",\"manifests\":[{\"mediaType\":\"application/vnd.oci.image.manifest.v1+json\",\"digest\":\"sha256:09c8ec8bf0d43a250ba7fed2eb6f242935b2987be5ed921ee06c93008558f980\",\"size\":838,\"platform\":{\"architecture\":\"unknown\",\"os\":\"unknown\"},\"annotations\":{\"com.docker.official-images.bashbrew.arch\":\"riscv64\",\"vnd.docker.reference.digest\":\"sha256:1de5eb4a9a6735adb46b2c9c88674c0cfba3444dd4ac2341b3babf1261700529\",\"vnd.docker.reference.type\":\"attestation-manifest\"}}]}",
                referrers.toString());
    }
}
