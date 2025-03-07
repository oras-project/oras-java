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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.nio.file.Path;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@WireMockTest
@Execution(ExecutionMode.SAME_THREAD)
public class OCILayoutWireMockTest {

    @TempDir
    private Path layoutPath;

    // Timeout with similar structure as previous test and request 408 with different artifact name
    @Test
    void copyToOciLayoutMissingInvalidContentType(WireMockRuntimeInfo wmRuntimeInfo) {

        WireMock wireMock = wmRuntimeInfo.getWireMock();
        String registryUrl = wmRuntimeInfo.getHttpBaseUrl().replace("http://", "");

        // Using here a unique container reference to avoid conflicts when running in parallel
        ContainerRef ref = ContainerRef.parse("%s/library/invalid-copy-artifact".formatted(registryUrl));

        wireMock.register(WireMock.head(WireMock.urlEqualTo("/v2/library/invalid-copy-artifact/manifests/latest"))
                .willReturn(WireMock.noContent()));

        // No content type
        Registry registry = Registry.Builder.builder().withInsecure(true).build();
        OCILayout ociLayout = OCILayout.Builder.builder().defaults(layoutPath).build();

        OrasException exception = assertThrows(OrasException.class, () -> ociLayout.copy(registry, ref));
        assertEquals("Content type not found in headers", exception.getMessage());

        // No manifest digest
        wireMock.register(WireMock.head(WireMock.urlEqualTo("/v2/library/invalid-copy-artifact/manifests/latest"))
                .willReturn(
                        WireMock.noContent().withHeader(Const.CONTENT_TYPE_HEADER, Const.DEFAULT_MANIFEST_MEDIA_TYPE)));
        exception = assertThrows(OrasException.class, () -> ociLayout.copy(registry, ref));
        assertEquals("Manifest digest not found in headers", exception.getMessage());

        // Invalid content type
        wireMock.register(WireMock.head(WireMock.urlEqualTo("/v2/library/invalid-copy-artifact/manifests/latest"))
                .willReturn(WireMock.noContent()
                        .withHeader(Const.CONTENT_TYPE_HEADER, "application/json")
                        .withHeader(Const.DOCKER_CONTENT_DIGEST_HEADER, "sha256:1234")));
        exception = assertThrows(OrasException.class, () -> ociLayout.copy(registry, ref));
        assertEquals("Unsupported content type: application/json", exception.getMessage());
    }
}
