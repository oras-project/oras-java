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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import land.oras.exception.OrasException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class SupportedAlgorithmTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldThrowIfInvalidDigest() {
        assertThrows(OrasException.class, () -> SupportedAlgorithm.fromDigest("invalid"));
    }

    @Test
    void shouldDigest() throws IOException {
        Path file = tempDir.resolve("shouldDigest.txt");
        Files.createFile(file);
        Files.writeString(file, "hello");
        assertEquals(
                "sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                SupportedAlgorithm.SHA256.digest("hello".getBytes()));
        assertEquals(
                "sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                SupportedAlgorithm.SHA256.digest(file));
        assertEquals(
                "sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                SupportedAlgorithm.SHA256.digest(Files.newInputStream(file)));
    }

    @Test
    void shouldPreventDuplicatePrefix() {
        assertThrows(
                OrasException.class,
                () -> SupportedAlgorithm.fromDigest(
                        "sha256:sha256:245d81d351d8d3b00ae1880ac480c593abd357d5bae561052ae23cbffbecbfe8"));
    }

    @Test
    void shouldWorkWithRegisteredDigest() {
        assertEquals(
                SupportedAlgorithm.SHA256,
                SupportedAlgorithm.fromDigest(
                        "sha256:245d81d351d8d3b00ae1880ac480c593abd357d5bae561052ae23cbffbecbfe8"));
        assertEquals(
                SupportedAlgorithm.BLAKE3,
                SupportedAlgorithm.fromDigest(
                        "blake3:0b8b60248fad7ac6dfac221b7e01a8b91c772421a15b387dd1fb2d6a94aee438"));
        assertEquals(
                SupportedAlgorithm.SHA512,
                SupportedAlgorithm.fromDigest(
                        "sha512:cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e"));
    }

    @Test
    void shouldCheckSupport() {

        // Supported
        assertTrue(SupportedAlgorithm.isSupported(
                "sha256:245d81d351d8d3b00ae1880ac480c593abd357d5bae561052ae23cbffbecbfe8"));
        assertTrue(
                SupportedAlgorithm.isSupported(
                        "sha512:cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e"));
        assertTrue(SupportedAlgorithm.isSupported(
                "blake3:0b8b60248fad7ac6dfac221b7e01a8b91c772421a15b387dd1fb2d6a94aee438"));

        // Not supported
        assertFalse(SupportedAlgorithm.isSupported("sha1:c22b5f9178342609428d6f51b2c5af4c0bde6a42"));
        assertFalse(SupportedAlgorithm.isSupported("latest"));
    }

    @Test
    void shouldCheckPattern() {

        // Matches
        assertTrue(SupportedAlgorithm.matchPattern(
                "sha256:245d81d351d8d3b00ae1880ac480c593abd357d5bae561052ae23cbffbecbfe8"));
        assertTrue(
                SupportedAlgorithm.matchPattern(
                        "sha512:cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e"));
        assertTrue(SupportedAlgorithm.matchPattern(
                "blake3:0b8b60248fad7ac6dfac221b7e01a8b91c772421a15b387dd1fb2d6a94aee438"));

        assertTrue(SupportedAlgorithm.matchPattern("sha1:c22b5f9178342609428d6f51b2c5af4c0bde6a42"));
        assertTrue(SupportedAlgorithm.matchPattern("multihash+base58:QmRZxt2b1FVZPNqd8hsiykDL3TdBDeTSPX9Kv46HmX4Gx8"));

        // Not match
        assertFalse(SupportedAlgorithm.matchPattern("latest"));
    }
}
