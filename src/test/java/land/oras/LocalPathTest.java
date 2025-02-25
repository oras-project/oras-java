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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import land.oras.utils.Const;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class LocalPathTest {

    @TempDir
    private Path blobDir;

    @Test
    void shouldReadFromExpression() throws IOException {
        LocalPath localPath = LocalPath.of("path/to/file:application/json");
        assertEquals("file", localPath.getPath().getFileName().toString());
        assertEquals("application/json", localPath.getMediaType());

        localPath = LocalPath.of(blobDir);

        // Directory default
        assertEquals(
                blobDir.getFileName().toString(),
                localPath.getPath().getFileName().toString());
        assertEquals(Const.DEFAULT_BLOB_DIR_MEDIA_TYPE, localPath.getMediaType());

        // File default
        Files.createFile(blobDir.resolve("file"));
        localPath = LocalPath.of(blobDir.resolve("file"));
        assertEquals("file", localPath.getPath().getFileName().toString());
        assertEquals(Const.DEFAULT_BLOB_MEDIA_TYPE, localPath.getMediaType());
    }
}
