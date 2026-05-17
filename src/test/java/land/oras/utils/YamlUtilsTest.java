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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import land.oras.exception.OrasException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class YamlUtilsTest {

    /**
     * Temporary dir
     */
    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    private static Path dir;

    @Test
    void failToParseYamlString() {
        assertThrows(OrasException.class, () -> YamlUtils.fromYaml("not a yaml: too, bar: test", Object.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldParseYaml() {
        String yamlMap =
                """
                ---
                key1: "value1"
                key2: "value2"
                """;
        Map<String, String> map = YamlUtils.fromYaml(yamlMap, Map.class);
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldParseYamlFile() throws IOException {
        String yamlMap =
                """
                ---
                key1: "value1"
                key2: "value2"
                """;
        Path yamlFile = dir.resolve("test.yaml");
        Files.writeString(yamlFile, yamlMap);
        Map<String, String> map = YamlUtils.fromYaml(yamlFile, Map.class);
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFailToParseYamlFile() {
        assertThrows(OrasException.class, () -> YamlUtils.fromYaml(Path.of("foo.yaml"), Map.class));
    }

    @Test
    void shouldConvertToYaml() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        String yaml = YamlUtils.toYaml(map);
        assertNotNull(yaml);
        String expected = """
            ---
            key1: "value1"
            key2: "value2"
            """;
        assertEquals(expected, yaml);
    }
}
