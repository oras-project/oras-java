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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import land.oras.exception.OrasException;
import org.jspecify.annotations.NullMarked;

/**
 * Digest utilities
 */
@NullMarked
public final class DigestUtils {

    /**
     * Utils class
     */
    private DigestUtils() {}

    /**
     * Calculate the sha256 digest of a byte array
     * @param bytes The bytes
     * @return The digest
     */
    public static String sha256(byte[] bytes) {
        return digest("SHA-256", bytes);
    }

    /**
     * Calculate the sha256 digest of a file
     * @param file The file
     * @return The digest
     */
    public static String sha256(Path file) {
        return digest("SHA-256", file);
    }

    /**
     * Calculate the digest of a file
     * @param algorithm The algorithm
     * @param path The path
     * @return The digest
     */
    public static String digest(String algorithm, Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return "%s:%s".formatted(algorithm.toLowerCase().replaceAll("-", ""), sb.toString());
        } catch (Exception e) {
            throw new OrasException("Failed to calculate digest", e);
        }
    }

    /**
     * Calculate the digest of a byte array
     * @param algorithm The algorithm
     * @param bytes bytes
     * @return The digest
     */
    public static String digest(String algorithm, byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(bytes);

            // Convert the byte array to hex
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            return "%s:%s".formatted(algorithm.toLowerCase().replaceAll("-", ""), sb.toString());
        } catch (Exception e) {
            throw new OrasException("Failed to calculate digest", e);
        }
    }

    /**
     * Calculate the sha256 digest of a InputStream
     * @param input The input
     * @return The digest
     */
    public static String sha256(InputStream input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return "sha256:%s".formatted(sb.toString());
        } catch (Exception e) {
            throw new OrasException("Failed to calculate digest", e);
        }
    }
}
