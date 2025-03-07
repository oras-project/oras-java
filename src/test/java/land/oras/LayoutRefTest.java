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

package land.oras;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import land.oras.exception.OrasException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class LayoutRefTest {

    @TempDir
    public static Path tempDir;

    @Test
    void shouldParseLayoutWithAllParts() {
        String ociLayout = tempDir.resolve("foo").toString();
        LayoutRef layoutRef = LayoutRef.parse("%s:v1".formatted(ociLayout));
        assertEquals("v1", layoutRef.getTag());
        assertEquals(ociLayout, layoutRef.getFolder().toString());
    }

    @Test
    void shouldParseLayoutWithDigest() {
        String ociLayout = tempDir.resolve("foo").toString();
        LayoutRef layoutRef = LayoutRef.parse("%s@sha256:12345".formatted(ociLayout));
        assertEquals("sha256:12345", layoutRef.getTag());
        assertEquals(ociLayout, layoutRef.getFolder().toString());
    }

    @Test
    void shouldParseFolderNameOnly() {
        LayoutRef layoutRef = LayoutRef.parse("foo");
        assertNull(layoutRef.getTag());
        assertEquals("foo", layoutRef.getFolder().toString());
    }

    @Test
    void shouldFailWithInvalidRef() {
        assertThrows(OrasException.class, () -> LayoutRef.parse(""));
    }
}
