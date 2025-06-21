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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class ConfigTest {

    @Test
    void shouldSerializeEmptyConfig() {
        Config config = Config.empty();
        assertEquals(
                "{\"data\":\"e30=\",\"mediaType\":\"application/vnd.oci.empty.v1+json\",\"digest\":\"sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a\",\"size\":2}",
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
}
