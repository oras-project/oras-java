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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import land.oras.auth.FileStoreAuthenticationProvider;
import land.oras.auth.UsernamePasswordProvider;
import land.oras.credentials.FileStore;
import land.oras.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WireMockTest
@Execution(ExecutionMode.SAME_THREAD)
public class RegistryWireMockTest {

    private static final Logger LOG = LoggerFactory.getLogger(RegistryWireMockTest.class);

    private final UsernamePasswordProvider authProvider = new UsernamePasswordProvider("myuser", "mypass");

    @TempDir
    private Path configDir;

    @Test
    void shouldRedirectWhenDownloadingBlob(WireMockRuntimeInfo wmRuntimeInfo) {

        // Return data from wiremock
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.any(WireMock.urlEqualTo("/v2/library/artifact-text/blobs/sha256:one"))
                .willReturn(WireMock.temporaryRedirect("http://localhost:%d/v2/library/artifact-text/blobs/sha256:other"
                        .formatted(wmRuntimeInfo.getHttpPort()))));

        // Return blob on new location
        wireMock.register(WireMock.head(WireMock.urlEqualTo("/v2/library/artifact-text/blobs/sha256:other"))
                .willReturn(WireMock.ok()));
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/v2/library/artifact-text/blobs/sha256:other"))
                .willReturn(WireMock.ok().withBody("blob-data")));

        // Insecure registry
        Registry registry = Registry.Builder.builder()
                .withAuthProvider(authProvider)
                .withInsecure(true)
                .build();

        ContainerRef containerRef =
                ContainerRef.parse("localhost:%d/library/artifact-text".formatted(wmRuntimeInfo.getHttpPort()));
        byte[] blob = registry.getBlob(containerRef.withDigest("sha256:one"));
        assertEquals("blob-data", new String(blob));
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
                .formatted(wmRuntimeInfo.getHttpBaseUrl().replace("http://", ""))));

        // Assert
        assertEquals(2, tags.size());
        assertEquals("latest", tags.get(0));
        assertEquals("0.1.1", tags.get(1));
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

        ContainerRef containerRef = ContainerRef.forRegistry("localhost:%d".formatted(wmRuntimeInfo.getHttpPort()));
        FileStoreAuthenticationProvider authProvider =
                new FileStoreAuthenticationProvider(FileStore.newFileStore(List.of(configDir.resolve("config.json"))));

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
                .formatted(wmRuntimeInfo.getHttpBaseUrl().replace("http://", ""))));

        // Assert
        assertEquals(2, tags.size());
        assertEquals("latest", tags.get(0));
        assertEquals("0.1.1", tags.get(1));
    }
}
