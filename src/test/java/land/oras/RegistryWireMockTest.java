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

package land.oras;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import land.oras.auth.AuthStore;
import land.oras.auth.AuthStoreAuthenticationProvider;
import land.oras.auth.BearerTokenProvider;
import land.oras.auth.HttpClient;
import land.oras.auth.NoAuthProvider;
import land.oras.auth.Scopes;
import land.oras.auth.UsernamePasswordProvider;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import land.oras.utils.SupportedAlgorithm;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@WireMockTest
@Execution(ExecutionMode.SAME_THREAD)
class RegistryWireMockTest {

    private final UsernamePasswordProvider authProvider = new UsernamePasswordProvider("myuser", "mypass");

    @TempDir
    private Path configDir;

    @Test
    void shouldRedirectWhenDownloadingBlob(WireMockRuntimeInfo wmRuntimeInfo) {

        String digest = SupportedAlgorithm.SHA256.digest("blob-data".getBytes());

        // Return data from wiremock
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.any(WireMock.urlEqualTo("/v2/library/artifact-text/blobs/%s".formatted(digest)))
                .willReturn(WireMock.temporaryRedirect("http://localhost:%d/v2/library/artifact-text/blobs/sha256:other"
                        .formatted(wmRuntimeInfo.getHttpPort()))));

        // Return blob on new location
        wireMock.register(head(WireMock.urlEqualTo("/v2/library/artifact-text/blobs/%s".formatted(digest)))
                .willReturn(WireMock.ok()));
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/v2/library/artifact-text/blobs/sha256:other"))
                .willReturn(
                        WireMock.ok().withBody("blob-data").withHeader(Const.DOCKER_CONTENT_DIGEST_HEADER, digest)));

        // Insecure registry
        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .build();

        ContainerRef containerRef =
                ContainerRef.parse("localhost:%d/library/artifact-text".formatted(wmRuntimeInfo.getHttpPort()));
        byte[] blob = registry.getBlob(containerRef.withDigest(digest));
        assertEquals("blob-data", new String(blob));
    }

    @Test
    void shouldRedirectWhenPushingBlob(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

        // Return data from wiremock
        WireMock wireMock = wmRuntimeInfo.getWireMock();

        // Return location without domain when pushing blob
        wireMock.register(head(WireMock.urlPathMatching("/v2/library/artifact-redirect/blobs/.*"))
                .willReturn(WireMock.status(404)));
        wireMock.register(WireMock.post(WireMock.urlPathMatching("/v2/library/artifact-redirect/blobs/uploads/.*"))
                .willReturn(WireMock.status(202).withHeader("Location", "/foobar")));

        // Push is on foobar
        wireMock.register(WireMock.put(WireMock.urlPathMatching("/foobar.*")).willReturn(WireMock.status(201)));

        // Insecure registry
        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .build();

        ContainerRef containerRef = ContainerRef.parse(
                "localhost:%d/library/artifact-redirect@sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
                        .formatted(wmRuntimeInfo.getHttpPort()));
        registry.pushBlob(containerRef, "hello".getBytes());

        // Via file
        Path testFile = configDir.resolve("test-data.temp");
        Files.writeString(testFile, "Test Content");
        registry.pushBlob(containerRef, testFile);
    }

    @Test
    void shouldNotSendAuthHeaderOnRedirectToDifferentDomain(WireMockRuntimeInfo wmRuntimeInfo) {
        String digest = SupportedAlgorithm.SHA256.digest("blob-data".getBytes());

        // Setup second WireMock instance on a different port
        WireMockServer redirectTarget =
                new WireMockServer(WireMockConfiguration.options().dynamicPort());
        redirectTarget.start();

        try {
            String redirectUrl = "http://localhost:%d/v2/other/blobs/sha256:other".formatted(redirectTarget.port());

            WireMock mainMock = wmRuntimeInfo.getWireMock();

            // Main mock responds with redirect to different domain
            mainMock.register(WireMock.any(WireMock.urlEqualTo("/v2/library/artifact/blobs/%s".formatted(digest)))
                    .willReturn(WireMock.temporaryRedirect(redirectUrl)));

            // Secondary server returns blob, we inspect headers here
            redirectTarget.stubFor(WireMock.get(WireMock.urlEqualTo("/v2/other/blobs/sha256:other"))
                    .willReturn(WireMock.ok()
                            .withBody("blob-data")
                            .withHeader(Const.DOCKER_CONTENT_DIGEST_HEADER, digest)));

            redirectTarget.stubFor(WireMock.head(WireMock.urlEqualTo("/v2/other/blobs/sha256:other"))
                    .willReturn(WireMock.ok()));

            // Registry setup with auth that would inject an Authorization header
            Registry registry = Registry.Builder.builder()
                    .withAuthProvider(authProvider)
                    .withInsecure(true)
                    .build();

            ContainerRef containerRef =
                    ContainerRef.parse("localhost:%d/library/artifact".formatted(wmRuntimeInfo.getHttpPort()));
            byte[] blob = registry.getBlob(containerRef.withDigest(digest));

            assertEquals("blob-data", new String(blob));

            // Assert Authorization header was not sent to the redirect target
            redirectTarget.verify(
                    1,
                    WireMock.getRequestedFor(WireMock.urlEqualTo("/v2/other/blobs/sha256:other"))
                            .withoutHeader("Authorization"));
        } finally {
            redirectTarget.stop();
        }
    }

    @Test
    void shouldListTags(WireMockRuntimeInfo wmRuntimeInfo) {

        // Return data from wiremock
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/v2/library/artifact-text/tags/list"))
                .willReturn(WireMock.okJson(JsonUtils.toJson(new Tags("artifact-text", List.of("latest", "0.1.1"))))));

        // Insecure registry
        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .build();

        // Test
        List<String> tags = registry.getTags(ContainerRef.parse("%s/library/artifact-text"
                        .formatted(wmRuntimeInfo.getHttpBaseUrl().replace("http://", ""))))
                .tags();

        // Assert
        assertEquals(2, tags.size());
        assertEquals("latest", tags.get(0));
        assertEquals("0.1.1", tags.get(1));
    }

    @Test
    void shouldListRepositories(WireMockRuntimeInfo wmRuntimeInfo) {

        // Return data from wiremock
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/v2/_catalog"))
                .willReturn(
                        WireMock.okJson(JsonUtils.toJson(new Repositories(List.of("foo", "bar", "library/alpine"))))));

        // Insecure registry
        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .withRegistry(wmRuntimeInfo.getHttpBaseUrl().replace("http://", ""))
                .build();

        // Test
        List<String> repositories = registry.getRepositories().repositories();

        // Assert
        assertEquals(3, repositories.size());
        assertEquals("foo", repositories.get(0));
        assertEquals("bar", repositories.get(1));
        assertEquals("library/alpine", repositories.get(2));
    }

    @Test
    void shouldListTagsWithFileStoreAuth(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

        // Auth file for current registry
        String authFile =
                """
                {
                        "auths": {
                                "localhost:%d": {
                                        "auth": "bXl1c2VyOm15cGFzcw=="
                                }
                        }
                }
                """
                        .formatted(wmRuntimeInfo.getHttpPort());

        Files.writeString(configDir.resolve("config.json"), authFile, StandardCharsets.UTF_8);

        AuthStoreAuthenticationProvider authProvider =
                new AuthStoreAuthenticationProvider(AuthStore.newStore(List.of(configDir.resolve("config.json"))));

        // Return data from wiremock
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/v2/library/artifact-text-store/tags/list"))
                .willReturn(WireMock.okJson(
                        JsonUtils.toJson(new Tags("artifact-text-store", List.of("latest", "0.1.1"))))));

        // Insecure registry
        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .build();

        // Test
        List<String> tags = registry.getTags(ContainerRef.parse("%s/library/artifact-text-store"
                        .formatted(wmRuntimeInfo.getHttpBaseUrl().replace("http://", ""))))
                .tags();

        // Assert
        assertEquals(2, tags.size());
        assertEquals("latest", tags.get(0));
        assertEquals("0.1.1", tags.get(1));
    }

    // Errors from registry
    @Test
    void shouldHandle500Error(WireMockRuntimeInfo wmRuntimeInfo) {
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        String registryUrl = wmRuntimeInfo.getHttpBaseUrl().replace("http://", "");

        // Register the wiremock
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/v2/library/error-artifact/tags/list"))
                .willReturn(WireMock.serverError().withBody("Internal Server Error")));
        wireMock.register(head(WireMock.urlEqualTo("/v2/library/error-artifact/blobs/sha256:1234"))
                .willReturn(WireMock.serverError().withBody("Internal Server Error")));
        Registry registry = Registry.Builder.builder().withInsecure(true).build();

        // Now we should have a reference to container
        ContainerRef ref = ContainerRef.parse("%s/library/error-artifact".formatted(registryUrl));

        OrasException exception = assertThrows(OrasException.class, () -> registry.getTags(ref));
        assertEquals(500, exception.getStatusCode());

        ContainerRef ref2 = ContainerRef.parse("%s/library/error-artifact@sha256:1234".formatted(registryUrl));
        OrasException exception2 = assertThrows(OrasException.class, () -> registry.fetchBlobDescriptor(ref2));
        assertEquals(500, exception2.getStatusCode());
    }

    // Timeout with similar structure as previous test and request 408 with different artifact name
    @Test
    void shouldHandleTimeout(WireMockRuntimeInfo wmRuntimeInfo) {
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        String registryUrl = wmRuntimeInfo.getHttpBaseUrl().replace("http://", "");

        // Using here a unique container reference to avoid conflicts when running in parallel
        ContainerRef ref = ContainerRef.parse("%s/library/timeout-artifact".formatted(registryUrl));

        // We Set up the stub for the timeout scenario
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/v2/library/timeout-artifact/tags/list"))
                .willReturn(WireMock.aResponse().withStatus(408).withBody("Request timed out")));

        Registry registry = Registry.Builder.builder().withInsecure(true).build();

        OrasException exception = assertThrows(OrasException.class, () -> registry.getTags(ref));
        assertEquals(408, exception.getStatusCode());
    }

    // Note: Currently this test is @Disabled because the retry functionality isn't implemented.
    // remove the @Disabled annotation and the test should pass.
    @Test
    @Disabled("Automatic retry not yet implemented in SDK")
    void shouldRetryBlobUpload(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        String registryUrl = wmRuntimeInfo.getHttpBaseUrl().replace("http://", "");
        String uploadUrl = "/v2/library/artifact-text/blobs/uploads/";

        // Setting up WireMock to simulate a failed first attempt
        wireMock.register(WireMock.post(WireMock.urlEqualTo(uploadUrl))
                .inScenario("upload retry scenario")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(WireMock.serverError())
                .willSetStateTo("retry"));

        // Setting up WireMock for successful retry
        wireMock.register(WireMock.post(WireMock.urlEqualTo(uploadUrl))
                .inScenario("upload retry scenario")
                .whenScenarioStateIs("retry")
                .willReturn(WireMock.aResponse().withStatus(202).withHeader("Location", uploadUrl + "12345")));

        wireMock.register(
                WireMock.put(WireMock.urlMatching(uploadUrl + "12345.*")).willReturn(WireMock.created()));

        Registry registry = Registry.Builder.builder().withInsecure(true).build();
        ContainerRef ref = ContainerRef.parse("%s/library/artifact-text".formatted(registryUrl));

        Path testFile = configDir.resolve("test-data.temp");
        Files.writeString(testFile, "Test Content");

        try (InputStream inputStream = Files.newInputStream(testFile)) {
            // when retry is implemented we dont want to throw an exception here as it will retry
            Layer layer = registry.pushBlob(ref, inputStream);

            // assertions will verify that the upload succeeded after retry
            assertNotNull(layer);
            assertNotNull(layer.getDigest());
        }
    }

    @Test
    void shouldGetToken(WireMockRuntimeInfo wmRuntimeInfo) {
        byte[] blob = tokenScenario(wmRuntimeInfo, "get-token", "token", null);
        assertEquals("blob-data", new String(blob));
    }

    @Test
    void shouldGetAuthToken(WireMockRuntimeInfo wmRuntimeInfo) {
        byte[] blob = tokenScenario(wmRuntimeInfo, "get-auth-token", null, "access-token");
        assertEquals("blob-data", new String(blob));
    }

    @Test
    void shouldThrowIfNoTokenFound(WireMockRuntimeInfo wmRuntimeInfo) {
        assertThrows(OrasException.class, () -> {
            tokenScenario(wmRuntimeInfo, "get-auth-token", null, null);
        });
    }

    @Test
    void shouldRefreshExpiredToken(WireMockRuntimeInfo wmRuntimeInfo) {

        String digest = SupportedAlgorithm.SHA256.digest("blob-data".getBytes());

        // Return data from wiremock
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.any(WireMock.urlEqualTo("/v2/library/refresh-token/blobs/%s".formatted(digest)))
                .inScenario("get token")
                .willReturn(WireMock.forbidden()
                        .withHeader(
                                Const.WWW_AUTHENTICATE_HEADER,
                                "Bearer realm=\"http://localhost:%d/token\",service=\"localhost\",scope=\"repository:library/refresh-token:pull\""
                                        .formatted(wmRuntimeInfo.getHttpPort()))));

        // Return token
        wireMock.register(WireMock.any(
                        WireMock.urlEqualTo("/token?scope=repository:library/refresh-token:pull&service=localhost"))
                .inScenario("get token")
                .willSetStateTo("get")
                .willReturn(WireMock.okJson(JsonUtils.toJson(
                        new HttpClient.TokenResponse("fake-token", "access-token", 300, ZonedDateTime.now())))));

        // On the second call we return ok
        wireMock.register(WireMock.any(WireMock.urlEqualTo("/v2/library/refresh-token/blobs/%s".formatted(digest)))
                .inScenario("get token")
                .whenScenarioStateIs("get")
                .willReturn(
                        WireMock.ok().withBody("blob-data").withHeader(Const.DOCKER_CONTENT_DIGEST_HEADER, digest)));

        // Insecure registry
        Registry registry = Registry.Builder.builder()
                .withAuthProvider(new BearerTokenProvider()) // Already bearer token
                .withInsecure(true)
                .build();

        ContainerRef containerRef =
                ContainerRef.parse("localhost:%d/library/refresh-token".formatted(wmRuntimeInfo.getHttpPort()));
        byte[] blob = registry.getBlob(containerRef.withDigest(digest));
        assertEquals("blob-data", new String(blob));
    }

    @Test
    void shouldExecutePatchRequestWithHeaders(WireMockRuntimeInfo wMockRuntimeInfo) {
        WireMock wireMock = wMockRuntimeInfo.getWireMock();
        String registryUrl = wMockRuntimeInfo.getHttpBaseUrl().replace("http://", "");
        HttpClient client = HttpClient.Builder.builder().withSkipTlsVerify(true).build();

        // Setup Mock to craete a PATCH request with Headers
        wireMock.register(patch(urlEqualTo("/v2/test/blobs/uploads/session1"))
                .withHeader(Const.CONTENT_TYPE_HEADER, equalTo(Const.APPLICATION_OCTET_STREAM_HEADER_VALUE))
                .withHeader(Const.CONTENT_RANGE_HEADER, equalTo("0-1023"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader(Const.LOCATION_HEADER, "/v2/test/blobs/uploads/session2")
                        .withHeader(Const.RANGE_HEADER, "0-1023")
                        .withHeader(Const.OCI_CHUNK_MIN_LENGTH_HEADER, "4096")));

        // Create sample data with headers
        byte[] data = "test patch".getBytes();
        Map<String, String> headers = new HashMap<>();
        headers.put(Const.CONTENT_TYPE_HEADER, Const.APPLICATION_OCTET_STREAM_HEADER_VALUE);
        headers.put(Const.CONTENT_RANGE_HEADER, "0-1023");

        // Execute Patch
        URI uri = URI.create("http://" + registryUrl + "/v2/test/blobs/uploads/session1");
        HttpClient.ResponseWrapper<String> response = client.patch(
                uri,
                data,
                headers,
                Scopes.of(Registry.builder().build(), ContainerRef.parse("foo")),
                new NoAuthProvider());

        // Verify response uses all our constants
        assertEquals(202, response.statusCode());
        assertEquals("/v2/test/blobs/uploads/session2", response.headers().get(Const.LOCATION_HEADER.toLowerCase()));
        assertEquals("0-1023", response.headers().get(Const.RANGE_HEADER.toLowerCase()));
        assertEquals("4096", response.headers().get(Const.OCI_CHUNK_MIN_LENGTH_HEADER.toLowerCase()));

        // Verify the PATCH request was made with correct headers
        wireMock.verifyThat(patchRequestedFor(urlEqualTo("/v2/test/blobs/uploads/session1"))
                .withHeader(Const.CONTENT_TYPE_HEADER, equalTo(Const.APPLICATION_OCTET_STREAM_HEADER_VALUE))
                .withHeader(Const.CONTENT_RANGE_HEADER, equalTo("0-1023")));
    }

    @Test
    void shouldHandleRateLimitingResponse(WireMockRuntimeInfo wmRuntimeInfo) {
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        String registryUrl = wmRuntimeInfo.getHttpBaseUrl().replace("http://", "");

        // Setup WireMock to return 429 Too Many Requests
        wireMock.register(get(urlEqualTo("/v2/library/rate-limited/tags/list"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Retry-After", "5")
                        .withBody("Rate limit exceeded")));

        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .build();

        ContainerRef ref = ContainerRef.parse("%s/library/rate-limited".formatted(registryUrl));

        // Verify that a 429 status code is thrown as an OrasException
        OrasException exception = assertThrows(OrasException.class, () -> registry.getTags(ref));
        assertEquals(429, exception.getStatusCode());
        assertEquals("Response code: 429", exception.getMessage());
    }

    @Test
    void shouldFollowRedirectOnBlobFetch(WireMockRuntimeInfo wmRuntimeInfo) {
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        String registryUrl = wmRuntimeInfo.getHttpBaseUrl().replace("http://", "");
        String digest = SupportedAlgorithm.SHA256.digest("blob-data".getBytes());

        // Setup WireMock to redirect to a new location
        String redirectUrl = "http://%s/v2/library/redirect-blob/blobs/redirected/%s".formatted(registryUrl, digest);
        wireMock.register(get(urlEqualTo("/v2/library/redirect-blob/blobs/%s".formatted(digest)))
                .willReturn(aResponse().withStatus(307).withHeader("Location", redirectUrl)));

        // Setup WireMock to serve blob at redirected location
        wireMock.register(get(urlEqualTo("/v2/library/redirect-blob/blobs/redirected/%s".formatted(digest)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(Const.DOCKER_CONTENT_DIGEST_HEADER, digest)
                        .withBody("blob-data")));

        // Setup HEAD request for validation
        wireMock.register(head(urlEqualTo("/v2/library/redirect-blob/blobs/%s".formatted(digest)))
                .willReturn(aResponse().withStatus(200)));

        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .build();

        ContainerRef containerRef = ContainerRef.parse("%s/library/redirect-blob".formatted(registryUrl));
        byte[] blob = registry.getBlob(containerRef.withDigest(digest));
        assertEquals("blob-data", new String(blob));
    }

    @Test
    void shouldComputeSizeWhenGettingDescriptorIfNull(WireMockRuntimeInfo wmRuntimeInfo) {

        WireMock wireMock = wmRuntimeInfo.getWireMock();
        String registryUrl = wmRuntimeInfo.getHttpBaseUrl().replace("http://", "");
        wireMock.register(any(urlEqualTo("/v2/library/null-size/manifests/latest"))
                .willReturn(
                        aResponse().withStatus(200).withBody("{}") // Empty JSON, no test on index
                        ));

        // Test
        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .build();
        ContainerRef containerRef = ContainerRef.parse("%s/library/null-size".formatted(registryUrl));

        Descriptor descriptor = registry.getDescriptor(containerRef);
        assertNotNull(descriptor, "Descriptor should not be null");
        assertEquals(2, descriptor.getSize(), "Size should be 0 when not provided by registry");
    }

    @Test
    void shouldGetSizeFromHeaderWhenGettingDescriptor(WireMockRuntimeInfo wmRuntimeInfo) {

        WireMock wireMock = wmRuntimeInfo.getWireMock();
        String registryUrl = wmRuntimeInfo.getHttpBaseUrl().replace("http://", "");
        wireMock.register(any(urlEqualTo("/v2/library/header-size-size/manifests/latest"))
                .willReturn(
                        aResponse()
                                .withStatus(200)
                                .withHeader("Content-Length", "42")
                                .withBody("{}") // Empty JSON, no test on index
                        ));

        // Test
        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .build();
        ContainerRef containerRef = ContainerRef.parse("%s/library/header-size-size".formatted(registryUrl));

        Descriptor descriptor = registry.getDescriptor(containerRef);
        assertNotNull(descriptor, "Descriptor should not be null");
        assertEquals(42, descriptor.getSize(), "Size should be 0 when not provided by registry");
    }

    @Test
    void shouldValidateDockerContentDigestForUnknownAlgorithm(WireMockRuntimeInfo wmRuntimeInfo) {
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        String registryUrl = wmRuntimeInfo.getHttpBaseUrl().replace("http://", "");
        String digest = SupportedAlgorithm.SHA256.digest("blob-data".getBytes());
        wireMock.register(any(urlEqualTo("/v2/library/validate-digest/blobs/%s".formatted(digest)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("blob-data")
                        .withHeader("Docker-Content-Digest", "fake:12345")));

        // Test
        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .build();
        ContainerRef containerRef = ContainerRef.parse("%s/library/validate-digest".formatted(registryUrl));
        OrasException e = assertThrows(
                OrasException.class,
                () -> registry.getBlob(containerRef.withDigest(digest)),
                "Expected OrasException to be thrown");
        assertEquals("Unsupported digest: fake:12345", e.getMessage());
    }

    @Test
    void shouldValidateDockerContentDigestMismatch(WireMockRuntimeInfo wmRuntimeInfo) {
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        String registryUrl = wmRuntimeInfo.getHttpBaseUrl().replace("http://", "");
        String digest = SupportedAlgorithm.SHA256.digest("blob-data".getBytes());
        wireMock.register(any(urlEqualTo("/v2/library/validate-digest/blobs/%s".formatted(digest)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("blob-data")
                        .withHeader("Docker-Content-Digest", "sha256:12345")));

        // Test
        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .build();
        ContainerRef containerRef = ContainerRef.parse("%s/library/validate-digest".formatted(registryUrl));
        OrasException e = assertThrows(
                OrasException.class,
                () -> registry.getBlob(containerRef.withDigest(digest)),
                "Expected OrasException to be thrown");
        assertEquals(
                "Digest mismatch: sha256:12345 != sha256:c2752ad96ee652e4d37fd3852de632c50f193490d132f27a1794c986e1f112ef",
                e.getMessage());
    }

    @Test
    void shouldNotValidateDockerContentDigestWhenProbingDescriptor(WireMockRuntimeInfo wmRuntimeInfo) {
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        String registryUrl = wmRuntimeInfo.getHttpBaseUrl().replace("http://", "");
        wireMock.register(head(urlEqualTo("/v2/library/validate-digest/manifests/latest"))
                .willReturn(aResponse().withStatus(200).withBody("blob-data")));

        // Test
        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .build();
        ContainerRef containerRef = ContainerRef.parse("%s/library/validate-digest".formatted(registryUrl));
        Descriptor descriptor = registry.probeDescriptor(containerRef);
        assertNotNull(descriptor, "Descriptor should not be null");
    }

    @Test
    void shouldFollowRedirectAfterRequestingToken(WireMockRuntimeInfo wmRuntimeInfo) {

        String digest = SupportedAlgorithm.SHA256.digest("blob-data".getBytes());

        String registryUrl = wmRuntimeInfo.getHttpBaseUrl().replace("http://", "");

        // Redirect to a fake other storage
        String redirectUrl = "http://%s/storage/%s".formatted(registryUrl, digest);

        WireMock wireMock = wmRuntimeInfo.getWireMock();

        // First we need to authenticate
        wireMock.register(WireMock.any(WireMock.urlEqualTo("/v2/library/get-first-token/blobs/%s".formatted(digest)))
                .inScenario("redirect after token")
                .willSetStateTo("auth requested")
                .willReturn(WireMock.unauthorized()
                        .withHeader(
                                Const.WWW_AUTHENTICATE_HEADER,
                                "Bearer realm=\"http://localhost:%d/token\",service=\"localhost\",scope=\"repository:library/get-first-token:pull\""
                                        .formatted(wmRuntimeInfo.getHttpPort()))));

        // Token is returned
        wireMock.register(WireMock.any(
                        WireMock.urlEqualTo("/token?scope=repository:library/get-first-token:pull&service=localhost"))
                .inScenario("redirect after token")
                .whenScenarioStateIs("auth requested")
                .willSetStateTo("got token")
                .willReturn(WireMock.okJson(JsonUtils.toJson(
                        new HttpClient.TokenResponse("fake-token", "access-token", 300, ZonedDateTime.now())))));

        // After getting token we get a redirect
        wireMock.register(WireMock.any(WireMock.urlEqualTo("/v2/library/get-first-token/blobs/%s".formatted(digest)))
                .inScenario("redirect after token")
                .whenScenarioStateIs("got token")
                .willSetStateTo("redirect")
                .willReturn(WireMock.temporaryRedirect(redirectUrl)));

        // We finally get the blob
        wireMock.register(WireMock.any(WireMock.urlEqualTo("/storage/%s".formatted(digest)))
                .inScenario("redirect after token")
                .whenScenarioStateIs("redirect")
                .willSetStateTo("done")
                .willReturn(WireMock.ok("blob-data").withHeader(Const.DOCKER_CONTENT_DIGEST_HEADER, digest)));

        // Test
        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .build();
        ContainerRef containerRef =
                ContainerRef.parse("localhost:%d/library/get-first-token".formatted(wmRuntimeInfo.getHttpPort()));
        byte[] blob = registry.getBlob(containerRef.withDigest(digest));
        assertEquals("blob-data", new String(blob));
    }

    @Test
    void shouldHandleConcurrentBlobPushes(WireMockRuntimeInfo wmRuntimeInfo) throws IOException, InterruptedException {
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        String registryUrl = wmRuntimeInfo.getHttpBaseUrl().replace("http://", "");
        String digest = SupportedAlgorithm.SHA256.digest("concurrent-data".getBytes());

        // Setup WireMock for blob push
        wireMock.register(head(urlPathMatching("/v2/library/concurrent-blob/blobs/.*"))
                .willReturn(aResponse().withStatus(404)));
        wireMock.register(post(urlPathMatching("/v2/library/concurrent-blob/blobs/uploads/.*"))
                .willReturn(aResponse().withStatus(202).withHeader("Location", "/upload")));
        wireMock.register(
                put(urlPathMatching("/upload.*")).willReturn(aResponse().withStatus(201)));

        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .build();

        ContainerRef containerRef = ContainerRef.parse("%s/library/concurrent-blob@%s".formatted(registryUrl, digest));

        // Create a temporary file for pushing
        Path testFile = configDir.resolve("concurrent-data.temp");
        Files.writeString(testFile, "concurrent-data");

        // Execute concurrent blob pushes
        ExecutorService executor = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                try {
                    registry.pushBlob(containerRef, testFile);
                } catch (OrasException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        executor.shutdown();
        boolean completed = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertEquals(true, completed, "Concurrent blob pushes did not complete within timeout");
    }

    @Test
    void shouldHandleNetworkConnectivityLoss(WireMockRuntimeInfo wmRuntimeInfo) {
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        String registryUrl = wmRuntimeInfo.getHttpBaseUrl().replace("http://", "");

        // Setup WireMock to simulate a connection reset
        wireMock.register(get(urlEqualTo("/v2/library/network-loss/tags/list"))
                .willReturn(aResponse().withStatus(503).withBody("Service Unavailable")));

        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .build();

        ContainerRef ref = ContainerRef.parse("%s/library/network-loss".formatted(registryUrl));

        // Verify that a network connectivity loss results in an OrasException
        OrasException exception = assertThrows(OrasException.class, () -> registry.getTags(ref));
        assertEquals("Response code: 503", exception.getMessage());
    }

    @Test
    void shouldHandleCorruptedResponse(WireMockRuntimeInfo wmRuntimeInfo) {
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        String registryUrl = wmRuntimeInfo.getHttpBaseUrl().replace("http://", "");
        String digest = SupportedAlgorithm.SHA256.digest("blob-data".getBytes());

        // Setup WireMock to return a corrupted blob response
        wireMock.register(head(urlEqualTo("/v2/library/corrupted-blob/blobs/%s".formatted(digest)))
                .willReturn(aResponse().withStatus(200)));
        wireMock.register(get(urlEqualTo("/v2/library/corrupted-blob/blobs/%s".formatted(digest)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(Const.DOCKER_CONTENT_DIGEST_HEADER, digest)
                        .withBody("corrupted-data")));

        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .build();

        ContainerRef containerRef = ContainerRef.parse("%s/library/corrupted-blob".formatted(registryUrl));

        // Expect digest mismatch exception
        OrasException exception =
                assertThrows(OrasException.class, () -> registry.getBlob(containerRef.withDigest(digest)));
        assertEquals(
                "Digest mismatch: sha256:c2752ad96ee652e4d37fd3852de632c50f193490d132f27a1794c986e1f112ef != sha256:2be4e14a6587ab9b637afb553f0654c70e80fa14bd0b8fbf9fa09079f55a2ace",
                exception.getMessage());
    }

    private byte[] tokenScenario(
            WireMockRuntimeInfo wmRuntimeInfo, String registryName, String token, String accessToken) {
        String digest = SupportedAlgorithm.SHA256.digest("blob-data".getBytes());

        // Return data from wiremock
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.any(WireMock.urlEqualTo("/v2/library/%s/blobs/%s".formatted(registryName, digest)))
                .inScenario(registryName)
                .willReturn(WireMock.unauthorized()
                        .withHeader(
                                Const.WWW_AUTHENTICATE_HEADER,
                                "Bearer realm=\"http://localhost:%d/token\",service=\"localhost\",scope=\"repository:library/%s:pull\""
                                        .formatted(wmRuntimeInfo.getHttpPort(), registryName))));

        // Return token
        wireMock.register(WireMock.any(WireMock.urlEqualTo(
                        "/token?scope=repository:library/%s:pull&service=localhost".formatted(registryName)))
                .inScenario(registryName)
                .willSetStateTo("get")
                .willReturn(WireMock.okJson(
                        JsonUtils.toJson(new HttpClient.TokenResponse(token, accessToken, 300, ZonedDateTime.now())))));

        // On the second call we return ok
        wireMock.register(WireMock.any(WireMock.urlEqualTo("/v2/library/%s/blobs/%s".formatted(registryName, digest)))
                .inScenario(registryName)
                .whenScenarioStateIs("get")
                .willReturn(
                        WireMock.ok().withBody("blob-data").withHeader(Const.DOCKER_CONTENT_DIGEST_HEADER, digest)));

        // Insecure registry
        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .build();

        ContainerRef containerRef =
                ContainerRef.parse("localhost:%d/library/%s".formatted(wmRuntimeInfo.getHttpPort(), registryName));
        return registry.getBlob(containerRef.withDigest(digest));
    }
}
