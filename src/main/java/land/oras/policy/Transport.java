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

package land.oras.policy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The transport of a containers policy scope
 * This SDK only operates on registry images, so only the {@code "docker"}
 */
@NullMarked
public enum Transport {

    /**
     * Docker transport (OCI)
     */
    DOCKER("docker"),

    /**
     * All other values
     */
    UNKNOWN("unknown");

    private final String value;

    Transport(String value) {
        this.value = value;
    }

    /**
     * Return the lower-case JSON value of this transport.
     *
     * @return the transport value, e.g. {@code "docker"}.
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Resolve transport from its JSON value
     * @param value The value to parse
     * @return the matching transport, never {@code null}.
     */
    @JsonCreator
    public static Transport fromValue(@Nullable String value) {
        return DOCKER.value.equals(value) ? DOCKER : UNKNOWN;
    }

    @Override
    public String toString() {
        return value;
    }
}
