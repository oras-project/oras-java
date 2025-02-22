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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class LayerTest {

    @Test
    void shouldReadLayer() {
        String json = sampleLayer();
        Layer layer = Layer.fromJson(json);
        assertEquals("application/vnd.oci.image.layer.v1.tar+gzip", layer.getMediaType());
        assertEquals("sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890", layer.getDigest());
        assertEquals(32654, layer.getSize());
        assertEquals(json, layer.toJson());
    }

    @Test
    void shouldHaveEmptyLayer() {
        String json = emptyLayer();
        assertEquals(Layer.fromJson(json).toJson(), Layer.empty().toJson());
    }

    @Test
    void shouldReadNullAnnotations() {
        String json =
                """
            {
              "mediaType": "application/vnd.oci.image.layer.v1.tar+gzip",
              "digest": "sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
              "size": 32654
            }
        """;
        Layer layer = Layer.fromJson(json);
        assertEquals("application/vnd.oci.image.layer.v1.tar+gzip", layer.getMediaType());
        assertEquals("sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890", layer.getDigest());
        assertEquals(32654, layer.getSize());
        assertEquals(0, layer.getAnnotations().size());
    }

    @Test
    void shouldReadBlobData() {
        String json =
                """
            {
              "mediaType": "application/vnd.oci.image.layer.v1.tar+gzip",
              "digest": "sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
              "data": "e30="
            }
        """;
        Layer layer = Layer.fromJson(json);
        assertEquals("application/vnd.oci.image.layer.v1.tar+gzip", layer.getMediaType());
        assertEquals("sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890", layer.getDigest());
        assertEquals("e30=", layer.getData());
    }

    private String emptyLayer() {
        return """
            {
              "mediaType": "application/vnd.oci.empty.v1+json",
              "digest": "sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a",
              "size": 2,
              "data": "e30=",
              "annotations": {}
            }
        """;
    }

    /**
     * A sample manifest
     * @return The manifest
     */
    private String sampleLayer() {
        return Layer.fromJson(
                        """
                            {
                              "mediaType": "application/vnd.oci.image.layer.v1.tar+gzip",
                              "digest": "sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                              "size": 32654
                            }
                        """)
                .toJson();
    }
}
