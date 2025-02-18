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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;
import land.oras.exception.OrasException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Archive utilities
 */
@NullMarked
public final class ArchiveUtils {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ArchiveUtils.class);

    /**
     * Hidden constructor
     */
    private ArchiveUtils() {}

    /**
     * Create a temporary archive when uploading directory layers
     * @return The path to the archive
     */
    public static Path createTempArchive() {
        try {
            return Files.createTempFile("oras", ".tar.gz");
        } catch (IOException e) {
            throw new OrasException("Failed to create temporary archive", e);
        }
    }

    /**
     * Extract a tar.gz file to a target directory
     * @param archive The archive
     * @param target The target directory
     */
    public static void extractTarGz(Path archive, Path target) {

        // Open the tar.gz file for reading
        try {
            try (InputStream fis = Files.newInputStream(archive);
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis);
                    TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {

                TarArchiveEntry entry;
                // Iterate through tar entries
                while ((entry = tais.getNextEntry()) != null) {

                    // Prevent path traversal attacks
                    Path outputPath = target.resolve(entry.getName()).normalize();

                    if (entry.isDirectory()) {
                        Files.createDirectories(outputPath);
                    } else {
                        Files.createDirectories(outputPath.getParent());

                        // Write the file data
                        try (OutputStream out = Files.newOutputStream(outputPath)) {
                            tais.transferTo(out);
                        }

                        // Restore file permissions (optional, based on your need)
                        if (entry.isSymbolicLink()) {
                            Files.createSymbolicLink(outputPath, Paths.get(entry.getLinkName()));
                        } else {
                            // Optionally set file permissions here
                            Files.setPosixFilePermissions(outputPath, convertToPosixPermissions(entry.getMode()));
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new OrasException("Failed to extract tar.gz file", e);
        }
    }

    /**
     * Convert the tar entry mode to PosixFilePermissions
     * @param mode The mode
     * @return The permissions
     */
    private static Set<PosixFilePermission> convertToPosixPermissions(int mode) {
        Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);

        if ((mode & 0400) != 0) permissions.add(PosixFilePermission.OWNER_READ);
        if ((mode & 0200) != 0) permissions.add(PosixFilePermission.OWNER_WRITE);
        if ((mode & 0100) != 0) permissions.add(PosixFilePermission.OWNER_EXECUTE);

        if ((mode & 0040) != 0) permissions.add(PosixFilePermission.GROUP_READ);
        if ((mode & 0020) != 0) permissions.add(PosixFilePermission.GROUP_WRITE);
        if ((mode & 0010) != 0) permissions.add(PosixFilePermission.GROUP_EXECUTE);

        if ((mode & 0004) != 0) permissions.add(PosixFilePermission.OTHERS_READ);
        if ((mode & 0002) != 0) permissions.add(PosixFilePermission.OTHERS_WRITE);
        if ((mode & 0001) != 0) permissions.add(PosixFilePermission.OTHERS_EXECUTE);

        return permissions;
    }

    /**
     * Create a tar.gz file from a directory
     * @param sourceDir The source directory
     * @return The path to the tar.gz file
     */
    public static Path createTarGz(Path sourceDir) {
        Path tarGzFile = createTempArchive();
        try (OutputStream fos = Files.newOutputStream(tarGzFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(bos);
                TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)) {

            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            try (Stream<Path> paths = Files.walk(sourceDir)) {
                paths.filter(path -> !Files.isDirectory(path)).forEach(path -> {
                    try {
                        String entryName = sourceDir.relativize(path).toString();
                        LOG.info("Adding: {}", entryName);
                        TarArchiveEntry entry = null;
                        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                        if (Files.isSymbolicLink(path)) {
                            Path linkTarget = Files.readSymbolicLink(path);
                            entry = new TarArchiveEntry(path.toFile(), entryName);
                            entry.setLinkName(linkTarget.toString());
                        } else {
                            entry = new TarArchiveEntry(path.toFile(), entryName);
                            entry.setSize(attrs.isRegularFile() ? Files.size(path) : 0);
                        }

                        // Set UID, GID, Uname, Gname to zero or empty
                        entry.setUserId(0);
                        entry.setGroupId(0);
                        entry.setUserName("");
                        entry.setGroupName("");

                        taos.putArchiveEntry(entry);
                        // If it's a regular file, write the file data
                        if (attrs.isRegularFile()) {
                            try (InputStream fis = Files.newInputStream(path)) {
                                fis.transferTo(taos); // Write file contents to tar
                            }
                        }
                        taos.closeArchiveEntry();
                    } catch (IOException e) {
                        throw new OrasException("Failed to create tar.gz file", e);
                    }
                });
            } catch (IOException e) {
                throw new OrasException("Failed to create tar.gz file", e);
            }
        } catch (IOException e) {
            throw new OrasException("Failed to create tar.gz file", e);
        }
        return tarGzFile;
    }
}
