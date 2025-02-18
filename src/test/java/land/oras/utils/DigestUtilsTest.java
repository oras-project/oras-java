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
    void testByteArray() {
        assertEquals(
                "sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                DigestUtils.sha256("hello".getBytes()));
    }

    @Test
    void testFile() throws IOException {
        Files.createFile(blobDir.resolve("hello.txt"));
        Files.writeString(blobDir.resolve("hello.txt"), "hello");
        assertEquals(
                "sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                DigestUtils.digest("SHA-256", blobDir.resolve("hello.txt")));
    }

    @Test
    void testLargeFile() throws IOException {
        Files.createFile(blobDir.resolve("large.txt"));
        Files.writeString(blobDir.resolve("large.txt"), "a".repeat(1000000));
        assertEquals(
                "sha256:cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
                DigestUtils.digest("SHA-256", blobDir.resolve("large.txt")));
    }
}
