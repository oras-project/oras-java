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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Objects;
import land.oras.utils.Const;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Record for platform information
 * @param os The operating system of the platform
 * @param architecture The architecture of the platform
 * @param variant The variant of the platform, which is optional and may be null
 * @param osVersion The operating system version of the platform, which is optional and may be
 * @param features The features of the platform, which is optional and may be null
 * @param osFeatures The operating system features of the platform, which is optional and may be
 */
@NullMarked
@OrasModel
@JsonPropertyOrder({
    Const.PLATFORM_OS,
    Const.PLATFORM_ARCHITECTURE,
    Const.PLATFORM_VARIANT,
    Const.PLATFORM_OS_VERSION,
    Const.PLATFORM_OS_FEATURES
})
public record Platform(
        @Nullable @JsonProperty(Const.PLATFORM_OS) String os,
        @Nullable @JsonProperty(Const.PLATFORM_ARCHITECTURE) String architecture,
        @Nullable @JsonProperty(Const.PLATFORM_OS_VERSION) String osVersion,
        @Nullable @JsonProperty(Const.PLATFORM_VARIANT) String variant,
        @Nullable @JsonProperty(Const.PLATFORM_FEATURES) List<String> features,
        @Nullable @JsonProperty(Const.PLATFORM_OS_FEATURES) List<String> osFeatures) {

    /**
     * Create a new platform linux/amd64
     * @return The platform
     */
    public static Platform linuxAmd64() {
        return of(Const.PLATFORM_LINUX, Const.PLATFORM_ARCHITECTURE_AMD64);
    }

    /**
     * Create a new platform windows/amd64
     * @return The platform
     */
    public static Platform windowsAmd64() {
        return of(Const.PLATFORM_WINDOWS, Const.PLATFORM_ARCHITECTURE_AMD64);
    }

    /**
     * Create a new platform linux/amd64
     * @return The platform
     */
    public static Platform linux386() {
        return of(Const.PLATFORM_LINUX, Const.PLATFORM_ARCHITECTURE_386);
    }

    /**
     * Create a new platform linux arm/v6
     * @return The platform
     */
    public static Platform linuxArmV6() {
        return of(Const.PLATFORM_LINUX, Const.PLATFORM_ARCHITECTURE_ARM, Const.VARIANT_V6);
    }

    /**
     * Create a new platform linux arm/v7
     * @return The platform
     */
    public static Platform linuxArmV7() {
        return of(Const.PLATFORM_LINUX, Const.PLATFORM_ARCHITECTURE_ARM, Const.VARIANT_V7);
    }

    /**
     * Create a new platform linux arm64/v8
     * @return The platform
     */
    public static Platform linuxArm64V8() {
        return of(Const.PLATFORM_LINUX, Const.PLATFORM_ARCHITECTURE_ARM64, Const.VARIANT_V8);
    }

    /**
     * Create a new platform ppc64le
     * @return The platform
     */
    public static Platform linuxPpc64le() {
        return of(Const.PLATFORM_LINUX, Const.PLATFORM_ARCHITECTURE_PPC64LE, null);
    }

    /**
     * Create a new platform riscv64
     * @return The platform
     */
    public static Platform linuxRiscv64() {
        return of(Const.PLATFORM_LINUX, Const.PLATFORM_ARCHITECTURE_RISCV64, null);
    }

    /**
     * Create a new platform s390x
     * @return The platform
     */
    public static Platform linuxS390x() {
        return of(Const.PLATFORM_LINUX, Const.PLATFORM_ARCHITECTURE_S390X, null);
    }

    /**
     * Create a new platform with empty os and architecture
     * @return The platform
     */
    public static Platform empty() {
        return new Platform(null, null, null, null, null, null);
    }

    /**
     * Create a new platform with unknown os and architecture
     * @return The platform
     */
    public static Platform unknown() {
        return of(Const.PLATFORM_UNKNOWN, Const.PLATFORM_UNKNOWN);
    }

    /**
     * Create a new platform with the given os and architecture
     * @param os The os of the platform
     * @param architecture The architecture of the platform
     * @return The platform
     */
    public static Platform of(String os, String architecture) {
        return new Platform(os, architecture, null, null, null, null);
    }

    /**
     * Create a new platform with the given os, architecture and variant
     * @param os The os of the platform
     * @param architecture The architecture of the platform
     * @param variant The variant of the platform
     * @return The platform
     */
    public static Platform of(String os, String architecture, @Nullable String variant) {
        return new Platform(os, architecture, null, variant, null, null);
    }

    /**
     * Return the os of the platform, or "unknown" if the os is null
     * @return The os of the platform
     */
    @Override
    public String os() {
        return os != null ? os : Const.PLATFORM_UNKNOWN;
    }

    /**
     * Return the os of the platform, or "unknown" if the os is null
     * @return The os of the platform
     */
    @Override
    public String architecture() {
        return architecture != null ? architecture : Const.PLATFORM_UNKNOWN;
    }

    /**
     * Create a new platform with the given features
     * @param features The features of the platform
     * @return The platform
     */
    public Platform withFeatures(List<String> features) {
        return new Platform(os, architecture, osVersion, variant, features, osFeatures);
    }

    /**
     * Create a new platform with the given variant
     * @param variant The variant of the platform
     * @return The platform
     */
    public Platform withVariant(String variant) {
        return new Platform(os, architecture, osVersion, variant, features, osFeatures);
    }

    /**
     * Create a new platform with the given os version
     * @param osVersion The os version of the platform
     * @return The platform
     */
    public Platform withOsVersion(String osVersion) {
        return new Platform(os, architecture, osVersion, variant, features, osFeatures);
    }

    /**
     * Create a new platform with the given os features
     * @param osFeatures The os features of the platform
     * @return The platform
     */
    public Platform withOsFeatures(List<String> osFeatures) {
        return new Platform(os, architecture, osVersion, variant, features, osFeatures);
    }

    /**
     * Return true if the platform is unspecified, which means both os and architecture are either empty or unknown
     * @param platform The platform to check
     * @return True if the platform is unspecified, false otherwise
     */
    public static boolean unspecified(Platform platform) {
        return platform.equals(Platform.empty()) || platform.equals(Platform.unknown());
    }

    /**
     * Check if 2 platform are matching, which means the os and architecture are the same (including variant)
     * @param platform The platform to check
     * @param target The target platform to match
     * @return True if the platform is matching, false otherwise
     */
    public static boolean matches(Platform platform, Platform target) {
        return matches(platform, target, false);
    }

    /**
     * Check if 2 platform are matching, which means the os and architecture are the same (including variant)
     * @param platform The platform to check
     * @param target The target platform to match
     * @param includeVersion Whether to include os version
     * @return True if the platform is matching, false otherwise
     */
    public static boolean matches(Platform platform, Platform target, boolean includeVersion) {
        if (!platform.os().equals(target.os()) || !platform.architecture().equals(target.architecture())) {
            return false;
        }
        if (!Objects.equals(platform.variant(), target.variant())) {
            return false;
        }
        if (includeVersion) {
            return Objects.equals(platform.osVersion(), target.osVersion());
        }
        return true;
    }
}
