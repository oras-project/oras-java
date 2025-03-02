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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class IndexTest {

    @Test
    void shouldReadAndWriteIndex() {
        String json =
                "{\"schemaVersion\":2,\"manifests\":[{\"mediaType\":\"application/vnd.oci.image.manifest.v1+json\",\"digest\":\"sha256:f381775b1f558b02165b5dfe1b2f973387d995e18302c4039daabd32f938cb27\",\"size\":559}]}";
        Index index = Index.fromJson(json);
        assertNull(index.getMediaType());
        assertEquals(2, index.getSchemaVersion());
        assertEquals(1, index.getManifests().size());
        assertNull(index.getArtifactType());
        assertNull(index.getDescriptor());
        assertEquals(
                "sha256:f381775b1f558b02165b5dfe1b2f973387d995e18302c4039daabd32f938cb27",
                index.getManifests().get(0).getDigest());
        assertEquals(559, index.getManifests().get(0).getSize());
        assertEquals(json, index.toJson());
        index.toJson();
    }
}
