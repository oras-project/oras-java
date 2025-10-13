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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import land.oras.exception.OrasException;
import org.jspecify.annotations.NullMarked;

/**
 * Utility class for JSON operations.
 * Use Gson internally for JSON operations
 */
@NullMarked
public final class JsonUtils {

    /**
     * Gson instance
     */
    private static final Gson gson;

    /**
     * Utils class
     */
    private JsonUtils() {
        // Hide constructor
    }

    /**
     * Type adapter for ZonedDateTime
     */
    private static final class ZonedDateTimeTypeAdapter extends TypeAdapter<ZonedDateTime> {
        @Override
        public void write(JsonWriter out, ZonedDateTime value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public ZonedDateTime read(JsonReader in) throws IOException {
            return ZonedDateTime.parse(in.nextString());
        }
    }

    static {
        gson = new GsonBuilder()
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeTypeAdapter())
                .disableHtmlEscaping()
                .create();
    }

    /**
     * Convert an object to a JSON string
     * @param object The object to convert
     * @return The JSON string
     */
    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    /**
     * Convert a JSON string to an object
     * @param json The JSON string
     * @param clazz The class of the object
     * @param <T> The type of the object
     * @return The object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return gson.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            throw new OrasException("Unable to parse JSON string", e);
        }
    }

    /**
     * Convert a JSON string to an object
     * @param is The JSON input stream
     * @param clazz The class of the object
     * @param <T> The type of the object
     * @return The object
     */
    public static <T> T fromJson(InputStream is, Class<T> clazz) {
        try {
            return jsonMapper.readValue(is, clazz);
        } catch (JacksonException e) {
            throw new OrasException("Unable to parse JSON string", e);
        }
    }

    /**
     * Convert a JSON string to an object. Be careful when using this utility since the original JSON string is lost
     * and might change content digest. Use this method only when the JSON string is not needed anymore or when the digest
     * of the JSON string is not important.
     * @param path The path to the JSON file
     * @param clazz The class of the object
     * @param <T> The type of the object
     * @return The object
     */
    public static <T> T fromJson(Path path, Class<T> clazz) {
        try {
            return gson.fromJson(Files.readString(path, StandardCharsets.UTF_8), clazz);
        } catch (IOException e) {
            throw new OrasException("Unable to read JSON file due to IO error", e);
        } catch (JsonSyntaxException e) {
            throw new OrasException("Unable to parse JSON file", e);
        }
    }

    /**
     * Converts the contents of a JSON file to an object of the specified type.
     *
     * @param path The {@code Path} to the JSON file to be read.
     * @param type The {@code Type} representing the class of the object to be deserialized.
     * @param <T>  The type of the object to be returned.
     * @return An object of type {@code T} deserialized from the JSON file.
     * @throws OrasException If an I/O error occurs while reading the file or the JSON is invalid.
     */
    public static <T> T fromJson(Path path, Type type) {
        try {
            return gson.fromJson(Files.readString(path, StandardCharsets.UTF_8), type);
        } catch (IOException e) {
            throw new OrasException("Unable to read JSON file due to IO error", e);
        } catch (JsonSyntaxException e) {
            throw new OrasException("Unable to parse JSON file", e);
        }
    }

    /**
     * Deserializes the contents of a JSON input to an object of the specified type.
     *
     * @param reader The {@code Reader} from which the JSON content is read.
     * @param type The {@code Type} representing the target object type to be deserialized.
     * @param <T> The type of the object to be returned.
     * @return An object of type {@code T} deserialized from the JSON content.
     * @throws OrasException If an error occurs while reading the input or the JSON format is invalid.
     */
    public static <T> T fromJson(Reader reader, Type type) {
        try {
            return gson.fromJson(reader, type);
        } catch (JsonSyntaxException e) {
            throw new OrasException("Unable to parse JSON content", e);
        }
    }

    /**
     * Read a file and return the content as a string
     * @param path The path to the file
     * @return The content of the file
     */
    public static String readFile(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new OrasException("Unable to read file", e);
        }
    }
}
