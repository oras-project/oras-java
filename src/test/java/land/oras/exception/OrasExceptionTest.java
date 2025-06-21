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

package land.oras.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import land.oras.auth.HttpClient;
import land.oras.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class OrasExceptionTest {

    @Test
    void shouldWrapException() {
        OrasException orasException = new OrasException("message", new RuntimeException());

        // Getters
        assertEquals("message", orasException.getMessage(), "Message should be correct");
        assertNull(orasException.getError(), "Error should be null");
        assertEquals(-1, orasException.getStatusCode(), "Status code should be -1");
    }

    @Test
    void shouldWrapResponse() {
        HttpClient.ResponseWrapper<String> response = new HttpClient.ResponseWrapper<>(
                JsonUtils.toJson(new Error("5001", "foo", "the details")), 500, Map.of());
        OrasException orasException = new OrasException(response);

        // Getters
        assertEquals("Response code: 500", orasException.getMessage(), "Message should be correct");
        assertNotNull(orasException.getError(), "Error should not be null");
        assertEquals(500, orasException.getStatusCode(), "Status code should be 500");
        assertEquals("5001", orasException.getError().code(), "Code should be correct");
        assertEquals("foo", orasException.getError().message(), "Message should be correct");
        assertEquals("the details", orasException.getError().details(), "Details should be correct");
    }

    @Test
    void shouldWrapInvalidResponse() {
        HttpClient.ResponseWrapper<String> response = new HttpClient.ResponseWrapper<>("corrupted", 500, Map.of());
        OrasException orasException = new OrasException(response);
        assertEquals("Response code: 500", orasException.getMessage(), "Message should be correct");
        assertNull(orasException.getError(), "Error should be null");
        assertEquals(500, orasException.getStatusCode(), "Status code should be 500");
    }
}
