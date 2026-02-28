/*-
 * =LICENSE=
 * ORAS Java SDK
 * ===
 * Copyright (C) 2024 - 2026 ORAS
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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Function;
import land.oras.LocalPath;
import land.oras.exception.OrasException;
import org.jspecify.annotations.NullMarked;

/**
 * Supported compression method for archive
 * See @link <a href="https://github.com/opencontainers/image-spec/blob/main/descriptor.md#digests">https://github.com/opencontainers/image-spec/blob/main/descriptor.md#digests</a>
 * See @link <a href="https://github.com/opencontainers/image-spec/blob/main/descriptor.md#registered-algorithms">https://github.com/opencontainers/image-spec/blob/main/descriptor.md#registered-algorithms</a>
 */
@NullMarked
public enum SupportedCompression {

    /**
     * No compression
     */
    NO_COMPRESSION(Const.DEFAULT_BLOB_MEDIA_TYPE, "tar", (localPath -> localPath), (is -> {
        // This is just a tar we need to copy the stream to a temporary file
        try {
            Path temp = ArchiveUtils.createTempTar();
            Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
            return LocalPath.of(temp, Const.DEFAULT_BLOB_MEDIA_TYPE);
        } catch (Exception e) {
            throw new OrasException("Failed to copy stream to temporary file", e);
        }
    })),

    /**
     * ZIP
     */
    ZIP(Const.ZIP_MEDIA_TYPE, "zip", ArchiveUtils::zip, ArchiveUtils::unzip),

    /**
     * GZIP
     */
    GZIP(Const.DEFAULT_BLOB_DIR_MEDIA_TYPE, "gz", ArchiveUtils::compressGzip, ArchiveUtils::uncompressGzip),

    /**
     * ZSTD
     */
    ZSTD(Const.BLOB_DIR_ZSTD_MEDIA_TYPE, "zst", ArchiveUtils::compressZstd, ArchiveUtils::uncompressZstd);

    /**
     * The media type
     */
    private final String mediaType;

    /**
     * The file extension
     */
    private final String fileExtension;

    /**
     * The compress function
     */
    private final Function<LocalPath, LocalPath> compressFunction;

    /**
     * The uncompress function
     */
    private final Function<InputStream, LocalPath> uncompressFunction;

    /**
     * Get the supported compression
     * @param mediaType The media type
     */
    SupportedCompression(
            String mediaType,
            String fileExtension,
            Function<LocalPath, LocalPath> compressFunction,
            Function<InputStream, LocalPath> uncompressFunction) {
        this.mediaType = mediaType;
        this.fileExtension = fileExtension;
        this.compressFunction = compressFunction;
        this.uncompressFunction = uncompressFunction;
    }

    /**
     * Get the file extension
     * @return The file extension
     */
    public String getFileExtension() {
        return fileExtension;
    }

    /**
     * Whether the media type is auto unpacked (it's an image layer, not whatever media type the user specified)
     * @return True if the media type is auto unpacked by OCI runtime, false otherwise
     */
    public boolean isAutoUnpack() {
        return getMediaType().startsWith("application/vnd.oci.image.layer.v1.tar");
    }

    /**
     * Get the media type
     * @return The media type
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * Compress
     * @param path The path
     * @return The compressed path
     */
    LocalPath compress(LocalPath path) {
        return compressFunction.apply(path);
    }

    /**
     * Uncompress
     * @param inputStream The input stream
     * @return The uncompressed path
     */
    LocalPath uncompress(InputStream inputStream) {
        return uncompressFunction.apply(inputStream);
    }

    /**
     * Get the algorithm from a digest
     * @param mediaType The media type
     * @return The supported algorithm
     */
    public static SupportedCompression fromMediaType(String mediaType) {
        for (SupportedCompression compression : SupportedCompression.values()) {
            if (mediaType.equalsIgnoreCase(compression.getMediaType())) {
                return compression;
            }
        }
        throw new OrasException("Unsupported mediaType: " + mediaType);
    }
}
