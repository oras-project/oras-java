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

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Execution(ExecutionMode.CONCURRENT)
public class ManifestTest {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ManifestTest.class);

    @Test
    void shouldReadManifest() {
        String json = sampleManifest();
        Manifest manifest = Manifest.fromJson(json);

        // Assert manifest
        assertEquals(2, manifest.getSchemaVersion());
        assertEquals("application/vnd.oci.image.manifest.v1+json", manifest.getMediaType());
        assertEquals(7023, manifest.getConfig().getSize());
        assertEquals(
                "sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                manifest.getConfig().getDigest());
        assertEquals(
                "application/vnd.oci.image.config.v1+json", manifest.getConfig().getMediaType());
        assertEquals(2, manifest.getLayers().size());
        assertEquals(0, manifest.getAnnotations().size());

        // Assert layer
        assertEquals(32654, manifest.getLayers().get(0).getSize());
        assertEquals(
                "sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                manifest.getLayers().get(0).getDigest());
        assertEquals(
                "application/vnd.oci.image.layer.v1.tar+gzip",
                manifest.getLayers().get(0).getMediaType());
        assertEquals(1048576, manifest.getLayers().get(1).getSize());
        assertEquals(
                "sha256:1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                manifest.getLayers().get(1).getDigest());
        assertEquals(
                "application/vnd.oci.image.layer.v1.tar+gzip",
                manifest.getLayers().get(1).getMediaType());
        manifest.toJson();
    }

    @Test
    void shouldHaveEmptyManifest() {
        assertEquals(
                Manifest.fromJson(emptyManifest()).toJson(), Manifest.empty().toJson());
    }

    @Test
    void testEqualsAndHashCode() {

        // Empty
        Manifest empty1 = Manifest.empty();
        Manifest empty2 = Manifest.empty();
        assertEquals(empty1, empty2);

        // Manifest data
        Manifest object1 = Manifest.fromJson(sampleManifest());
        Manifest object2 = Manifest.fromJson(sampleManifest());
        assertEquals(object1, object2);
        assertEquals(object1.hashCode(), object2.hashCode());

        // Not equals
        object2 = object2.withArtifactType(ArtifactType.from("test/plain"));
        assertNotEquals(object1, object2);
        assertNotEquals(object1.hashCode(), object2.hashCode());

        // Different type
        assertNotEquals("not a manifest", object1);
    }

    @Test
    void testToString() {
        Manifest manifest = Manifest.fromJson(sampleManifest());
        String json = manifest.toString();
        assertNotNull(json);
        assertEquals(
                "{\"schemaVersion\":2,\"config\":{\"mediaType\":\"application/vnd.oci.image.config.v1+json\",\"digest\":\"sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890\",\"size\":7023},\"layers\":[{\"mediaType\":\"application/vnd.oci.image.layer.v1.tar+gzip\",\"digest\":\"sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890\",\"size\":32654},{\"mediaType\":\"application/vnd.oci.image.layer.v1.tar+gzip\",\"digest\":\"sha256:1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef\",\"size\":1048576}],\"mediaType\":\"application/vnd.oci.image.manifest.v1+json\"}",
                json);
    }

    @Test
    void shouldReadDigestTopLevelDescriptor() {
        String json =
                "{\"schemaVersion\":2,\"mediaType\":\"application/vnd.oci.image.index.v1+json\",\"digest\":\"sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef\",\"size\":7023}";
        Manifest manifest = Manifest.fromJson(json);
        assertEquals(
                "sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", manifest.getDigest());
    }

    @Test
    void shouldHaveNoLayerForIndex() {
        String json =
                "{\"schemaVersion\":2,\"mediaType\":\"application/vnd.oci.image.index.v1+json\",\"annotations\":{}}";
        Manifest manifest = Manifest.fromJson(json);
        assertEquals(0, manifest.getLayers().size());
        assertEquals(json, manifest.getJson());
    }

    @Test
    void shouldReadFromPath() {
        Path path = Path.of(
                "src/test/resources/oci/artifact/blobs/sha256/cb1d49baba271af2c56d493d66dddb112ecf1c2c52f47e6f45f3617bb2155d34");
        Manifest manifest = Manifest.fromPath(path);
        assertNotNull(manifest.getJson());
    }

    @Test
    void shouldGetArtifactTest() {
        Manifest manifest1 = Manifest.empty().withArtifactType(ArtifactType.from("test/plain"));
        assertEquals("test/plain", manifest1.getArtifactType().getMediaType());
        Manifest manifest2 = Manifest.empty().withConfig(Config.empty().withMediaType("test/plain"));
        assertEquals("test/plain", manifest2.getArtifactType().getMediaType());
    }

    private String emptyManifest() {
        return """
            {
              "schemaVersion": 2,
              "mediaType": "application/vnd.oci.image.manifest.v1+json",
              "config": {
                "mediaType": "application/vnd.oci.empty.v1+json",
                "digest": "sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a",
                "size": 2,
                "data": "e30="
              },
              "layers": [],
              "annotations": {}
            }
             """;
    }

    /**
     * A sample manifest
     * @return The manifest
     */
    private String sampleManifest() {
        // language=JSON
        return """
                {
                  "schemaVersion": 2,
                  "mediaType": "application/vnd.oci.image.manifest.v1+json",
                  "config": {
                    "mediaType": "application/vnd.oci.image.config.v1+json",
                    "digest": "sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                    "size": 7023
                  },
                  "layers": [
                    {
                      "mediaType": "application/vnd.oci.image.layer.v1.tar+gzip",
                      "digest": "sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                      "size": 32654
                    },
                    {
                      "mediaType": "application/vnd.oci.image.layer.v1.tar+gzip",
                      "digest": "sha256:1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                      "size": 1048576
                    }
                  ]
                }
             """;
    }
}
