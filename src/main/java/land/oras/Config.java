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
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Class for config
 */
@NullUnmarked
public final class Config {

    private final String mediaType;
    private final String digest;
    private final long size;

    /**
     * Annotations for the layer
     * Can be nullable due to serialization
     */
    private final @Nullable Map<String, String> annotations;

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
        this.mediaType = mediaType;
        this.digest = digest;
        this.size = size;
        this.data = data;
        // Config annotation are generally empty since not default annotations are added by ORAS
        if (!annotations.configAnnotations().isEmpty()) {
            this.annotations = Map.copyOf(annotations.configAnnotations());
        } else {
            this.annotations = null;
        }
    }

    /**
     * Get the media type
     * @return The media type
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * Get the digest
     * @return The digest
     */
    public String getDigest() {
        return digest;
    }

    /**
     * Get the size
     * @return The size
     */
    public long getSize() {
        return size;
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
     * Get the annotations
     * @return The annotations
     */
    public @Nullable Map<String, String> getAnnotations() {
        return annotations;
    }

    /**
     * Return the JSON representation of the manifest
     * @return The JSON string
     */
    public String toJson() {
        return JsonUtils.toJson(this);
    }

    /**
     * Create a manifest from a JSON string
     * @param json The JSON string
     * @return The manifest
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
}
