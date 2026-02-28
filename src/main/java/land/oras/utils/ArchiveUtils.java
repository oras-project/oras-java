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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;
import land.oras.LocalPath;
import land.oras.exception.OrasException;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.AsiExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
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
    public static Path createTempTar() {
        try {
            return Files.createTempFile("oras", ".tar");
        } catch (IOException e) {
            throw new OrasException("Failed to create temporary archive", e);
        }
    }

    /**
     * Create a temporary zip file when uploading directory layers with zip media type
     * @return The path to the zip file
     */
    public static Path createTempZip() {
        try {
            return Files.createTempFile("oras", ".zip");
        } catch (IOException e) {
            throw new OrasException("Failed to create temporary zip file", e);
        }
    }

    /**
     * Create a temporary directory
     * @return The path to the temporary directory
     */
    public static Path createTempDir() {
        try {
            return Files.createTempDirectory("oras");
        } catch (IOException e) {
            throw new OrasException("Failed to create temporary directory", e);
        }
    }

    /**
     * Zip a local source dire and return a temporary zip file as a local path
     * @param sourceDir The source directory
     * @return The local path to the zip file
     */
    public static LocalPath zip(LocalPath sourceDir) {
        Path zipFile = createTempZip();
        boolean isAbsolute = sourceDir.getPath().isAbsolute();
        try (OutputStream fos = Files.newOutputStream(zipFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                ZipArchiveOutputStream zaos = new ZipArchiveOutputStream(bos)) {
            try (Stream<Path> paths = Files.walk(sourceDir.getPath())) {
                paths.forEach(path -> {
                    LOG.trace("Visiting path: {}", path);
                    try {
                        Path baseName = isAbsolute ? sourceDir.getPath().getFileName() : sourceDir.getPath();
                        Path relativePath = baseName.resolve(sourceDir.getPath().relativize(path));
                        if (relativePath.toString().isEmpty()) {
                            LOG.trace("Skipping root directory: {}", path);
                            return;
                        }
                        String entryName = relativePath.toString();
                        if (Files.isSymbolicLink(path)) {
                            LOG.trace("Adding symlink entry to zip: {}", entryName);
                            Path linkTarget = Files.readSymbolicLink(path);
                            ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
                            AsiExtraField asiField = new AsiExtraField();
                            asiField.setLinkedFile(linkTarget.toString());
                            // 0120000 = S_IFLNK (symlink file type), 0755 = permissions
                            asiField.setMode(0120755);
                            entry.addExtraField(asiField);
                            entry.setSize(0);
                            zaos.putArchiveEntry(entry);
                        } else if (Files.isDirectory(path)) {
                            LOG.trace("Adding directory entry to zip: {}", entryName + "/");
                            ZipArchiveEntry entry = new ZipArchiveEntry(entryName + "/");
                            zaos.putArchiveEntry(entry);
                        } else {
                            LOG.trace("Adding file entry to zip: {}", entryName);
                            ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
                            entry.setSize(Files.size(path));
                            zaos.putArchiveEntry(entry);
                            try (InputStream fis = Files.newInputStream(path)) {
                                fis.transferTo(zaos);
                            }
                        }
                        zaos.closeArchiveEntry();
                    } catch (IOException e) {
                        throw new OrasException("Failed to create zip file", e);
                    }
                });
            }
        } catch (IOException e) {
            throw new OrasException("Failed to create zip file", e);
        }
        return LocalPath.of(zipFile, Const.ZIP_MEDIA_TYPE);
    }

    /**
     * Create a tar.gz file from a directory
     * @param sourceDir The source directory
     * @return The path to the tar.gz file
     */
    public static LocalPath tar(LocalPath sourceDir) {
        Path tarFile = createTempTar();
        boolean isAbsolute = sourceDir.getPath().isAbsolute();
        try (OutputStream fos = Files.newOutputStream(tarFile);

                // Output stream chain
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                TarArchiveOutputStream taos = new TarArchiveOutputStream(bos)) {

            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            try (Stream<Path> paths = Files.walk(sourceDir.getPath())) {
                paths.forEach(path -> {
                    LOG.trace("Visiting path: {}", path);
                    try {
                        Path baseName = isAbsolute ? sourceDir.getPath().getFileName() : sourceDir.getPath();
                        Path relativePath = baseName.resolve(sourceDir.getPath().relativize(path));
                        if (relativePath.toString().isEmpty()) {
                            LOG.trace("Skipping root directory: {}", path);
                            return;
                        }
                        String entryName = relativePath.toString();

                        TarArchiveEntry entry = null;
                        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

                        if (Files.isSymbolicLink(path)) {
                            LOG.trace("Adding symlink entry: {}", entryName);
                            Path linkTarget = Files.readSymbolicLink(path);
                            entry = new TarArchiveEntry(entryName, TarArchiveEntry.LF_SYMLINK);
                            entry.setLinkName(linkTarget.toString());
                            entry.setSize(0);
                        } else {
                            LOG.trace("Adding entry: {}", entryName);
                            entry = new TarArchiveEntry(path.toFile(), entryName);
                            entry.setSize(attrs.isRegularFile() ? Files.size(path) : 0);
                        }

                        // Get posix permissions
                        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);
                        int mode = permissionsToMode(permissions);
                        LOG.trace("Permissions: {}", permissions);
                        LOG.trace("Mode: {}", mode);

                        // Set UID, GID, Uname, Gname to zero or empty
                        entry.setUserId(0);
                        entry.setGroupId(0);
                        entry.setUserName("");
                        entry.setGroupName("");
                        entry.setMode(mode);
                        taos.putArchiveEntry(entry);
                        // If it's a regular file, write the file data
                        if (attrs.isRegularFile() && !entry.isSymbolicLink()) {
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
        return LocalPath.of(tarFile, Const.DEFAULT_BLOB_MEDIA_TYPE);
    }

    /**
     * Create a tar compressed file from a directory
     * @param sourceDir The source directory
     * @param mediaType The media type
     * @return The path to the tar compressed file
     */
    public static LocalPath tarcompress(LocalPath sourceDir, String mediaType) {
        return compress(tar(sourceDir), mediaType);
    }

    /**
     * Ensure that the entry is safe to extract
     * @param entry The tar entry
     * @param target The target directory
     * @throws IOException
     */
    static void ensureSafeEntry(ArchiveEntry entry, Path target) throws IOException {
        // Prevent path traversal attacks
        Path outputPath = target.resolve(entry.getName()).normalize();
        Path normalizedTarget = target.toAbsolutePath().normalize();
        if (!outputPath.startsWith(normalizedTarget)) {
            throw new IOException("Entry is outside of the target dir: " + target);
        }
    }

    /**
     * Extract a tar file to a target directory
     * @param path The tar file
     * @param target The target directory
     */
    public static void untar(Path path, Path target) {
        try {
            untar(Files.newInputStream(path), target);
        } catch (IOException e) {
            throw new OrasException("Failed to extract tar.gz file", e);
        }
    }

    /**
     * Uncompress a compressed file and untar it to the target directory
     * @param path The compressed file
     * @param target The target directory
     * @param mediaType The media type of the compressed file
     */
    public static void uncompressuntar(Path path, Path target, String mediaType) {
        try {
            LocalPath tar = uncompress(Files.newInputStream(path), mediaType);
            untar(tar.getPath(), target);
        } catch (IOException e) {
            throw new OrasException("Failed to extract tar.gz file", e);
        }
    }

    /**
     * Uncompress a compressed file and untar to a temporary directory
     * @param path The compressed file
     * @param mediaType The media type of the compressed file
     * @return The path to the temporary directory
     */
    public static Path uncompressuntar(Path path, String mediaType) {
        Path tempDir = createTempDir();
        uncompressuntar(path, tempDir, mediaType);
        return tempDir;
    }

    /**
     * Convienience method to extract a tar file to a temporary directory
     * @param path The tar file
     * @return The path to the temporary directory
     */
    public static Path untar(Path path) {
        Path tempDir = createTempDir();
        untar(path, tempDir);
        return tempDir;
    }

    /**
     * Extract a zip file to a target directory
     * @param path The zip file
     * @param target The target directory
     */
    public static void unzip(Path path, Path target) {
        try {
            unzip(Files.newInputStream(path), target);
        } catch (IOException e) {
            throw new OrasException("Failed to extract zip file", e);
        }
    }

    /**
     * Unzip a file to a temporary directory and return the local path to the temporary directory
     * @param fis The zip file input stream
     * @return The local path to the temporary directory
     */
    static LocalPath unzip(InputStream fis) {
        Path tempDir = createTempDir();
        unzip(fis, tempDir);
        return LocalPath.of(tempDir);
    }

    /**
     * Extract a zip file to a target directory
     * @param fis The zip file input stream
     * @param target The target directory
     */
    static void unzip(InputStream fis, Path target) {
        // Open the zip file for reading
        try {
            try (BufferedInputStream bis = new BufferedInputStream(fis);
                    ZipArchiveInputStream zais = new ZipArchiveInputStream(bis)) {
                ZipArchiveEntry entry;

                // Iterate through zip entries
                while ((entry = zais.getNextEntry()) != null) {

                    // Prevent path traversal attacks
                    Path outputPath = target.resolve(entry.getName()).normalize();

                    // Check if the entry is outside the target directory
                    ensureSafeEntry(entry, target);

                    if (entry.isDirectory()) {
                        LOG.debug("Extracting directory: {}", entry.getName());
                        Files.createDirectories(outputPath);
                    }
                    // Check symlink from AsiExtraField
                    else {
                        AsiExtraField asiField = (AsiExtraField) entry.getExtraField(new AsiExtraField().getHeaderId());
                        if (entry.isUnixSymlink() || (asiField != null && asiField.isLink())) {
                            LOG.debug("Extracting symlink: {}", entry.getName());
                            Files.createDirectories(outputPath.getParent());
                            String linkStr = asiField != null
                                    ? asiField.getLinkedFile()
                                    : new String(zais.readAllBytes(), StandardCharsets.UTF_8);
                            Files.createSymbolicLink(outputPath, Paths.get(linkStr));
                        } else {
                            LOG.debug("Extracting file: {}", entry.getName());
                            Files.createDirectories(outputPath.getParent());
                            try (OutputStream out = Files.newOutputStream(outputPath)) {
                                zais.transferTo(out);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new OrasException("Failed to extract zip file", e);
        }
    }

    /**
     * Extract a tar file to a target directory
     * @param fis The archive stream
     * @param target The target directory
     */
    public static void untar(InputStream fis, Path target) {

        // Open the tar.gz file for reading
        try {
            try (BufferedInputStream bis = new BufferedInputStream(fis);
                    TarArchiveInputStream tais = new TarArchiveInputStream(bis)) {

                TarArchiveEntry entry;
                // Iterate through tar entries
                while ((entry = tais.getNextEntry()) != null) {

                    // Prevent path traversal attacks
                    Path outputPath = target.resolve(entry.getName()).normalize();

                    // Check if the entry is outside the target directory
                    ensureSafeEntry(entry, target);

                    LOG.trace("Extracting entry: {}", entry.getName());

                    if (entry.isDirectory()) {
                        LOG.debug("Extracting directory: {}", entry.getName());
                        Files.createDirectories(outputPath);
                    } else {
                        LOG.trace("Creating directories for file: {}", outputPath.getParent());
                        Files.createDirectories(outputPath.getParent());

                        // Restore file permissions (optional, based on your need)
                        if (entry.isSymbolicLink()) {
                            LOG.trace("Extracting symlink {} to: {}", outputPath, entry.getLinkName());
                            Files.createSymbolicLink(outputPath, Paths.get(entry.getLinkName()));
                        } else {
                            try (OutputStream out = Files.newOutputStream(outputPath)) {
                                tais.transferTo(out);
                            }
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
     * Compress a tar file to a tar.gz or tar.zstd file depending on the requested media type
     * @param path The tar file
     * @param mediaType The target media type
     * @return The path to the tar.gz file or the tar.zstd file
     */
    public static LocalPath compress(LocalPath path, String mediaType) {
        return SupportedCompression.fromMediaType(mediaType).compress(path);
    }

    /**
     * Extract a compressed file from a tar.gz or tar.zstd file depending on the requested media type
     * @param is The compressed input stream
     * @param mediaType The media type of the stream to select the uncompression method
     * @return The path to the tar.gz file or the tar.zstd file
     */
    public static LocalPath uncompress(InputStream is, String mediaType) {
        return SupportedCompression.fromMediaType(mediaType).uncompress(is);
    }

    static LocalPath compressZstd(LocalPath tarFile) {
        LOG.trace("Compressing tar file to zstd archive");
        Path tarGzFile = Paths.get(tarFile + ".gz");
        try (InputStream fis = Files.newInputStream(tarFile.getPath());
                BufferedInputStream bis = new BufferedInputStream(fis);
                OutputStream fos = Files.newOutputStream(tarGzFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                ZstdCompressorOutputStream zstdos = new ZstdCompressorOutputStream(bos)) {

            bis.transferTo(zstdos);
        } catch (IOException e) {
            throw new OrasException("Failed to compress tar file to zstd archive", e);
        }
        return LocalPath.of(tarGzFile, Const.BLOB_DIR_ZSTD_MEDIA_TYPE);
    }

    static LocalPath compressGzip(LocalPath tarFile) {
        LOG.trace("Compressing tar file to gz archive");
        Path tarGzFile = Paths.get(tarFile.toString() + ".gz");
        try (InputStream fis = Files.newInputStream(tarFile.getPath());
                BufferedInputStream bis = new BufferedInputStream(fis);
                OutputStream fos = Files.newOutputStream(tarGzFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(bos)) {

            bis.transferTo(gzos);
        } catch (IOException e) {
            throw new OrasException("Failed to compress tar file to gz archive", e);
        }
        return LocalPath.of(tarGzFile, Const.DEFAULT_BLOB_DIR_MEDIA_TYPE);
    }

    static LocalPath uncompressGzip(InputStream inputStream) {
        LOG.trace("Uncompressing tar.gz file");
        Path tarFile = createTempTar();
        try (BufferedInputStream bis = new BufferedInputStream(inputStream);
                GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis);
                OutputStream fos = Files.newOutputStream(tarFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            gzis.transferTo(bos);
        } catch (IOException e) {
            throw new OrasException("Failed to uncompress tar.gz file", e);
        }
        return LocalPath.of(tarFile, Const.DEFAULT_BLOB_MEDIA_TYPE);
    }

    static LocalPath uncompressZstd(InputStream inputStream) {
        LOG.trace("Uncompressing zstd file");
        Path tarFile = createTempTar();
        try (BufferedInputStream bis = new BufferedInputStream(inputStream);
                ZstdCompressorInputStream gzis = new ZstdCompressorInputStream(bis);
                OutputStream fos = Files.newOutputStream(tarFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            gzis.transferTo(bos);
        } catch (IOException e) {
            throw new OrasException("Failed to uncompress tar.zstd file", e);
        }
        return LocalPath.of(tarFile, Const.DEFAULT_BLOB_MEDIA_TYPE);
    }

    /**
     * Opposite of convertToPosixPermissions. Convert PosixFilePermissions to mode
     * @param permissions The permissions
     * @return The mode
     */
    private static int permissionsToMode(Set<PosixFilePermission> permissions) {
        int mode = 0;
        if (permissions.contains(PosixFilePermission.OWNER_READ)) mode |= 0400;
        if (permissions.contains(PosixFilePermission.OWNER_WRITE)) mode |= 0200;
        if (permissions.contains(PosixFilePermission.OWNER_EXECUTE)) mode |= 0100;
        if (permissions.contains(PosixFilePermission.GROUP_READ)) mode |= 0040;
        if (permissions.contains(PosixFilePermission.GROUP_WRITE)) mode |= 0020;
        if (permissions.contains(PosixFilePermission.GROUP_EXECUTE)) mode |= 0010;
        if (permissions.contains(PosixFilePermission.OTHERS_READ)) mode |= 0004;
        if (permissions.contains(PosixFilePermission.OTHERS_WRITE)) mode |= 0002;
        if (permissions.contains(PosixFilePermission.OTHERS_EXECUTE)) mode |= 0001;
        return mode;
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
}
