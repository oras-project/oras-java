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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Class for config
 */
@NullUnmarked
@OrasModel
public final class Config extends Descriptor {

    /**
     * The base 64 encoded data
     */
    private final @Nullable String data;

    /**
     * Constructor
     * @param mediaType The media type
     * @param digest The digest
     * @param size The size
     */
    private Config(String mediaType, String digest, long size, @Nullable String data, Annotations annotations) {
        super(
                digest,
                size,
                mediaType,
                !annotations.configAnnotations().isEmpty() ? Map.copyOf(annotations.configAnnotations()) : null,
                null,
                null);
        this.data = data;
    }

    /**
     * Get the annotations
     * @return The annotations
     */
    @Override
    public @Nullable Map<String, String> getAnnotations() {
        return annotations;
    }

    /**
     * Create a new config with annotations
     * @param annotations The annotations
     * @return The new config
     */
    public Config withAnnotations(Annotations annotations) {
        return new Config(mediaType, digest, size, data, annotations);
    }

    /**
     * Create a new config with media type
     * @param mediaType The media type
     * @return The new config
     */
    public Config withMediaType(String mediaType) {
        return new Config(
                mediaType,
                digest,
                size,
                data,
                annotations == null ? Annotations.empty() : Annotations.ofConfig(annotations));
    }

    /**
     * Get the data as bytes
     * @return The data as bytes
     */
    public byte[] getDataBytes() {
        if (data != null) {
            return Base64.getDecoder().decode(data);
        }
        return "{}".getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Get the data as a string
     * @return The data as a string
     */
    public @Nullable String getData() {
        return data;
    }

    /**
     * Create a config from a JSON string
     * @param json The JSON string
     * @return The config
     */
    public static Config fromJson(String json) {
        return JsonUtils.fromJson(json, Config.class);
    }

    /**
     * An empty config
     * @return The empty config
     */
    public static Config empty() {
        return new Config(
                Const.DEFAULT_EMPTY_MEDIA_TYPE,
                "sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a",
                2,
                "e30=",
                Annotations.empty());
    }

    /**
     * A config with referrence on a blob (too large for data)
     * @param mediaType The media type
     * @param layer The layer
     * @return The config
     */
    public static Config fromBlob(String mediaType, Layer layer) {
        return new Config(mediaType, layer.getDigest(), layer.getSize(), null, Annotations.empty());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Config config = (Config) o;
        return Objects.equals(toJson(), config.toJson());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(toJson());
    }

    @Override
    public String toString() {
        return toJson();
    }
}
