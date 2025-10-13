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
import tools.jackson.dataformat.toml.TomlMapper;

/**
 * Utility class for TOML operations.
 * Use Jackson 3 internally for TOML operations
 */
@NullMarked
public final class TomlUtils {

    /**
     * TOML mapper instance
     */
    private static final ObjectMapper tomlMapper;

    /**
     * Utils class
     */
    private TomlUtils() {
        // Hide constructor
    }

    static {
        tomlMapper = TomlMapper.builder().build();
    }

    /**
     * Convert an object to a TOML string
     * @param object The object to convert
     * @return The TOML string
     */
    public static String toToml(Object object) {
        try {
            return tomlMapper.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new OrasException("Unable to convert object to TOML string", e);
        }
    }

    /**
     * Convert a TOML string to an object
     * @param toml The TOML string
     * @param clazz The class of the object
     * @param <T> The type of the object
     * @return The object
     */
    public static <T> T fromToml(String toml, Class<T> clazz) {
        try {
            return tomlMapper.readValue(toml, clazz);
        } catch (JacksonException e) {
            throw new OrasException("Unable to parse TOML string", e);
        }
    }

    /**
     * Read a TOML file and convert its contents to an object.
     * The file at the given {@code path} is read as UTF-8 TOML and deserialized into the specified type.
     * @param path The path to the TOML file
     * @param clazz The class of the object
     * @param <T> The type of the object
     * @return The object deserialized from the TOML file
     */
    public static <T> T fromToml(Path path, Class<T> clazz) {
        try {
            return tomlMapper.readValue(Files.readString(path, StandardCharsets.UTF_8), clazz);
        } catch (IOException | JacksonException e) {
            throw new OrasException("Unable to read TOML from file", e);
        }
    }
}
