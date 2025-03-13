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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import land.oras.exception.OrasException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class JsonUtilsTest {

    /**
     * Temporary dir
     */
    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    private static Path dir;

    @Test
    void failToParseJsonString() {
        assertThrows(OrasException.class, () -> JsonUtils.fromJson("not a json", Object.class));
    }

    @Test
    void failtToReadFromFile() {
        assertThrows(OrasException.class, () -> JsonUtils.fromJson(Path.of("foo"), Object.class));
    }

    @Test
    void failToParseJsonFile() throws IOException {
        Files.createFile(dir.resolve("file.json"));
        Files.write(dir.resolve("file.json"), "not a json".getBytes());

        Type type = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] {Object.class};
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        assertThrows(OrasException.class, () -> JsonUtils.fromJson("not a json", Object.class));
        assertThrows(OrasException.class, () -> JsonUtils.fromJson(dir.resolve("file.json"), Object.class));
        assertThrows(OrasException.class, () -> JsonUtils.fromJson(dir.resolve("file.json"), type));
        assertThrows(OrasException.class, () -> JsonUtils.fromJson(dir.resolve("unknown.json"), Object.class));
        assertThrows(OrasException.class, () -> JsonUtils.fromJson(dir.resolve("unknown.json"), type));
        assertThrows(
                OrasException.class,
                () -> JsonUtils.fromJson(new FileReader(dir.resolve("file.json").toFile()), type));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldParseJsonFile() throws IOException {
        Files.createFile(dir.resolve("valid.json"));
        Files.write(
                dir.resolve("valid.json"),
                JsonUtils.toJson(List.of(1.0, 2.0, 3.0, 4.0)).getBytes());

        // With class
        List<Double> list = JsonUtils.fromJson(dir.resolve("valid.json"), List.class);
        assertEquals(List.of(1.0, 2.0, 3.0, 4.0), list);
        List<Double> list2 =
                JsonUtils.fromJson(new FileReader(dir.resolve("valid.json").toFile()), List.class);
        assertEquals(List.of(1.0, 2.0, 3.0, 4.0), list2);

        // With type
        Type type = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] {Double.class};
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        List<Double> list3 = JsonUtils.fromJson(dir.resolve("valid.json"), type);
        assertEquals(List.of(1.0, 2.0, 3.0, 4.0), list3);
        List<Double> list4 =
                JsonUtils.fromJson(new FileReader(dir.resolve("valid.json").toFile()), type);
        assertEquals(List.of(1.0, 2.0, 3.0, 4.0), list4);
    }
}
