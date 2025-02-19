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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import land.oras.ContainerRef;
import land.oras.credentials.FileStore;
import land.oras.credentials.FileStore.Credential;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileStoreAuthenticationProviderTest {

    @Mock
    private FileStore mockFileStore;

    private FileStoreAuthenticationProvider authProvider;

    public static final String REGISTRY = "localhost:5000";

    @Test
    void testGetAuthHeader() throws Exception {
        // Mock valid credentials for the server address
        Credential credential = new Credential("testUser", "testPassword");
        doReturn(credential).when(mockFileStore).get(any(ContainerRef.class));

        // Create the authentication provider
        authProvider = new FileStoreAuthenticationProvider(mockFileStore);

        // Verify that the getAuthHeader method returns the expected Basic Auth header
        String authHeader = authProvider.getAuthHeader(ContainerRef.fromUrl(REGISTRY));
        String expectedAuthString = "testUser:testPassword";
        String expectedEncodedAuth =
                "Basic " + Base64.getEncoder().encodeToString(expectedAuthString.getBytes(StandardCharsets.UTF_8));

        assertEquals(expectedEncodedAuth, authHeader);
    }
}
