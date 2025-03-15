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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import land.oras.utils.SupportedAlgorithm;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Class for layer
 */
@NullMarked
public final class Layer extends Descriptor {

    /**
     * The base 64 encoded data. Might be null if path is set
     */
    private final @Nullable String data;

    /**
     * The path to the blob. Might be null if data is set
     */
    private final transient @Nullable Path blobPath;

    /**
     * Constructor that can directly set the data
     * Not adapted for large blob due to memory usage but convenient for small data
     * @param mediaType The media type
     * @param digest The digest
     * @param size The size
     */
    private Layer(
            String mediaType,
            String digest,
            long size,
            @Nullable String data,
            @Nullable Map<String, String> annotations) {
        super(digest, size, mediaType, annotations, null, null);
        this.data = data;
        this.blobPath = null;
    }

    /**
     * Constructor that set the data from a file
     * @param mediaType The media type
     * @param digest The digest
     * @param size The size
     * @param blobPath The path to the blob
     */
    private Layer(String mediaType, String digest, long size, Path blobPath, Map<String, String> annotations) {
        super(digest, size, mediaType, annotations, null, null);
        this.data = null;
        this.blobPath = blobPath;
    }

    /**
     * Get the data
     * @return The data
     */
    public @Nullable String getData() {
        return data;
    }

    /**
     * Get the blob path
     * @return The blob path
     */
    public @Nullable Path getBlobPath() {
        return blobPath;
    }

    /**
     * Create a new layer with annotations
     * @param annotations The annotations
     * @return The new layer
     */
    public Layer withAnnotations(Map<String, String> annotations) {
        return new Layer(mediaType, digest, size, data, annotations);
    }

    /**
     * Create a new layer with media type
     * @param mediaType The media type
     * @return The new layer
     */
    public Layer withMediaType(String mediaType) {
        return new Layer(mediaType, digest, size, data, annotations);
    }

    /**
     * Get the data as bytes
     * @return The data as bytes
     */
    public byte[] getDataBytes() {
        if (data != null) {
            return Base64.getDecoder().decode(data);
        } else if (blobPath != null) {
            try {
                return Files.readAllBytes(blobPath);
            } catch (IOException e) {
                throw new OrasException("Failed to read layer data", e);
            }
        }
        throw new OrasException("No data or blob path set");
    }

    /**
     * Create a layer from a JSON string
     * @param json The JSON string
     * @return The manifest
     */
    public static Layer fromJson(String json) {
        return JsonUtils.fromJson(json, Layer.class);
    }

    /**
     * Create a layer from a file using default digest
     * @param file The file
     * @return The layer
     */
    public static Layer fromFile(Path file) {
        return fromFile(file, SupportedAlgorithm.getDefault());
    }

    /**
     * Create a layer from a file using a specific algorithm
     * @param file The file
     * @param algorithm The algorithm
     * @return The layer
     */
    public static Layer fromFile(Path file, SupportedAlgorithm algorithm) {
        Map<String, String> annotations =
                Map.of(Const.ANNOTATION_TITLE, file.getFileName().toString());
        return new Layer(
                Const.DEFAULT_BLOB_MEDIA_TYPE,
                algorithm.digest(file),
                file.toFile().length(),
                file,
                annotations);
    }

    /**
     * Create a layer from data
     * @param containerRef The container reference
     * @param data The data
     * @return The layer
     */
    public static Layer fromData(ContainerRef containerRef, byte[] data) {
        return new Layer(
                Const.DEFAULT_BLOB_MEDIA_TYPE,
                containerRef.getAlgorithm().digest(data),
                data.length,
                Base64.getEncoder().encodeToString(data),
                Map.of());
    }

    /**
     * Create a layer from a digest
     * @param digest The digest
     * @param size The size
     * @return The layer
     */
    public static Layer fromDigest(String digest, long size) {
        return new Layer(Const.DEFAULT_EMPTY_MEDIA_TYPE, digest, size, (String) null, Map.of());
    }

    /**
     * An empty config
     * @return The empty config
     */
    public static Layer empty() {
        return new Layer(
                Const.DEFAULT_EMPTY_MEDIA_TYPE,
                "sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a",
                2,
                "e30=",
                Map.of());
    }
}
