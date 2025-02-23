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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    void testSha256ByteArray() {
        assertEquals(
                "sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                DigestUtils.digest("SHA-256", "hello".getBytes()));
    }

    @Test
    void testSha256File() throws IOException {
        Files.createFile(blobDir.resolve("hello.txt"));
        Files.writeString(blobDir.resolve("hello.txt"), "hello");
        assertEquals(
                "sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                DigestUtils.digest("SHA-256", blobDir.resolve("hello.txt")));
    }

    @Test
    void testSha256Stream() throws IOException {
        Files.createFile(blobDir.resolve("hello.txt"));
        Files.writeString(blobDir.resolve("hello.txt"), "hello");
        try (var stream = Files.newInputStream(blobDir.resolve("hello.txt"))) {
            assertEquals(
                    "sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                    DigestUtils.digest("SHA-256", stream));
        }
    }

    @Test
    void testSha256LargeFile() throws IOException {
        Files.createFile(blobDir.resolve("large.txt"));
        Files.writeString(blobDir.resolve("large.txt"), "a".repeat(1000000));
        assertEquals(
                "sha256:cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
                DigestUtils.digest("SHA-256", blobDir.resolve("large.txt")));
    }

    @Test
    void testSha512ByteArray() {
        assertEquals(
                "sha512:9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca72323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043",
                DigestUtils.digest("SHA-512", "hello".getBytes()));
    }

    @Test
    void testSha512File() throws IOException {
        Files.createFile(blobDir.resolve("hello.txt"));
        Files.writeString(blobDir.resolve("hello.txt"), "hello");
        assertEquals(
                "sha512:9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca72323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043",
                DigestUtils.digest("SHA-512", blobDir.resolve("hello.txt")));
    }

    @Test
    void testSha512Stream() throws IOException {
        Files.createFile(blobDir.resolve("hello.txt"));
        Files.writeString(blobDir.resolve("hello.txt"), "hello");
        try (var stream = Files.newInputStream(blobDir.resolve("hello.txt"))) {
            assertEquals(
                    "sha512:9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca72323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043",
                    DigestUtils.digest("SHA-512", stream));
        }
    }

    @Test
    void testSha512LargeFile() throws IOException {
        Files.createFile(blobDir.resolve("large.txt"));
        Files.writeString(blobDir.resolve("large.txt"), "a".repeat(1000000));
        assertEquals(
                "sha512:e718483d0ce769644e2e42c7bc15b4638e1f98b13b2044285632a803afa973ebde0ff244877ea60a4cb0432ce577c31beb009c5c2c49aa2e4eadb217ad8cc09b",
                DigestUtils.digest("SHA-512", blobDir.resolve("large.txt")));
    }
}
