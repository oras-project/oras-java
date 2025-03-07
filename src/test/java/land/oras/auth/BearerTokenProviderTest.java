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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.ZonedDateTime;
import java.util.Map;
import land.oras.ContainerRef;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import land.oras.utils.OrasHttpClient;
import land.oras.utils.RegistryContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@WireMockTest
@Execution(ExecutionMode.SAME_THREAD)
public class BearerTokenProviderTest {

    @Container
    private final RegistryContainer registry = new RegistryContainer().withStartupAttempts(3);

    private final ContainerRef containerRef = ContainerRef.parse("localhost:5000/library/test:latest");

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldRefreshToken(WireMockRuntimeInfo wmRuntimeInfo) {

        // Mock responses
        OrasHttpClient.ResponseWrapper mockResponse = Mockito.mock(OrasHttpClient.ResponseWrapper.class);
        BearerTokenProvider.TokenResponse tokenResponse =
                new BearerTokenProvider.TokenResponse("fake-token", "access-token", 300, ZonedDateTime.now());
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.get(WireMock.urlEqualTo(
                        "/token?scope=repository:library/test:pull&service=%s".formatted(registry.getRegistry())))
                .willReturn(WireMock.okJson(JsonUtils.toJson(tokenResponse))));

        // Return WWW-Authenticate header from registry
        Mockito.when(mockResponse.headers())
                .thenReturn(Map.of(
                        Const.WWW_AUTHENTICATE_HEADER.toLowerCase(),
                        String.format(
                                "Bearer realm=\"%s/token\",service=\"%s\",scope=\"repository:library/test:pull\"",
                                wmRuntimeInfo.getHttpBaseUrl(), registry.getRegistry())));

        // Test
        BearerTokenProvider provider = new BearerTokenProvider(new UsernamePasswordProvider("user", "password"));
        provider.refreshToken(containerRef, OrasHttpClient.Builder.builder().build(), mockResponse);
        BearerTokenProvider.TokenResponse token = provider.getToken();

        // Assert tokens
        assertEquals("fake-token", token.token());
        assertEquals("access-token", token.access_token());
        assertEquals(300, token.expire_in());
        assertEquals(tokenResponse.issued_at(), token.issued_at());

        // Check the token header is set
        assertEquals("Bearer fake-token", provider.getAuthHeader(containerRef));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldTestTokenWithNoAuth(WireMockRuntimeInfo wmRuntimeInfo) {

        // Mock responses
        OrasHttpClient.ResponseWrapper mockResponse = Mockito.mock(OrasHttpClient.ResponseWrapper.class);
        BearerTokenProvider.TokenResponse tokenResponse =
                new BearerTokenProvider.TokenResponse("fake-token", "access-token", 300, ZonedDateTime.now());
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.get(WireMock.urlEqualTo(
                        "/token?scope=repository:library/test:pull&service=%s".formatted(registry.getRegistry())))
                .willReturn(WireMock.okJson(JsonUtils.toJson(tokenResponse))));

        // Return WWW-Authenticate header from registry
        Mockito.when(mockResponse.headers())
                .thenReturn(Map.of(
                        Const.WWW_AUTHENTICATE_HEADER.toLowerCase(),
                        String.format(
                                "Bearer realm=\"%s/token\",service=\"%s\",scope=\"repository:library/test:pull\"",
                                wmRuntimeInfo.getHttpBaseUrl(), registry.getRegistry())));

        // Test
        BearerTokenProvider provider = new BearerTokenProvider(new NoAuthProvider());
        provider.refreshToken(containerRef, OrasHttpClient.Builder.builder().build(), mockResponse);
        BearerTokenProvider.TokenResponse token = provider.getToken();

        // Assert tokens
        assertEquals("fake-token", token.token());
        assertEquals("access-token", token.access_token());
        assertEquals(300, token.expire_in());
        assertEquals(tokenResponse.issued_at(), token.issued_at());

        // Check the token header is set
        assertEquals("Bearer fake-token", provider.getAuthHeader(containerRef));
    }

    @Test
    void testNoRefreshedToken() {
        BearerTokenProvider provider = new BearerTokenProvider(new UsernamePasswordProvider("user", "password"));
        assertNull(provider.getAuthHeader(containerRef), "No token should be returned");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testInvalidWwwAuthentication() {
        OrasHttpClient.ResponseWrapper mockResponse = Mockito.mock(OrasHttpClient.ResponseWrapper.class);
        BearerTokenProvider provider = new BearerTokenProvider(new UsernamePasswordProvider("user", "password"));
        ContainerRef containerRef = ContainerRef.parse("localhost:5000/library/test:latest");
        assertThrows(OrasException.class, () -> {
            provider.refreshToken(containerRef, OrasHttpClient.Builder.builder().build(), mockResponse);
        });
        doReturn(Map.of(Const.WWW_AUTHENTICATE_HEADER.toLowerCase(), "invalid"))
                .when(mockResponse)
                .headers();
        assertThrows(OrasException.class, () -> {
            provider.refreshToken(containerRef, OrasHttpClient.Builder.builder().build(), mockResponse);
        });
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testWWWAuthenticateFormat(WireMockRuntimeInfo wmRuntimeInfo) {
        OrasHttpClient.ResponseWrapper mockResponse = Mockito.mock(OrasHttpClient.ResponseWrapper.class);

        BearerTokenProvider.TokenResponse tokenResponse =
                new BearerTokenProvider.TokenResponse("fake-token", "access-token", 300, ZonedDateTime.now());
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.get(WireMock.urlMatching("/token(.*)"))
                .willReturn(WireMock.okJson(JsonUtils.toJson(tokenResponse))));

        BearerTokenProvider provider = new BearerTokenProvider(new UsernamePasswordProvider("user", "password"));
        assertThrows(OrasException.class, () -> {
            provider.refreshToken(containerRef, OrasHttpClient.Builder.builder().build(), mockResponse);
        });

        // Without error
        doReturn(Map.of(
                        Const.WWW_AUTHENTICATE_HEADER.toLowerCase(),
                        "Bearer realm=\"%s/token\",service=\"%s\",scope=\"repository:repository/library:push,pull\""
                                .formatted(wmRuntimeInfo.getHttpBaseUrl(), registry.getRegistry())))
                .when(mockResponse)
                .headers();
        // No exception should be thrown
        provider.refreshToken(containerRef, OrasHttpClient.Builder.builder().build(), mockResponse);

        // With error
        doReturn(Map.of(
                        Const.WWW_AUTHENTICATE_HEADER.toLowerCase(),
                        "Bearer realm=\"%s/token\",service=\"%s\",scope=\"repository/library:push,pull\",error=\"insufficient_scope\""
                                .formatted(wmRuntimeInfo.getHttpBaseUrl(), registry.getRegistry())))
                .when(mockResponse)
                .headers();
        // No exception should be thrown
        provider.refreshToken(containerRef, OrasHttpClient.Builder.builder().build(), mockResponse);
    }
}
