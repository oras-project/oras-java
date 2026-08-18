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

package land.oras.exception;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import land.oras.OrasModel;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * An error object for OCI API
 */
@NullMarked
@OrasModel
public final class Error {

    private final String code;
    private final String message;
    private final @Nullable String details;

    /**
     * Create a new error instance
     * @param code The error code
     * @param message The error message
     * @param details The error details
     */
    @JsonCreator
    public Error(
            @JsonProperty("code") String code,
            @JsonProperty("message") String message,
            @JsonProperty("details") @Nullable String details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

    /**
     * Get the error code
     * @return The error code
     */
    @JsonProperty("code")
    public String code() {
        return code;
    }

    /**
     * Get the error message
     * @return The error message
     */
    @JsonProperty("message")
    public String message() {
        return message;
    }

    /**
     * Get the error details
     * @return The error details
     */
    @JsonProperty("details")
    public @Nullable String details() {
        return details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Error)) return false;
        Error that = (Error) o;
        return Objects.equals(code, that.code)
                && Objects.equals(message, that.message)
                && Objects.equals(details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message, details);
    }

    @Override
    public String toString() {
        return "Error[code=" + code + ", message=" + message + ", details=" + details + "]";
    }
}
