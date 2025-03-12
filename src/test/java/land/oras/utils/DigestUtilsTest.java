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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import land.oras.exception.OrasException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class DigestUtilsTest {

    /**
     * Blob temporary dir
     */
    @TempDir
    private Path blobDir;

    @Test
    void shouldFailWithUnknownAlgorithm() throws IOException {
        Path file = blobDir.resolve("shouldFailWithUnknownAlgorithm.txt");
        Files.createFile(file);
        Files.writeString(blobDir.resolve("shouldFailWithUnknownAlgorithm.txt"), "hello");
        assertThrows(OrasException.class, () -> DigestUtils.digest("unknown", "test", file));
        assertThrows(OrasException.class, () -> DigestUtils.digest("unknown", "test", Files.newInputStream(file)));
    }

    @Test
    void testSha256ByteArray() {
        assertEquals(
                "sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                DigestUtils.digest("SHA-256", "sha256", "hello".getBytes()));
    }

    @Test
    void testSha256File() throws IOException {
        Files.createFile(blobDir.resolve("hello.txt"));
        Files.writeString(blobDir.resolve("hello.txt"), "hello");
        assertEquals(
                "sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                DigestUtils.digest("SHA-256", "sha256", blobDir.resolve("hello.txt")));
    }

    @Test
    void testSha256Stream() throws IOException {
        Files.createFile(blobDir.resolve("hello.txt"));
        Files.writeString(blobDir.resolve("hello.txt"), "hello");
        try (var stream = Files.newInputStream(blobDir.resolve("hello.txt"))) {
            assertEquals(
                    "sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                    DigestUtils.digest("SHA-256", "sha256", stream));
        }
    }

    @Test
    void testSha256LargeFile() throws IOException {
        Files.createFile(blobDir.resolve("large.txt"));
        Files.writeString(blobDir.resolve("large.txt"), "a".repeat(1000000));
        assertEquals(
                "sha256:cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
                DigestUtils.digest("SHA-256", "sha256", blobDir.resolve("large.txt")));
    }

    @Test
    void testSha512ByteArray() {
        assertEquals(
                "sha512:9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca72323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043",
                DigestUtils.digest("SHA-512", "sha512", "hello".getBytes()));
    }

    @Test
    void testSha512File() throws IOException {
        Files.createFile(blobDir.resolve("hello.txt"));
        Files.writeString(blobDir.resolve("hello.txt"), "hello");
        assertEquals(
                "sha512:9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca72323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043",
                DigestUtils.digest("SHA-512", "sha512", blobDir.resolve("hello.txt")));
    }

    @Test
    void testSha512Stream() throws IOException {
        Files.createFile(blobDir.resolve("hello.txt"));
        Files.writeString(blobDir.resolve("hello.txt"), "hello");
        try (var stream = Files.newInputStream(blobDir.resolve("hello.txt"))) {
            assertEquals(
                    "sha512:9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca72323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043",
                    DigestUtils.digest("SHA-512", "sha512", stream));
        }
    }

    @Test
    void testSha512LargeFile() throws IOException {
        Files.createFile(blobDir.resolve("large.txt"));
        Files.writeString(blobDir.resolve("large.txt"), "a".repeat(1000000));
        assertEquals(
                "sha512:e718483d0ce769644e2e42c7bc15b4638e1f98b13b2044285632a803afa973ebde0ff244877ea60a4cb0432ce577c31beb009c5c2c49aa2e4eadb217ad8cc09b",
                DigestUtils.digest("SHA-512", "sha512", blobDir.resolve("large.txt")));
    }

    @Test
    void testBlake3ByteArray() {
        Security.addProvider(new BouncyCastleProvider());
        assertEquals(
                "blake3:ea8f163db38682925e4491c5e58d4bb3506ef8c14eb78a86e908c5624a67200f",
                DigestUtils.digest("BLAKE3-256", "blake3", "hello".getBytes()));
    }

    @Test
    void testBlake3File() throws IOException {
        Files.createFile(blobDir.resolve("hello.txt"));
        Files.writeString(blobDir.resolve("hello.txt"), "hello");
        assertEquals(
                "blake3:ea8f163db38682925e4491c5e58d4bb3506ef8c14eb78a86e908c5624a67200f",
                DigestUtils.digest("BLAKE3-256", "blake3", blobDir.resolve("hello.txt")));
    }

    @Test
    void testBlake3Stream() throws IOException {
        Files.createFile(blobDir.resolve("hello.txt"));
        Files.writeString(blobDir.resolve("hello.txt"), "hello");
        try (var stream = Files.newInputStream(blobDir.resolve("hello.txt"))) {
            assertEquals(
                    "blake3:ea8f163db38682925e4491c5e58d4bb3506ef8c14eb78a86e908c5624a67200f",
                    DigestUtils.digest("BLAKE3-256", "blake3", stream));
        }
    }

    @Test
    void testBlakeLargeFile() throws IOException {
        Files.createFile(blobDir.resolve("large.txt"));
        Files.writeString(blobDir.resolve("large.txt"), "a".repeat(1000000));
        assertEquals(
                "blake3:616f575a1b58d4c9797d4217b9730ae5e6eb319d76edef6549b46f4efe31ff8b",
                DigestUtils.digest("BLAKE3-256", "blake3", blobDir.resolve("large.txt")));
    }
}
