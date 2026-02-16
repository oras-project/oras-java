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

import land.oras.auth.HttpClient;
import land.oras.utils.JsonUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception for ORAS
 */
@NullMarked
public class OrasException extends RuntimeException {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(OrasException.class);

    /**
     * Possible error response
     */
    private @Nullable Error error;

    /**
     * Status code
     */
    private @Nullable Integer statusCode;

    /**
     * Constructor
     * @param message The message
     */
    public OrasException(String message) {
        super(message);
    }

    /**
     * New exception with a message and a response
     * @param response The response
     */
    public OrasException(HttpClient.ResponseWrapper<String> response) {
        this("Response code: " + response.statusCode());
        try {
            this.statusCode = response.statusCode();
            error = JsonUtils.fromJson(response.response(), Error.class);
        } catch (Exception e) {
            LOG.debug("Failed to parse error response", e);
        }
    }

    /**
     * Constructor
     * @param statusCode The status code
     * @param message The message
     */
    public OrasException(int statusCode, String message) {
        this(message);
        this.statusCode = statusCode;
    }

    /**
     * Constructor
     * @param message The message
     * @param cause The cause
     */
    public OrasException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Get the error
     * @return The error
     */
    public @Nullable Error getError() {
        return error;
    }

    /**
     * Get the status code
     * @return The status code
     */
    public Integer getStatusCode() {
        if (statusCode == null) {
            return -1;
        }
        return statusCode;
    }
}
