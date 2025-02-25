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

import java.nio.file.Files;
import java.nio.file.Path;
import land.oras.utils.Const;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Local path
 */
@NullMarked
public class LocalPath {

    @Nullable
    private final String mediaType;

    private final Path path;

    private LocalPath(@Nullable String mediaType, Path path) {
        this.mediaType = mediaType;
        this.path = path;
    }

    /**
     * New local path with no media type
     * @param path The path
     * @return The local path
     */
    public static LocalPath of(Path path) {
        return new LocalPath(null, path);
    }

    /**
     * New path with no media type
     * @param path The path
     * @param mediaType The media type
     * @return The local path
     */
    public static LocalPath of(Path path, String mediaType) {
        return new LocalPath(mediaType, path);
    }

    /**
     * New path with path:mediaType (for example /path/to/file.json:application/json)
     * @param expression The expression
     * @return The local path
     */
    public static LocalPath of(String expression) {
        int index = expression.lastIndexOf(':');
        if (index == -1) {
            return new LocalPath(null, Path.of(expression));
        }
        return new LocalPath(expression.substring(index + 1), Path.of(expression.substring(0, index)));
    }

    /**
     * Get the media type or default depending on directory or standard file
     * @return The media type
     */
    public String getMediaType() {
        return mediaType != null
                ? mediaType
                : Files.isDirectory(path) ? Const.DEFAULT_BLOB_DIR_MEDIA_TYPE : Const.DEFAULT_BLOB_MEDIA_TYPE;
    }

    /**
     * Get the path
     * @return The path
     */
    public Path getPath() {
        return path;
    }
}
