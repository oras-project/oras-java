package land.oras.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.ZonedDateTime;
import java.util.Map;
import land.oras.OrasException;
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

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldTestToken(WireMockRuntimeInfo wmRuntimeInfo) {

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
        provider.refreshToken(mockResponse);
        BearerTokenProvider.TokenResponse token = provider.getToken();

        // Assert tokens
        assertEquals("fake-token", token.token());
        assertEquals("access-token", token.access_token());
        assertEquals(300, token.expire_in());
        assertEquals(tokenResponse.issued_at(), token.issued_at());

        // Check the token header is set
        assertEquals("Bearer fake-token", provider.getAuthHeader());
    }

    @Test
    void testNoRefreshedToken() {
        BearerTokenProvider provider = new BearerTokenProvider(new UsernamePasswordProvider("user", "password"));
        assertThrows(OrasException.class, provider::getAuthHeader);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testInvalidWwwAuthentication() {
        OrasHttpClient.ResponseWrapper mockResponse = Mockito.mock(OrasHttpClient.ResponseWrapper.class);
        BearerTokenProvider provider = new BearerTokenProvider(new UsernamePasswordProvider("user", "password"));
        assertThrows(OrasException.class, () -> {
            provider.refreshToken(mockResponse);
        });
        doReturn(Map.of(Const.WWW_AUTHENTICATE_HEADER.toLowerCase(), "invalid"))
                .when(mockResponse)
                .headers();
        assertThrows(OrasException.class, () -> {
            provider.refreshToken(mockResponse);
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
            provider.refreshToken(mockResponse);
        });

        // Without error
        doReturn(Map.of(
                        Const.WWW_AUTHENTICATE_HEADER.toLowerCase(),
                        "Bearer realm=\"%s/token\",service=\"%s\",scope=\"repository:repository/library:push,pull\""
                                .formatted(wmRuntimeInfo.getHttpBaseUrl(), registry.getRegistry())))
                .when(mockResponse)
                .headers();
        // No exception should be thrown
        provider.refreshToken(mockResponse);

        // With error
        doReturn(Map.of(
                        Const.WWW_AUTHENTICATE_HEADER.toLowerCase(),
                        "Bearer realm=\"%s/token\",service=\"%s\",scope=\"repository/library:push,pull\",error=\"insufficient_scope\""
                                .formatted(wmRuntimeInfo.getHttpBaseUrl(), registry.getRegistry())))
                .when(mockResponse)
                .headers();
        // No exception should be thrown
        provider.refreshToken(mockResponse);
    }
}
