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
class LayoutRefTest {

    @TempDir
    public static Path tempDir;

    @TempDir
    public static Path ociLayoutTempDir;

    @TempDir
    public static Path otherOciLayout;

    @Test
    void shouldParseLayoutWithAllParts() {
        String ociLayout = tempDir.resolve("foo").toString();
        LayoutRef layoutRef = LayoutRef.parse("%s:v1".formatted(ociLayout));
        assertEquals("v1", layoutRef.getTag());
        assertEquals(ociLayout, layoutRef.getFolder().toString());
        assertEquals(ociLayout, layoutRef.getRepository());
        assertFalse(layoutRef.isValidDigest(), "v1 is not a valid digest");
    }

    @Test
    void shouldParseLayoutWithDigest() {
        String ociLayout = tempDir.resolve("foo").toString();
        LayoutRef layoutRef = LayoutRef.parse(
                "%s@sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824".formatted(ociLayout));
        assertEquals("sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", layoutRef.getTag());
        assertEquals(ociLayout, layoutRef.getFolder().toString());
        assertEquals(ociLayout, layoutRef.getRepository());
        assertTrue(
                layoutRef.isValidDigest(),
                "sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824 should be a valid digest pattern");
    }

    @Test
    void shouldParseFolderNameOnly() {
        LayoutRef layoutRef = LayoutRef.parse("foo");
        assertNull(layoutRef.getTag());
        assertEquals("foo", layoutRef.getFolder().toString());
    }

    @Test
    void shouldReturnForOciLayout() {
        OCILayout ociLayout = OCILayout.builder().defaults(ociLayoutTempDir).build();
        LayoutRef layoutRef =
                LayoutRef.parse(otherOciLayout.getFileName().toString()).forLayout(ociLayout);
        assertEquals(layoutRef.getFolder(), ociLayoutTempDir, "Path should be the same");
    }

    @Test
    void shouldFailWithInvalidRef() {
        assertThrows(OrasException.class, () -> LayoutRef.parse(""));
    }

    @Test
    void shouldGetAlgorithm() {
        LayoutRef layoutRef = LayoutRef.parse("foo");
        assertEquals("sha256", layoutRef.getAlgorithm().getPrefix());
        layoutRef = LayoutRef.parse("foo@sha256:");
        assertEquals("sha256", layoutRef.getAlgorithm().getPrefix());
        layoutRef = LayoutRef.parse(
                "foo@sha512:cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e");
        assertEquals("sha512", layoutRef.getAlgorithm().getPrefix());
    }

    @Test
    void testEqualsAndHashCode() {
        LayoutRef layoutRef1 = LayoutRef.parse("foo:v1");
        LayoutRef layoutRef2 = LayoutRef.parse("foo:v1");
        LayoutRef layoutRef3 = LayoutRef.parse("bar:v1");

        assertEquals(layoutRef1, layoutRef2);
        assertNotEquals(layoutRef1, layoutRef3);
        assertEquals(layoutRef1.hashCode(), layoutRef2.hashCode());

        // Not equals
        assertNotEquals(layoutRef1.withTag("newtag"), layoutRef1);
        assertNotEquals("foo", layoutRef1);
        assertNotEquals(null, layoutRef1);
    }

    @Test
    void testToString() {
        LayoutRef layoutRef = LayoutRef.parse(
                "foo@sha512:cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e");
        assertEquals(
                "foo@sha512:cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
                layoutRef.toString());
        layoutRef = LayoutRef.parse("foo:latest");
        assertEquals("foo:latest", layoutRef.toString());
        layoutRef = LayoutRef.parse("foo");
        assertEquals("foo", layoutRef.toString());
    }
}
