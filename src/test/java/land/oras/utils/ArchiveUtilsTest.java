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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import land.oras.LocalPath;
import land.oras.exception.OrasException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.AsiExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Execution(ExecutionMode.CONCURRENT)
class ArchiveUtilsTest {

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
    private static Path targetZipDir;

    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    private static Path targetZstdDir;

    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    private static Path existingArchiveDir;

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

        // Create one symlink file3 -> file1 (use a relative target so the extracted
        // symlink stays inside the extraction directory; absolute targets would escape)
        if (OsUtils.isPosixFileSystemSupported()) {
            Path file3 = dir1.resolve("file3");
            Files.createSymbolicLink(file3, Paths.get("file1"));

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
    void shouldFailWithUnknownDirectoriesForTar() {
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
    void shouldFailWithUnknownDirectoriesForZip() {
        assertThrows(OrasException.class, () -> {
            ArchiveUtils.unzip(Path.of("unknown"), Path.of("foo"));
        });
        assertThrows(OrasException.class, () -> {
            ArchiveUtils.zip(LocalPath.of("foo"));
        });
    }

    @Test
    void shouldCreateZipAndExtractIt() throws Exception {
        LocalPath directory = LocalPath.of(archiveDir);
        LocalPath archive = ArchiveUtils.zip(directory);
        LOG.info("Archive created: {}", archive);
        assertEquals(Const.ZIP_MEDIA_TYPE, archive.getMediaType());
        assertTrue(Files.exists(archive.getPath()), "Archive should exist");

        ArchiveUtils.unzip(archive.getPath(), targetZipDir);

        // Ensure all files are extracted
        Path extractedDir = targetZipDir.resolve(directory.getPath().getFileName());
        assertTrue(Files.exists(extractedDir.resolve("dir1")), "dir1 should exist");
        assertTrue(Files.exists(extractedDir.resolve("dir2")), "dir2 should exist");
        assertTrue(Files.exists(extractedDir.resolve("dir1").resolve("file1")), "file1 should exist");
        assertTrue(Files.exists(extractedDir.resolve("dir2").resolve("file2")), "file2 should exist");
        if (OsUtils.isPosixFileSystemSupported()) {
            assertTrue(Files.exists(extractedDir.resolve("dir1").resolve("file3")), "file3 should exist");
        }
        assertTrue(Files.exists(extractedDir.resolve("dir2").resolve("dir3")), "dir3 should exist");
        assertTrue(Files.exists(extractedDir.resolve("dir2").resolve("dir3").resolve("file4")), "file4 should exist");

        // Empty directory
        assertTrue(Files.exists(extractedDir.resolve("empty")), "empty should exist");

        // Assert file content
        assertTrue(
                Files.readString(extractedDir.resolve("dir1").resolve("file1")).equals("file1"),
                "file1 content should match");
        assertTrue(
                Files.readString(extractedDir.resolve("dir2").resolve("file2")).equals("file2"),
                "file2 content should match");
        assertTrue(
                Files.readString(extractedDir.resolve("dir2").resolve("dir3").resolve("file4"))
                        .equals("file4"),
                "file4 content should match");
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
        assertTrue(Files.exists(tmp.resolve(directory.getPath().getFileName()).resolve("dir1")), "dir1 should exist");

        // Ensure all files are extracted
        Path extractedDir = targetGzDir.resolve(directory.getPath().getFileName());
        assertTrue(Files.exists(extractedDir.resolve("dir1")), "dir1 should exist");
        assertTrue(Files.exists(extractedDir.resolve("dir2")), "dir2 should exist");
        assertTrue(Files.exists(extractedDir.resolve("dir1").resolve("file1")), "file1 should exist");
        assertTrue(Files.exists(extractedDir.resolve("dir2").resolve("file2")), "file2 should exist");
        if (OsUtils.isPosixFileSystemSupported()) {
            assertTrue(Files.exists(extractedDir.resolve("dir1").resolve("file3")), "file3 should exist");
        }
        assertTrue(Files.exists(extractedDir.resolve("dir2").resolve("dir3")), "dir3 should exist");
        assertTrue(Files.exists(extractedDir.resolve("dir2").resolve("dir3").resolve("file4")), "file4 should exist");

        // Empty directory
        assertTrue(Files.exists(extractedDir.resolve("empty")), "empty should exist");

        // Assert file content
        assertTrue(
                Files.readString(extractedDir.resolve("dir1").resolve("file1")).equals("file1"),
                "file1 content should match");
        assertTrue(
                Files.readString(extractedDir.resolve("dir2").resolve("file2")).equals("file2"),
                "file2 content should match");
        assertTrue(
                Files.readString(extractedDir.resolve("dir2").resolve("dir3").resolve("file4"))
                        .equals("file4"),
                "file4 content should match");

        // Ensure symlink is extracted
        if (OsUtils.isPosixFileSystemSupported()) {
            assertTrue(Files.isSymbolicLink(extractedDir.resolve("dir1").resolve("file3")), "file3 should be symlink");
        }

        // To temporary
        Path temp = ArchiveUtils.uncompressuntar(compressedArchive, directory.getMediaType());
        assertTrue(Files.exists(temp), "Temp should exist");
    }

    @ParameterizedTest
    @ValueSource(strings = {"jenkins-chart.tgz", "jenkins-sources.tar.gz", "flux-manifests.tgz"})
    @Disabled("https://issues.apache.org/jira/browse/COMPRESS-705")
    void shouldExtractSeveralExistingArchive(String file) {
        Path archive = Paths.get("src/test/resources/archives").resolve(file);
        assertNotNull(archive, "Archive should exist");
        assertTrue(Files.exists(archive), "Archive should exist");
        ArchiveUtils.uncompressuntar(archive, existingArchiveDir, SupportedCompression.GZIP.getMediaType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"terraform-provider-random_3.8.1_linux_amd64.zip"})
    void shouldExtractExistingZipArchive(String file) {
        Path archive = Paths.get("src/test/resources/archives").resolve(file);
        assertNotNull(archive, "Archive should exist");
        assertTrue(Files.exists(archive), "Archive should exist");
        ArchiveUtils.unzip(archive, existingArchiveDir);
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
        assertTrue(Files.exists(tmp.resolve(directory.getPath().getFileName()).resolve("dir1")), "dir1 should exist");

        // Ensure all files are extracted
        Path extractedDir = targetZstdDir.resolve(directory.getPath().getFileName());
        assertTrue(Files.exists(extractedDir.resolve("dir1")), "dir1 should exist");
        assertTrue(Files.exists(extractedDir.resolve("dir2")), "dir2 should exist");
        assertTrue(Files.exists(extractedDir.resolve("dir1").resolve("file1")), "file1 should exist");
        assertTrue(Files.exists(extractedDir.resolve("dir2").resolve("file2")), "file2 should exist");
        if (OsUtils.isPosixFileSystemSupported()) {
            assertTrue(Files.exists(extractedDir.resolve("dir1").resolve("file3")), "file3 should exist");
        }
        assertTrue(Files.exists(extractedDir.resolve("dir2").resolve("dir3")), "dir3 should exist");
        assertTrue(Files.exists(extractedDir.resolve("dir2").resolve("dir3").resolve("file4")), "file4 should exist");

        // Empty directory
        assertTrue(Files.exists(extractedDir.resolve("empty")), "empty should exist");

        // Assert file content
        assertTrue(
                Files.readString(extractedDir.resolve("dir1").resolve("file1")).equals("file1"),
                "file1 content should match");
        assertTrue(
                Files.readString(extractedDir.resolve("dir2").resolve("file2")).equals("file2"),
                "file2 content should match");
        assertTrue(
                Files.readString(extractedDir.resolve("dir2").resolve("dir3").resolve("file4"))
                        .equals("file4"),
                "file4 content should match");

        // Ensure symlink is extracted
        if (OsUtils.isPosixFileSystemSupported()) {
            assertTrue(Files.isSymbolicLink(extractedDir.resolve("dir1").resolve("file3")), "file3 should be symlink");
        }

        // To temporary
        Path temp = ArchiveUtils.uncompressuntar(compressedArchive, directory.getMediaType());
        assertTrue(Files.exists(temp), "Temp should exist");
    }

    /**
     * Verify that untar rejects a tar that plants a symlink whose target escapes
     * the extraction directory, even when the entry name itself stays under target.
     * A second regular-file entry whose name traverses through the planted symlink
     * would otherwise resolve to a path outside target.
     */
    @Test
    void shouldRejectSymlinkEscapingTargetOnUntar(@TempDir Path tmp) throws IOException {
        if (!OsUtils.isPosixFileSystemSupported()) {
            return;
        }
        Path target = tmp.resolve("safe-output");
        Files.createDirectories(target);
        Path escapeFile = tmp.resolve("ESCAPED.txt");
        Files.deleteIfExists(escapeFile);

        Path mtar = tmp.resolve("malicious.tar");
        try (TarArchiveOutputStream tout = new TarArchiveOutputStream(Files.newOutputStream(mtar))) {
            tout.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            TarArchiveEntry symlinkEntry = new TarArchiveEntry("evil-link", TarArchiveEntry.LF_SYMLINK);
            symlinkEntry.setLinkName(tmp.toAbsolutePath().toString());
            symlinkEntry.setMode(0777);
            tout.putArchiveEntry(symlinkEntry);
            tout.closeArchiveEntry();

            byte[] data = "should not land outside target\n".getBytes();
            TarArchiveEntry fileEntry = new TarArchiveEntry("evil-link/ESCAPED.txt");
            fileEntry.setSize(data.length);
            fileEntry.setMode(0644);
            tout.putArchiveEntry(fileEntry);
            tout.write(data);
            tout.closeArchiveEntry();
        }

        assertThrows(OrasException.class, () -> ArchiveUtils.untar(mtar, target));
        assertFalse(Files.exists(escapeFile), "Symlink-target escape must not create a file outside target");
    }

    /**
     * GHSA-f7cp-5f43-6jcx: ensureSafeSymlinkTarget validates a symlink target against a purely
     * lexical path model. A first entry "a" -> "." plants a symlink pointing back at the
     * extraction root; a second entry "a/b" -> ".." is then lexically computed as
     * target/a/.. (normalizes to target, in bounds) even though the real filesystem resolves
     * it to the parent of the extraction root, because "a" is itself a symlink to the root. A
     * third, regular-file entry "a/b/ESCAPED.txt" then writes through that chain and lands
     * outside the extraction directory.
     */
    @Test
    void shouldRejectChainedSymlinkEscapeOnUntar(@TempDir Path tmp) throws IOException {
        if (!OsUtils.isPosixFileSystemSupported()) {
            return;
        }
        Path target = tmp.resolve("extract");
        Files.createDirectories(target);
        Path escapeFile = tmp.resolve("ESCAPED.txt");
        Files.deleteIfExists(escapeFile);

        Path mtar = tmp.resolve("malicious-chain.tar");
        try (TarArchiveOutputStream tout = new TarArchiveOutputStream(Files.newOutputStream(mtar))) {
            tout.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            TarArchiveEntry s1 = new TarArchiveEntry("a", TarArchiveEntry.LF_SYMLINK);
            s1.setLinkName(".");
            tout.putArchiveEntry(s1);
            tout.closeArchiveEntry();

            TarArchiveEntry s2 = new TarArchiveEntry("a/b", TarArchiveEntry.LF_SYMLINK);
            s2.setLinkName("..");
            tout.putArchiveEntry(s2);
            tout.closeArchiveEntry();

            byte[] data = "escaped-via-symlink-chain\n".getBytes(StandardCharsets.UTF_8);
            TarArchiveEntry f = new TarArchiveEntry("a/b/ESCAPED.txt");
            f.setSize(data.length);
            tout.putArchiveEntry(f);
            tout.write(data);
            tout.closeArchiveEntry();
        }

        assertThrows(OrasException.class, () -> ArchiveUtils.untar(mtar, target));
        assertFalse(Files.exists(escapeFile), "Chained symlink escape must not create a file outside target");
    }

    /**
     * Two-level variant of {@link #shouldRejectChainedSymlinkEscapeOnUntar}: each additional
     * in-bounds symlink pair climbs one more ancestor directory, confirming the guard isn't
     * merely bounding the escape depth at one level.
     */
    @Test
    void shouldRejectMultiLevelChainedSymlinkEscapeOnUntar(@TempDir Path tmp) throws IOException {
        if (!OsUtils.isPosixFileSystemSupported()) {
            return;
        }
        Path target = tmp.resolve("extract");
        Files.createDirectories(target);
        Path escapeFile = tmp.getParent().resolve("DEEP.txt");
        Files.deleteIfExists(escapeFile);

        Path mtar = tmp.resolve("malicious-deep-chain.tar");
        try (TarArchiveOutputStream tout = new TarArchiveOutputStream(Files.newOutputStream(mtar))) {
            tout.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            String[][] links = {{"a", "."}, {"a/b", ".."}, {"a/b/c", "."}, {"a/b/c/d", ".."}};
            for (String[] link : links) {
                TarArchiveEntry s = new TarArchiveEntry(link[0], TarArchiveEntry.LF_SYMLINK);
                s.setLinkName(link[1]);
                tout.putArchiveEntry(s);
                tout.closeArchiveEntry();
            }

            byte[] data = "escaped-two-levels-up\n".getBytes(StandardCharsets.UTF_8);
            TarArchiveEntry f = new TarArchiveEntry("a/b/c/d/DEEP.txt");
            f.setSize(data.length);
            tout.putArchiveEntry(f);
            tout.write(data);
            tout.closeArchiveEntry();
        }

        assertThrows(OrasException.class, () -> ArchiveUtils.untar(mtar, target));
        assertFalse(
                Files.exists(escapeFile), "Multi-level chained symlink escape must not create a file outside target");
    }

    /**
     * Same chained-symlink escape as {@link #shouldRejectChainedSymlinkEscapeOnUntar} but via
     * the zip extraction path, which shares the same lexical ensureSafeSymlinkTarget guard.
     */
    @Test
    void shouldRejectChainedSymlinkEscapeOnUnzip(@TempDir Path tmp) throws IOException {
        if (!OsUtils.isPosixFileSystemSupported()) {
            return;
        }
        Path target = tmp.resolve("extract");
        Files.createDirectories(target);
        Path escapeFile = tmp.resolve("ESCAPED.txt");
        Files.deleteIfExists(escapeFile);

        Path mzip = tmp.resolve("malicious-chain.zip");
        try (ZipArchiveOutputStream zout = new ZipArchiveOutputStream(Files.newOutputStream(mzip))) {
            // Symlink metadata must travel in an AsiExtraField (local-header extra field), not
            // just the central-directory external attributes set by setUnixMode(): unzip() reads
            // entries via a streaming ZipArchiveInputStream that never sees the central directory.
            ZipArchiveEntry s1 = new ZipArchiveEntry("a");
            AsiExtraField asi1 = new AsiExtraField();
            asi1.setLinkedFile(".");
            asi1.setMode(0120755);
            s1.addExtraField(asi1);
            s1.setSize(0);
            zout.putArchiveEntry(s1);
            zout.closeArchiveEntry();

            ZipArchiveEntry s2 = new ZipArchiveEntry("a/b");
            AsiExtraField asi2 = new AsiExtraField();
            asi2.setLinkedFile("..");
            asi2.setMode(0120755);
            s2.addExtraField(asi2);
            s2.setSize(0);
            zout.putArchiveEntry(s2);
            zout.closeArchiveEntry();

            byte[] data = "escaped-via-symlink-chain\n".getBytes(StandardCharsets.UTF_8);
            ZipArchiveEntry f = new ZipArchiveEntry("a/b/ESCAPED.txt");
            f.setSize(data.length);
            zout.putArchiveEntry(f);
            zout.write(data);
            zout.closeArchiveEntry();
        }

        assertThrows(OrasException.class, () -> ArchiveUtils.unzip(mzip, target));
        assertFalse(Files.exists(escapeFile), "Chained symlink escape must not create a file outside target");
    }

    @Test
    void shouldUntarOverwriteExistingFiles(@TempDir Path tmp) throws IOException {
        Path target = tmp.resolve("output");
        Files.createDirectories(target);

        byte[] firstContent = "first".getBytes();
        byte[] secondContent = "second-overwritten".getBytes();

        Path tar1 = tmp.resolve("first.tar");
        try (TarArchiveOutputStream tout = new TarArchiveOutputStream(Files.newOutputStream(tar1))) {
            TarArchiveEntry e = new TarArchiveEntry("file.txt");
            e.setSize(firstContent.length);
            tout.putArchiveEntry(e);
            tout.write(firstContent);
            tout.closeArchiveEntry();
        }
        ArchiveUtils.untar(tar1, target);
        assertEquals("first", Files.readString(target.resolve("file.txt")));

        Path tar2 = tmp.resolve("second.tar");
        try (TarArchiveOutputStream tout = new TarArchiveOutputStream(Files.newOutputStream(tar2))) {
            TarArchiveEntry e = new TarArchiveEntry("file.txt");
            e.setSize(secondContent.length);
            tout.putArchiveEntry(e);
            tout.write(secondContent);
            tout.closeArchiveEntry();
        }
        ArchiveUtils.untar(tar2, target);
        assertEquals("second-overwritten", Files.readString(target.resolve("file.txt")));
    }

    @Test
    void shouldUnzipOverwriteExistingFiles(@TempDir Path tmp) throws IOException {
        Path target = tmp.resolve("output");
        Files.createDirectories(target);

        byte[] firstContent = "first".getBytes();
        byte[] secondContent = "second-overwritten".getBytes();

        Path zip1 = tmp.resolve("first.zip");
        try (ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(zip1))) {
            zout.putNextEntry(new ZipEntry("file.txt"));
            zout.write(firstContent);
            zout.closeEntry();
        }
        ArchiveUtils.unzip(zip1, target);
        assertEquals("first", Files.readString(target.resolve("file.txt")));

        Path zip2 = tmp.resolve("second.zip");
        try (ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(zip2))) {
            zout.putNextEntry(new ZipEntry("file.txt"));
            zout.write(secondContent);
            zout.closeEntry();
        }
        ArchiveUtils.unzip(zip2, target);
        assertEquals("second-overwritten", Files.readString(target.resolve("file.txt")));
    }
}
