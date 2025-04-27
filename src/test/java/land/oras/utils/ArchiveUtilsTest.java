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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import land.oras.LocalPath;
import land.oras.exception.OrasException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
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
    private static Path targetGzDir;

    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    private static Path targetZstdDir;

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
    void testEnsureSafeEntry() throws Exception {
        TarArchiveEntry entry = mock(TarArchiveEntry.class);
        doReturn("test").when(entry).getName();
        ArchiveUtils.ensureSafeEntry(entry, archiveDir);
    }

    @Test
    void throwOnUnsafeEntries() throws Exception {
        TarArchiveEntry entry = mock(TarArchiveEntry.class);

        // Simulate a path traversal attack
        assertThrows(IOException.class, () -> {
            doReturn("/").when(entry).getName();
            ArchiveUtils.ensureSafeEntry(entry, archiveDir);
        });
        assertThrows(IOException.class, () -> {
            doReturn("foo/bar/../../../test").when(entry).getName();
            ArchiveUtils.ensureSafeEntry(entry, archiveDir);
        });
    }

    @Test
    void shouldFailWithUnknownDirectories() {
        assertThrows(OrasException.class, () -> {
            ArchiveUtils.untar(Path.of("unknown"), Path.of("foo"));
        });
        assertThrows(OrasException.class, () -> {
            ArchiveUtils.uncompressuntar(Path.of("unknown"), SupportedCompression.GZIP.getMediaType());
        });
        assertThrows(OrasException.class, () -> {
            ArchiveUtils.tar(LocalPath.of("foo"));
        });
        assertThrows(OrasException.class, () -> {
            ArchiveUtils.tarcompress(LocalPath.of("foo"), SupportedCompression.ZSTD.getMediaType());
        });
    }

    @Test
    void shouldCreateTarGzAndExtractIt() throws Exception {
        LocalPath directory = LocalPath.of(archiveDir);
        LocalPath archive = ArchiveUtils.tar(LocalPath.of(archiveDir));
        LOG.info("Archive created: {}", archive);
        Path compressedArchive = ArchiveUtils.tarcompress(LocalPath.of(archiveDir), directory.getMediaType())
                .getPath();

        assertTrue(Files.exists(compressedArchive), "Archive should exist");

        ArchiveUtils.uncompressuntar(compressedArchive, targetGzDir, directory.getMediaType());

        // Untar to temporary
        Path tmp = ArchiveUtils.untar(archive.getPath());
        assertTrue(Files.exists(tmp), "Temp should exist");
        assertTrue(Files.exists(tmp.resolve("dir1")), "dir1 should exist");

        // Ensure all files are extracted
        assertTrue(Files.exists(targetGzDir.resolve("dir1")), "dir1 should exist");
        assertTrue(Files.exists(targetGzDir.resolve("dir2")), "dir2 should exist");
        assertTrue(Files.exists(targetGzDir.resolve("dir1").resolve("file1")), "file1 should exist");
        assertTrue(Files.exists(targetGzDir.resolve("dir2").resolve("file2")), "file2 should exist");
        assertTrue(Files.exists(targetGzDir.resolve("dir1").resolve("file3")), "file3 should exist");
        assertTrue(Files.exists(targetGzDir.resolve("dir2").resolve("dir3")), "dir3 should exist");
        assertTrue(Files.exists(targetGzDir.resolve("dir2").resolve("dir3").resolve("file4")), "file4 should exist");

        // Empty directory
        assertTrue(Files.exists(targetGzDir.resolve("empty")), "empty should exist");

        // Assert file content
        assertTrue(
                Files.readString(targetGzDir.resolve("dir1").resolve("file1")).equals("file1"),
                "file1 content should match");
        assertTrue(
                Files.readString(targetGzDir.resolve("dir2").resolve("file2")).equals("file2"),
                "file2 content should match");
        assertTrue(
                Files.readString(targetGzDir.resolve("dir2").resolve("dir3").resolve("file4"))
                        .equals("file4"),
                "file4 content should match");

        // Ensure symlink is extracted
        assertTrue(Files.isSymbolicLink(targetGzDir.resolve("dir1").resolve("file3")), "file3 should be symlink");

        // To temporary
        Path temp = ArchiveUtils.uncompressuntar(compressedArchive, directory.getMediaType());
        assertTrue(Files.exists(temp), "Temp should exist");
    }

    @Test
    void shouldCreateTarZstdAndExtractIt() throws Exception {
        LocalPath directory = LocalPath.of(archiveDir, Const.BLOB_DIR_ZSTD_MEDIA_TYPE);
        LocalPath archive = ArchiveUtils.tar(directory);
        LOG.info("Archive created: {}", archive);
        Path compressedArchive =
                ArchiveUtils.compress(archive, directory.getMediaType()).getPath();

        assertTrue(Files.exists(compressedArchive), "Archive should exist");

        Path uncompressedArchive = ArchiveUtils.uncompress(
                        Files.newInputStream(compressedArchive), Const.BLOB_DIR_ZSTD_MEDIA_TYPE)
                .getPath();
        ArchiveUtils.untar(Files.newInputStream(uncompressedArchive), targetZstdDir);

        // Untar to temporary
        Path tmp = ArchiveUtils.untar(archive.getPath());
        assertTrue(Files.exists(tmp), "Temp should exist");
        assertTrue(Files.exists(tmp.resolve("dir1")), "dir1 should exist");

        // Ensure all files are extracted
        assertTrue(Files.exists(targetZstdDir.resolve("dir1")), "dir1 should exist");
        assertTrue(Files.exists(targetZstdDir.resolve("dir2")), "dir2 should exist");
        assertTrue(Files.exists(targetZstdDir.resolve("dir1").resolve("file1")), "file1 should exist");
        assertTrue(Files.exists(targetZstdDir.resolve("dir2").resolve("file2")), "file2 should exist");
        assertTrue(Files.exists(targetZstdDir.resolve("dir1").resolve("file3")), "file3 should exist");
        assertTrue(Files.exists(targetZstdDir.resolve("dir2").resolve("dir3")), "dir3 should exist");
        assertTrue(Files.exists(targetZstdDir.resolve("dir2").resolve("dir3").resolve("file4")), "file4 should exist");

        // Empty directory
        assertTrue(Files.exists(targetZstdDir.resolve("empty")), "empty should exist");

        // Assert file content
        assertTrue(
                Files.readString(targetZstdDir.resolve("dir1").resolve("file1")).equals("file1"),
                "file1 content should match");
        assertTrue(
                Files.readString(targetZstdDir.resolve("dir2").resolve("file2")).equals("file2"),
                "file2 content should match");
        assertTrue(
                Files.readString(targetZstdDir.resolve("dir2").resolve("dir3").resolve("file4"))
                        .equals("file4"),
                "file4 content should match");

        // Ensure symlink is extracted
        assertTrue(Files.isSymbolicLink(targetZstdDir.resolve("dir1").resolve("file3")), "file3 should be symlink");

        // To temporary
        Path temp = ArchiveUtils.uncompressuntar(compressedArchive, directory.getMediaType());
        assertTrue(Files.exists(temp), "Temp should exist");
    }
}
