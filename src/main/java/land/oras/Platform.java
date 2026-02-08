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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Map;
import land.oras.utils.Const;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Record for platform information
 * @param annotations Platform annotations, which can include os and architecture information
 */
@NullMarked
@OrasModel
@JsonPropertyOrder({Const.PLATFORM_OS, Const.PLATFORM_ARCHITECTURE})
public record Platform(@Nullable @JsonIgnore Map<String, String> annotations) {

    /**
     * Constructor for JSON deserialization
     * @param annotations Platform annotations, which can include os and architecture information
     */
    @JsonCreator
    public Platform {}

    /**
     * Create a new platform with the given annotations
     * @param annotations Platform annotations, which can include os and architecture information
     * @return The platform
     */
    public static Platform of(@Nullable Map<String, String> annotations) {
        return new Platform(annotations);
    }

    /**
     * Create a new platform linux/amd64
     * @return The platform
     */
    public static Platform linuxAmd64() {
        return of(Const.PLATFORM_LINUX, Const.PLATFORM_ARCHITECTURE_AMD64);
    }

    /**
     * Create a new platform with empty os and architecture
     * @return The platform
     */
    public static Platform empty() {
        return new Platform(null);
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
        return new Platform(Map.of(Const.PLATFORM_OS, os, Const.PLATFORM_ARCHITECTURE, architecture));
    }

    /**
     * Create a new platform with the given os, architecture and variant
     * @param os The os of the platform
     * @param architecture The architecture of the platform
     * @param variant The variant of the platform
     * @return The platform
     */
    public static Platform of(String os, String architecture, @Nullable String variant) {
        if (variant == null) {
            return of(os, architecture);
        }
        return new Platform(Map.of(
                Const.PLATFORM_OS, os,
                Const.PLATFORM_ARCHITECTURE, architecture,
                Const.PLATFORM_VARIANT, variant));
    }

    /**
     * Return the architecture of the platform, or "unknown" if not specified
     * @return The architecture of the platform
     */
    @JsonGetter
    public String os() {
        return annotations != null
                ? annotations.getOrDefault(Const.PLATFORM_OS, Const.PLATFORM_UNKNOWN)
                : Const.PLATFORM_UNKNOWN;
    }

    /**
     * Return the architecture of the platform, or "unknown" if not specified
     * @return The architecture of the platform
     */
    @JsonGetter
    public String architecture() {
        return annotations != null
                ? annotations.getOrDefault(Const.PLATFORM_ARCHITECTURE, Const.PLATFORM_UNKNOWN)
                : Const.PLATFORM_UNKNOWN;
    }

    /**
     * Return the variant of the platform, or null if not specified
     * @return The variant of the platform
     */
    @JsonGetter
    public @Nullable String variant() {
        return annotations != null ? annotations.get(Const.PLATFORM_VARIANT) : null;
    }

    /**
     * Return true if the platform is unspecified, which means both os and architecture are either empty or unknown
     * @param platform The platform to check
     * @return True if the platform is unspecified, false otherwise
     */
    public static boolean unspecified(Platform platform) {
        return platform.equals(Platform.empty()) || platform.equals(Platform.unknown());
    }
}
