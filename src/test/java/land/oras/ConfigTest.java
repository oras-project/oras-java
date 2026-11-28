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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import land.oras.utils.SupportedAlgorithm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class ConfigTest {

    @Test
    void shouldSerializeEmptyConfig() {
        Config config = Config.empty();
        assertEquals(
                "{\"mediaType\":\"application/vnd.oci.empty.v1+json\",\"digest\":\"sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a\",\"size\":2,\"data\":\"e30=\"}",
                config.toJson());
    }

    @Test
    void shouldDeserializeConfigWithNoAnnotations() {
        Config config = Config.fromJson(
                "{\"mediaType\":\"application/vnd.oci.empty.v1+json\",\"digest\":\"sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a\",\"size\":2}");
        assertEquals("application/vnd.oci.empty.v1+json", config.getMediaType());
        assertEquals("sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a", config.getDigest());
        assertEquals(2, config.getSize());
        assertNull(config.getAnnotations(), "Annotations should be null");
    }

    @Test
    void shouldDeserializeConfigWithAnnotations() {
        Config config = Config.fromJson(
                "{\"mediaType\":\"application/vnd.oci.empty.v1+json\",\"digest\":\"sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a\",\"size\":2,\"annotations\":{\"key\":\"value\"}}");
        assertEquals("application/vnd.oci.empty.v1+json", config.getMediaType());
        assertEquals("sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a", config.getDigest());
        assertEquals(2, config.getSize());
        assertNotNull(config.getAnnotations(), "Annotations should not be null");
        assertEquals(1, config.getAnnotations().size());
        assertEquals("value", config.getAnnotations().get("key"));
    }

    @Test
    void shouldSaveNullForEmptyAnnotations() {
        Config config = Config.empty();
        config = config.withAnnotations(Annotations.ofConfig(Map.of()));
        assertNull(config.getAnnotations(), "Annotations should be null");
    }

    @Test
    void shouldSaveNonEmptyAnnotations() {
        Config config = Config.empty();
        config = config.withAnnotations(Annotations.ofConfig(Map.of("foo", "bar")));
        assertNotNull(config.getAnnotations(), "Annotations should be null");
        assertEquals("bar", config.getAnnotations().get("foo"));
    }

    @Test
    void testEqualsAndHashCode() {

        Config empty1 = Config.empty();
        Config empty2 = Config.empty();
        assertEquals(empty1, empty2);

        Config config1 = Config.fromJson(
                "{\"mediaType\":\"application/vnd.oci.empty.v1+json\",\"digest\":\"sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a\",\"size\":2}");
        Config config2 = Config.fromJson(
                "{\"mediaType\":\"application/vnd.oci.empty.v1+json\",\"digest\":\"sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a\",\"size\":2}");

        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());

        // Not equals
        assertNotEquals("foo", config1);
        assertNotEquals(null, config1);
    }

    @Test
    void testToString() {
        Config config = Config.fromJson(
                "{\"mediaType\":\"application/vnd.oci.empty.v1+json\",\"digest\":\"sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a\",\"size\":2}");
        assertEquals(
                "{\"mediaType\":\"application/vnd.oci.empty.v1+json\",\"digest\":\"sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a\",\"size\":2}",
                config.toString());
    }

    @Test
    void shouldCreateImageConfigFromLayers() {
        Layer directory = Layer.fromDigest("sha256:aaaa", 10)
                .withMediaType(Const.DEFAULT_BLOB_DIR_MEDIA_TYPE)
                .withAnnotations(Map.of(
                        Const.ANNOTATION_TITLE, "dir",
                        Const.ANNOTATION_ORAS_CONTENT_DIGEST, "sha256:bbbb",
                        Const.ANNOTATION_ORAS_UNPACK, "true"));
        Layer tar = Layer.fromDigest("sha256:cccc", 20).withMediaType(Const.DEFAULT_BLOB_MEDIA_TYPE);

        Config config = Config.ofImage(Platform.linuxAmd64(), List.of(directory, tar));

        String expectedData =
                "{\"architecture\":\"amd64\",\"os\":\"linux\",\"rootfs\":{\"type\":\"layers\",\"diff_ids\":[\"sha256:bbbb\",\"sha256:cccc\"]}}";
        byte[] expectedBytes = expectedData.getBytes(StandardCharsets.UTF_8);
        assertEquals(Const.CONFIG_RUNNING_CONTAINER_MEDIA_TYPE, config.getMediaType());
        assertEquals(expectedData, new String(config.getDataBytes(), StandardCharsets.UTF_8));
        assertEquals(SupportedAlgorithm.SHA256.digest(expectedBytes), config.getDigest());
        assertEquals(expectedBytes.length, config.getSize());
        assertNull(config.getAnnotations(), "Annotations should be null");
    }

    @Test
    void shouldCreateImageConfigWithPlatformDetails() {
        Layer tar = Layer.fromDigest("sha256:cccc", 20).withMediaType(Const.DEFAULT_BLOB_MEDIA_TYPE);
        Platform platform = Platform.of(Const.PLATFORM_WINDOWS, Const.PLATFORM_ARCHITECTURE_ARM64, Const.VARIANT_V8)
                .withOsVersion("10.0.17763.1040")
                .withOsFeatures(List.of("win32k"));

        Config config = Config.ofImage(platform, List.of(tar));

        assertEquals(
                "{\"architecture\":\"arm64\",\"os\":\"windows\",\"os.version\":\"10.0.17763.1040\",\"os.features\":[\"win32k\"],\"variant\":\"v8\",\"rootfs\":{\"type\":\"layers\",\"diff_ids\":[\"sha256:cccc\"]}}",
                new String(config.getDataBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void shouldFailToCreateImageConfigWithoutLayers() {
        assertThrows(OrasException.class, () -> Config.ofImage(Platform.linuxAmd64(), List.of()));
    }

    @Test
    void shouldFailToCreateImageConfigWithUnknownDiffId() {
        Layer zip = Layer.fromDigest("sha256:dddd", 30)
                .withMediaType(Const.ZIP_MEDIA_TYPE)
                .withAnnotations(Map.of(Const.ANNOTATION_TITLE, "dir.zip", Const.ANNOTATION_ORAS_UNPACK, "false"));
        OrasException e = assertThrows(OrasException.class, () -> Config.ofImage(Platform.linuxAmd64(), List.of(zip)));
        assertTrue(e.getMessage().contains("sha256:dddd"), "Message should contain the layer digest");
    }
}
