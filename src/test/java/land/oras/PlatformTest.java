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

package land.oras;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link Platform}
 */
@Execution(ExecutionMode.CONCURRENT)
class PlatformTest {

    @ParameterizedTest
    @ValueSource(strings = {Const.VARIANT_V5, Const.VARIANT_V6, Const.VARIANT_V7, Const.VARIANT_V8})
    void testVariants(String variant) {
        Platform platform1 = Platform.of(Const.PLATFORM_LINUX, Const.PLATFORM_ARCHITECTURE_ARM64, variant);
        assertEquals("linux", platform1.os());
        assertEquals("arm64", platform1.architecture());
        assertEquals(variant, platform1.variant());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                Const.PLATFORM_UNKNOWN,
                Const.PLATFORM_ARCHITECTURE_AMD64,
                Const.PLATFORM_ARCHITECTURE_ARM,
                Const.PLATFORM_ARCHITECTURE_ARM64,
                Const.PLATFORM_ARCHITECTURE_386
            })
    void testArchitectures(String architecture) {
        Platform platform1 = Platform.of(Const.PLATFORM_LINUX, architecture);
        assertEquals("linux", platform1.os());
        assertEquals(architecture, platform1.architecture());
        assertNull(platform1.variant());
    }

    @ParameterizedTest
    @ValueSource(strings = {Const.PLATFORM_UNKNOWN, Const.PLATFORM_LINUX, Const.PLATFORM_WINDOWS})
    void testOS(String os) {
        Platform platform1 = Platform.of(os, Const.PLATFORM_ARCHITECTURE_AMD64);
        assertEquals(os, platform1.os());
        assertEquals("amd64", platform1.architecture());
        assertNull(platform1.variant());
    }

    @Test
    void shouldSerializeToJson() {
        Platform platform = Platform.linuxAmd64();
        String json = JsonUtils.toJson(platform);
        // language=json
        String expected = "{\"os\":\"linux\",\"architecture\":\"amd64\"}";
        assertEquals(expected, json);
    }

    @Test
    void shouldSerializeToJsonWithOptionalValues() {
        Platform platform =
                Platform.linuxAmd64().withFeatures(List.of("sse4", "avx2")).withOsFeatures(List.of("linux-gnu"));
        String json = JsonUtils.toJson(platform);
        // language=json
        String expected =
                "{\"os\":\"linux\",\"architecture\":\"amd64\",\"os.features\":[\"linux-gnu\"],\"features\":[\"sse4\",\"avx2\"]}";
        assertEquals(expected, json);
    }

    @Test
    void shouldTestEmptyPlatform() {
        Platform platform = Platform.empty();
        assertNotEquals(Platform.unknown(), platform);
        assertEquals(Const.PLATFORM_UNKNOWN, platform.os());
        assertEquals(Const.PLATFORM_UNKNOWN, platform.architecture());
        assertNull(platform.variant());
    }

    @Test
    void shouldTestUnknownPlatform() {
        Platform platform = Platform.unknown();
        assertNotEquals(Platform.empty(), platform);
        assertEquals(Const.PLATFORM_UNKNOWN, platform.os());
        assertEquals(Const.PLATFORM_UNKNOWN, platform.architecture());
        assertNull(platform.variant());
    }

    @Test
    void shouldReadFromJson() {
        // language=json
        String json =
                """
            {
              "architecture": "amd64",
              "os": "linux"
            }
            """;
        Platform platform = JsonUtils.fromJson(json, Platform.class);
        assertEquals("amd64", platform.architecture());
        assertEquals("linux", platform.os());
        assertNull(platform.variant());
        assertEquals(Platform.linuxAmd64(), platform);

        json =
                """
            {
              "architecture": "unknown",
              "os": "unknown"
            }
            """;
        platform = JsonUtils.fromJson(json, Platform.class);
        assertEquals(Platform.unknown(), platform);
    }

    @Test
    void shouldReadFromJsonWithOptionalValues() {
        // language=json
        String json =
                """
            {
              "architecture": "amd64",
              "variant": "v8",
              "os.features": ["linux-gnu"],
              "os.version": "1.0",
              "features": ["sse4", "avx2"],
              "os": "linux"
            }
            """;
        Platform platform = JsonUtils.fromJson(json, Platform.class);
        assertEquals("amd64", platform.architecture());
        assertEquals("linux", platform.os());
        assertEquals("v8", platform.variant());
        assertEquals(List.of("linux-gnu"), platform.osFeatures());
        assertEquals(List.of("sse4", "avx2"), platform.features());
        assertEquals(
                Platform.linuxAmd64()
                        .withOsVersion("1.0")
                        .withFeatures(List.of("sse4", "avx2"))
                        .withOsFeatures(List.of("linux-gnu"))
                        .withVariant("v8"),
                platform);

        json =
                """
            {
              "architecture": "unknown",
              "os": "unknown"
            }
            """;
        platform = JsonUtils.fromJson(json, Platform.class);
        assertEquals(Platform.unknown(), platform);
    }

    @Test
    void shouldCompareTwoPlatformWithoutVersion() {
        assertTrue(Platform.matches(Platform.linuxAmd64(), Platform.linuxAmd64().withOsVersion("1.0")));
        assertFalse(Platform.matches(Platform.windowsAmd64(), Platform.linuxAmd64()));
        assertFalse(
                Platform.matches(Platform.linuxAmd64(), Platform.linuxAmd64().withOsVersion("1.0"), true));
        assertTrue(Platform.matches(
                Platform.linuxAmd64().withOsVersion("1.0"),
                Platform.linuxAmd64().withOsVersion("1.0"),
                true));
        assertTrue(Platform.matches(Platform.linuxArmV6(), Platform.linuxArmV6()));
        assertFalse(Platform.matches(Platform.linuxArmV6(), Platform.linuxArmV7()));
    }
}
