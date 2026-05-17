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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import land.oras.exception.OrasException;
import org.jspecify.annotations.NullMarked;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Utility class for YAML operations.
 * Use Jackson 3 internally for YAML operations
 */
@NullMarked
public final class YamlUtils {

    /**
     * TOML mapper instance
     */
    private static final ObjectMapper yamlMapper;

    /**
     * Utils class
     */
    private YamlUtils() {
        // Hide constructor
    }

    static {
        yamlMapper = YAMLMapper.builder().build();
    }

    /**
     * Convert an object to a YAML string
     * @param object The object to convert
     * @return The YAML string
     */
    public static String toYaml(Object object) {
        try {
            return yamlMapper.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new OrasException("Unable to convert object to YAML string", e);
        }
    }

    /**
     * Convert a YAML string to an object
     * @param yaml The YAML string
     * @param clazz The class of the object
     * @param <T> The type of the object
     * @return The object
     */
    public static <T> T fromYaml(String yaml, Class<T> clazz) {
        try {
            return yamlMapper.readValue(yaml, clazz);
        } catch (JacksonException e) {
            throw new OrasException("Unable to parse YAML string", e);
        }
    }

    /**
     * Read a YAML file and convert its contents to an object.
     * The file at the given {@code path} is read as UTF-8 YAML and deserialized into the specified type.
     * @param path The path to the YAML file
     * @param clazz The class of the object
     * @param <T> The type of the object
     * @return The object deserialized from the YAML file
     */
    public static <T> T fromYaml(Path path, Class<T> clazz) {
        try {
            return yamlMapper.readValue(Files.readString(path, StandardCharsets.UTF_8), clazz);
        } catch (IOException | JacksonException e) {
            throw new OrasException("Unable to read YAML from file", e);
        }
    }
}
