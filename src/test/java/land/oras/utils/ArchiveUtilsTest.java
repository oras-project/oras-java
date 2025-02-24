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
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
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
    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    private static Path archiveDir;

    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    private static Path targetDir;

    @BeforeAll
    static void beforeAll() throws Exception {
        // Create directory structure with few files
        Path dir1 = archiveDir.resolve("dir1");
        Files.createDirectory(dir1);
        Path dir2 = archiveDir.resolve("dir2");
        Files.createDirectory(dir2);
        Path dir3 = Files.createDirectory(dir2.resolve("dir3"));

        // Empty directory
        Files.createDirectory(archiveDir.resolve("empty"));

        Path file1 = dir1.resolve("file1");
        Path file2 = dir2.resolve("file2");
        Path file4 = dir3.resolve("file4");

        // Write some content to the files
        Files.writeString(file1, "file1");
        Files.writeString(file2, "file2");
        Files.writeString(file4, "file4");

        // Create one symlink file3 -> file1
        Path file3 = dir1.resolve("file3");
        Files.createSymbolicLink(file3, file1);

        // Add 777 permission to file2
        Files.setPosixFilePermissions(
                file2,
                Set.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.GROUP_WRITE,
                        PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_READ,
                        PosixFilePermission.OTHERS_WRITE,
                        PosixFilePermission.OTHERS_EXECUTE));
    }

    @Test
    void shouldCreateTarGzAndExtractIt() throws Exception {
        Path archive = ArchiveUtils.createTar(archiveDir);
        LOG.info("Archive created: {}", archive);
        Path compressedArchive = ArchiveUtils.compressGzip(archive);

        assertTrue(Files.exists(compressedArchive), "Archive should exist");

        Path uncompressedArchive = ArchiveUtils.uncompressGzip(Files.newInputStream(compressedArchive));
        ArchiveUtils.extractTar(Files.newInputStream(uncompressedArchive), targetDir);

        // Ensure all files are extracted
        assertTrue(Files.exists(targetDir.resolve("dir1")), "dir1 should exist");
        assertTrue(Files.exists(targetDir.resolve("dir2")), "dir2 should exist");
        assertTrue(Files.exists(targetDir.resolve("dir1").resolve("file1")), "file1 should exist");
        assertTrue(Files.exists(targetDir.resolve("dir2").resolve("file2")), "file2 should exist");
        assertTrue(Files.exists(targetDir.resolve("dir1").resolve("file3")), "file3 should exist");
        assertTrue(Files.exists(targetDir.resolve("dir2").resolve("dir3")), "dir3 should exist");
        assertTrue(Files.exists(targetDir.resolve("dir2").resolve("dir3").resolve("file4")), "file4 should exist");

        // Empty directory
        assertTrue(Files.exists(targetDir.resolve("empty")), "empty should exist");

        // Assert file content
        assertTrue(
                Files.readString(targetDir.resolve("dir1").resolve("file1")).equals("file1"),
                "file1 content should match");
        assertTrue(
                Files.readString(targetDir.resolve("dir2").resolve("file2")).equals("file2"),
                "file2 content should match");
        assertTrue(
                Files.readString(targetDir.resolve("dir2").resolve("dir3").resolve("file4"))
                        .equals("file4"),
                "file4 content should match");

        // Ensure symlink is extracted
        assertTrue(Files.isSymbolicLink(targetDir.resolve("dir1").resolve("file3")), "file3 should be symlink");
    }
}
