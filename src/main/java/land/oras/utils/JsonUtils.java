package land.oras.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import land.oras.exception.OrasException;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for JSON operations.
 * Use Gson internally for JSON operations
 */
@NullMarked
public final class JsonUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);

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
        return gson.fromJson(json, clazz);
    }

    /**
     * Convert a JSON string to an object
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
        return gson.fromJson(reader, type);
    }
}
