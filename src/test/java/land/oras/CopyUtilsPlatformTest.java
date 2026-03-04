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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Tests for {@link CopyUtils} platform filtering functionality
 */
@Execution(ExecutionMode.CONCURRENT)
class CopyUtilsPlatformTest {

    @Test
    void testCopyOptionsDefaults() {
        CopyUtils.CopyOptions shallow = CopyUtils.CopyOptions.shallow();
        assertFalse(shallow.includeReferrers());
        assertNull(shallow.targetPlatforms());
        assertTrue(shallow.includeUnspecifiedPlatforms());
        assertFalse(shallow.isPlatformFilteringEnabled());

        CopyUtils.CopyOptions deep = CopyUtils.CopyOptions.deep();
        assertTrue(deep.includeReferrers());
        assertNull(deep.targetPlatforms());
        assertTrue(deep.includeUnspecifiedPlatforms());
        assertFalse(deep.isPlatformFilteringEnabled());
    }

    @Test
    void testCopyOptionsWithPlatforms() {
        List<Platform> platforms = List.of(Platform.linuxAmd64(), Platform.windowsAmd64());

        CopyUtils.CopyOptions shallow = CopyUtils.CopyOptions.withPlatforms(false, platforms);
        assertFalse(shallow.includeReferrers());
        assertEquals(platforms, shallow.targetPlatforms());
        assertFalse(shallow.includeUnspecifiedPlatforms());
        assertTrue(shallow.isPlatformFilteringEnabled());

        CopyUtils.CopyOptions deep = CopyUtils.CopyOptions.withPlatforms(true, platforms);
        assertTrue(deep.includeReferrers());
        assertEquals(platforms, deep.targetPlatforms());
        assertFalse(deep.includeUnspecifiedPlatforms());
        assertTrue(deep.isPlatformFilteringEnabled());
    }

    @Test
    void testCopyOptionsWithPlatformsAndUnspecified() {
        List<Platform> platforms = List.of(Platform.linuxAmd64());

        CopyUtils.CopyOptions options = CopyUtils.CopyOptions.withPlatforms(false, platforms, true);
        assertFalse(options.includeReferrers());
        assertEquals(platforms, options.targetPlatforms());
        assertTrue(options.includeUnspecifiedPlatforms());
        assertTrue(options.isPlatformFilteringEnabled());
    }

    @Test
    void testShouldIncludeManifestWithoutPlatformFiltering() {
        CopyUtils.CopyOptions options = CopyUtils.CopyOptions.shallow();

        ManifestDescriptor linuxManifest = ManifestDescriptor.of(
                        "application/vnd.oci.image.manifest.v1+json", "sha256:123", 1000)
                .withPlatform(Platform.linuxAmd64());
        ManifestDescriptor windowsManifest = ManifestDescriptor.of(
                        "application/vnd.oci.image.manifest.v1+json", "sha256:456", 1000)
                .withPlatform(Platform.windowsAmd64());
        ManifestDescriptor unspecifiedManifest =
                ManifestDescriptor.of("application/vnd.oci.image.manifest.v1+json", "sha256:789", 1000);

        // Without platform filtering, all manifests should be included
        assertTrue(options.shouldIncludeManifest(linuxManifest));
        assertTrue(options.shouldIncludeManifest(windowsManifest));
        assertTrue(options.shouldIncludeManifest(unspecifiedManifest));
    }

    @Test
    void testShouldIncludeManifestWithPlatformFiltering() {
        List<Platform> targetPlatforms = List.of(Platform.linuxAmd64());
        CopyUtils.CopyOptions options = CopyUtils.CopyOptions.withPlatforms(false, targetPlatforms, false);

        ManifestDescriptor linuxManifest = ManifestDescriptor.of(
                        "application/vnd.oci.image.manifest.v1+json", "sha256:123", 1000)
                .withPlatform(Platform.linuxAmd64());
        ManifestDescriptor windowsManifest = ManifestDescriptor.of(
                        "application/vnd.oci.image.manifest.v1+json", "sha256:456", 1000)
                .withPlatform(Platform.windowsAmd64());
        ManifestDescriptor unspecifiedManifest =
                ManifestDescriptor.of("application/vnd.oci.image.manifest.v1+json", "sha256:789", 1000);

        // With platform filtering, only linux should be included
        assertTrue(options.shouldIncludeManifest(linuxManifest));
        assertFalse(options.shouldIncludeManifest(windowsManifest));
        assertFalse(options.shouldIncludeManifest(unspecifiedManifest)); // includeUnspecifiedPlatforms is false
    }

    @Test
    void testShouldIncludeManifestWithPlatformFilteringAndUnspecified() {
        List<Platform> targetPlatforms = List.of(Platform.linuxAmd64());
        CopyUtils.CopyOptions options = CopyUtils.CopyOptions.withPlatforms(false, targetPlatforms, true);

        ManifestDescriptor linuxManifest = ManifestDescriptor.of(
                        "application/vnd.oci.image.manifest.v1+json", "sha256:123", 1000)
                .withPlatform(Platform.linuxAmd64());
        ManifestDescriptor windowsManifest = ManifestDescriptor.of(
                        "application/vnd.oci.image.manifest.v1+json", "sha256:456", 1000)
                .withPlatform(Platform.windowsAmd64());
        ManifestDescriptor unspecifiedManifest =
                ManifestDescriptor.of("application/vnd.oci.image.manifest.v1+json", "sha256:789", 1000);

        // With platform filtering and includeUnspecified=true
        assertTrue(options.shouldIncludeManifest(linuxManifest));
        assertFalse(options.shouldIncludeManifest(windowsManifest));
        assertTrue(options.shouldIncludeManifest(unspecifiedManifest)); // includeUnspecifiedPlatforms is true
    }

    @Test
    void testShouldIncludeManifestWithMultiplePlatforms() {
        List<Platform> targetPlatforms = List.of(Platform.linuxAmd64(), Platform.windowsAmd64());
        CopyUtils.CopyOptions options = CopyUtils.CopyOptions.withPlatforms(false, targetPlatforms, false);

        ManifestDescriptor linuxManifest = ManifestDescriptor.of(
                        "application/vnd.oci.image.manifest.v1+json", "sha256:123", 1000)
                .withPlatform(Platform.linuxAmd64());
        ManifestDescriptor windowsManifest = ManifestDescriptor.of(
                        "application/vnd.oci.image.manifest.v1+json", "sha256:456", 1000)
                .withPlatform(Platform.windowsAmd64());
        ManifestDescriptor linuxArmManifest = ManifestDescriptor.of(
                        "application/vnd.oci.image.manifest.v1+json", "sha256:789", 1000)
                .withPlatform(Platform.linuxArmV7());

        // With multiple target platforms
        assertTrue(options.shouldIncludeManifest(linuxManifest));
        assertTrue(options.shouldIncludeManifest(windowsManifest));
        assertFalse(options.shouldIncludeManifest(linuxArmManifest));
    }

    @Test
    void testShouldIncludeManifestWithVariantMatching() {
        List<Platform> targetPlatforms = List.of(Platform.linuxArmV7());
        CopyUtils.CopyOptions options = CopyUtils.CopyOptions.withPlatforms(false, targetPlatforms, false);

        ManifestDescriptor armV6Manifest = ManifestDescriptor.of(
                        "application/vnd.oci.image.manifest.v1+json", "sha256:123", 1000)
                .withPlatform(Platform.linuxArmV6());
        ManifestDescriptor armV7Manifest = ManifestDescriptor.of(
                        "application/vnd.oci.image.manifest.v1+json", "sha256:456", 1000)
                .withPlatform(Platform.linuxArmV7());

        // Variants should match exactly
        assertFalse(options.shouldIncludeManifest(armV6Manifest));
        assertTrue(options.shouldIncludeManifest(armV7Manifest));
    }

    @Test
    void testPlatformFilteringWithEmptyTargetPlatforms() {
        CopyUtils.CopyOptions options = CopyUtils.CopyOptions.withPlatforms(false, List.of(), false);

        ManifestDescriptor linuxManifest = ManifestDescriptor.of(
                        "application/vnd.oci.image.manifest.v1+json", "sha256:123", 1000)
                .withPlatform(Platform.linuxAmd64());

        // Empty target platforms means no filtering
        assertFalse(options.isPlatformFilteringEnabled());
        assertTrue(options.shouldIncludeManifest(linuxManifest));
    }
}
