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
class TomlUtilsTest {

    /**
     * Temporary dir
     */
    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    private static Path dir;

    @Test
    void failToParseTomlString() {
        assertThrows(OrasException.class, () -> TomlUtils.fromToml("not a toml", Object.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldParseToml() {
        String tomlMap = """
                key1 = "value1"
                key2 = "value2"
                """;
        Map<String, String> map = TomlUtils.fromToml(tomlMap, Map.class);
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldParseTomlFile() throws IOException {
        String tomlMap = """
                key1 = "value1"
                key2 = "value2"
                """;
        Path tomlFile = dir.resolve("test.toml");
        Files.writeString(tomlFile, tomlMap);
        Map<String, String> map = TomlUtils.fromToml(tomlFile, Map.class);
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
    }

    @Test
    void shouldConvertToToml() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        String toml = TomlUtils.toToml(map);
        assertNotNull(toml);
        String expected = """
            key1 = 'value1'
            key2 = 'value2'
            """;
        assertEquals(expected, toml);
    }
}
