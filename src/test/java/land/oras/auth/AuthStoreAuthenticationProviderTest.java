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

package land.oras.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import land.oras.ContainerRef;
import land.oras.auth.AuthStore.Credential;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
class AuthStoreAuthenticationProviderTest {

    @Mock
    private AuthStore mockAuthStore;

    public static final String REGISTRY = "localhost:5000";

    @Test
    void testGetAuthHeader() throws Exception {
        // Mock valid credentials for the server address
        Credential credential = new Credential("testUser", "testPassword");
        doReturn(credential).when(mockAuthStore).get(any(ContainerRef.class));

        // Create the authentication provider
        AuthStoreAuthenticationProvider authProvider = new AuthStoreAuthenticationProvider(mockAuthStore);

        // Verify that the getAuthHeader method returns the expected Basic Auth header
        String authHeader = authProvider.getAuthHeader(ContainerRef.parse("%s/%s".formatted(REGISTRY, "alpine")));
        String expectedAuthString = "testUser:testPassword";
        String expectedEncodedAuth =
                "Basic " + Base64.getEncoder().encodeToString(expectedAuthString.getBytes(StandardCharsets.UTF_8));

        assertEquals(expectedEncodedAuth, authHeader);
    }

    @Test
    void testGetNullAuthHeader() throws Exception {

        // No credentials
        doReturn(null).when(mockAuthStore).get(any(ContainerRef.class));

        // Create the authentication provider
        AuthStoreAuthenticationProvider authProvider = new AuthStoreAuthenticationProvider(mockAuthStore);

        // Verify that the getAuthHeader method returns the expected Basic Auth header
        String authHeader = authProvider.getAuthHeader(ContainerRef.parse("%s/%s".formatted(REGISTRY, "alpine")));
        String expectedAuthString = "testUser:testPassword";
        String expectedEncodedAuth =
                "Basic " + Base64.getEncoder().encodeToString(expectedAuthString.getBytes(StandardCharsets.UTF_8));

        assertNull(authHeader, "Auth header should be null");
    }

    @Test
    void testDefaultLocation() {
        new AuthStoreAuthenticationProvider();
    }
}
