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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.Security;
import java.util.HexFormat;
import land.oras.exception.OrasException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jspecify.annotations.NullMarked;

/**
 * Digest utilities
 */
@NullMarked
final class DigestUtils {

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Utils class
     */
    private DigestUtils() {}

    /**
     * Calculate the digest of a file
     * @param algorithm The algorithm
     * @param prefix The prefix
     * @param path The path
     * @return The digest
     */
    static String digest(String algorithm, String prefix, Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            try (var channel = FileChannel.open(path, StandardOpenOption.READ)) {
                long fileSize = channel.size();
                long position = 0;
                while (position < fileSize) {
                    long remaining = fileSize - position;
                    int chunkSize = (int) Math.min(Integer.MAX_VALUE, remaining);
                    MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, position, chunkSize);
                    digest.update(buffer);
                    position += chunkSize;
                }
            }
            byte[] hashBytes = digest.digest();
            return formatHex(prefix, hashBytes);
        } catch (Exception e) {
            throw new OrasException("Failed to calculate digest", e);
        }
    }

    /**
     * Calculate the digest of a byte array
     * @param algorithm The algorithm
     * @param prefix The prefix
     * @param bytes bytes
     * @return The digest
     */
    static String digest(String algorithm, String prefix, byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(bytes);

            // Convert the byte array to hex
            return formatHex(prefix, hashBytes);
        } catch (Exception e) {
            throw new OrasException("Failed to calculate digest", e);
        }
    }

    /**
     * Calculate the sha256 digest of a InputStream
     * @param algorithm The algorithm
     * @param prefix The prefix
     * @param input The input
     * @return The digest
     */
    static String digest(String algorithm, String prefix, InputStream input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hashBytes = digest.digest();
            return formatHex(prefix, hashBytes);
        } catch (Exception e) {
            throw new OrasException("Failed to calculate digest", e);
        }
    }

    private static String formatHex(String prefix, final byte[] hashBytes) {
        String formatHex = HEX_FORMAT.formatHex(hashBytes);
        return prefix + ":" + formatHex;
    }
}
