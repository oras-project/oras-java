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
import java.nio.file.Path;
import java.util.regex.Pattern;
import land.oras.exception.OrasException;
import org.jspecify.annotations.NullMarked;

/**
 * Supported algorithms for digest.
 * See @link <a href="https://github.com/opencontainers/image-spec/blob/main/descriptor.md#digests">https://github.com/opencontainers/image-spec/blob/main/descriptor.md#digests</a>
 * See @link <a href="https://github.com/opencontainers/image-spec/blob/main/descriptor.md#registered-algorithms">https://github.com/opencontainers/image-spec/blob/main/descriptor.md#registered-algorithms</a>
 */
@NullMarked
public enum SupportedAlgorithm {

    /**
     * SHA-256
     */
    SHA256("SHA-256", "sha256"),

    /**
     * SHA-384
     */
    SHA384("SHA-384", "sha384"),

    /**
     * SHA-512
     */
    SHA512("SHA-512", "sha512"),

    /**
     * BLAKE3
     */
    BLAKE3("BLAKE3-256", "blake3");

    /**
     * The algorithm
     */
    private final String algorithm;

    /**
     * The prefix
     */
    private final String prefix;

    /**
     * Regex for a digest
     * <a href="https://github.com/opencontainers/image-spec/blob/main/descriptor.md#digests">Digests</a>
     */
    private static final Pattern DIGEST_REGEX = Pattern.compile("^[a-z0-9]+(?:[+._-][a-z0-9]+)*:[a-zA-Z0-9=_-]+$");

    /**
     * Get the algorithm
     * @param algorithm The algorithm
     * @param prefix The prefix
     */
    SupportedAlgorithm(String algorithm, String prefix) {
        this.algorithm = algorithm;
        this.prefix = prefix;
    }

    /**
     * Get the prefix
     * @return The prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Get the algorithm
     * @return The algorithm
     */
    public String getAlgorithmName() {
        return algorithm;
    }

    /**
     * Digest a byte array
     * @param bytes The bytes
     * @return The digest
     */
    public String digest(byte[] bytes) {
        return DigestUtils.digest(algorithm, prefix, bytes);
    }

    /**
     * Digest a file
     * @param file The file
     * @return The digest
     */
    public String digest(Path file) {
        return DigestUtils.digest(algorithm, prefix, file);
    }

    /**
     * Digest an input stream
     * @param inputStream The input stream
     * @return The digest
     */
    public String digest(InputStream inputStream) {
        return DigestUtils.digest(algorithm, prefix, inputStream);
    }

    /**
     * Check if the algorithm match pattern
     * @param digest The digest
     * @return True if supported
     */
    public static boolean matchPattern(String digest) {
        return DIGEST_REGEX.matcher(digest).matches();
    }

    /**
     * Check if the digest is supported
     * @param digest The digest
     * @return True if supported
     */
    public static boolean isSupported(String digest) {
        if (!matchPattern(digest)) {
            return false;
        }
        for (SupportedAlgorithm algorithm : SupportedAlgorithm.values()) {
            if (digest.startsWith(algorithm.getPrefix())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the algorithm from a digest
     * @param digest The digest
     * @return The algorithm
     */
    public static SupportedAlgorithm fromDigest(String digest) {
        if (!DIGEST_REGEX.matcher(digest).matches()) {
            throw new OrasException("Invalid digest: " + digest);
        }
        for (SupportedAlgorithm algorithm : SupportedAlgorithm.values()) {
            if (digest.startsWith(algorithm.getPrefix())) {
                return algorithm;
            }
        }
        throw new OrasException("Unsupported digest: " + digest);
    }

    /**
     * Get the default algorithm
     * @return The default algorithm
     */
    public static SupportedAlgorithm getDefault() {
        return SupportedAlgorithm.SHA256;
    }

    /**
     * Return  the digest without the prefix
     * @param digest The digest
     * @return The digest without the prefix
     */
    public static String getDigest(String digest) {
        SupportedAlgorithm algorithm = fromDigest(digest);
        return digest.substring(algorithm.getPrefix().length() + 1);
    }
}
