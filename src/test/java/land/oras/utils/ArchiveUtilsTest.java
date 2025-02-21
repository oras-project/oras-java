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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Execution(ExecutionMode.CONCURRENT)
public class ArchiveUtilsTest {

    /**
     * Logger
     */
    private static Logger LOG = LoggerFactory.getLogger(ArchiveUtilsTest.class);

    /**
     * Archive temporary dir
     */
    @TempDir
    private static Path archiveDir;

    @TempDir
    private static Path targetDir;

    @BeforeAll
    static void beforeAll() throws Exception {
        // Create directory structure with few files
        Path dir1 = archiveDir.resolve("dir1");
        Files.createDirectory(dir1);
        Path dir2 = archiveDir.resolve("dir2");
        Files.createDirectory(dir2);
        Path file1 = dir1.resolve("file1");
        Path file2 = dir2.resolve("file2");

        // Write some content to the files
        Files.writeString(file1, "file1");
        Files.writeString(file2, "file2");
    }

    @Test
    void shouldCreateTarGzAndExtractIt() throws Exception {
        Path archive = ArchiveUtils.createTarGz(archiveDir);
        LOG.info("Archive created: {}", archive);

        assertTrue(Files.exists(archive), "Archive should exist");

        ArchiveUtils.extractTarGz(archive, targetDir);

        // Ensure all files are extracted
        assertTrue(Files.exists(targetDir.resolve("dir1")), "dir1 should exist");
        assertTrue(Files.exists(targetDir.resolve("dir2")), "dir2 should exist");
        assertTrue(Files.exists(targetDir.resolve("dir1").resolve("file1")), "file1 should exist");
        assertTrue(Files.exists(targetDir.resolve("dir2").resolve("file2")), "file2 should exist");
    }
}
