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

package land.oras;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import land.oras.utils.Const;
import land.oras.utils.ZotUnsecureContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Execution(ExecutionMode.CONCURRENT)
class FluxCDITCase {

    @TempDir
    Path tempDir;

    @Container
    private final ZotUnsecureContainer unsecureRegistry = new ZotUnsecureContainer().withStartupAttempts(3);

    /**
     * This test demonstrate how to assemble a Flux CD OCI Artifact
     */
    @Test
    void shouldAssembleArtifact() {

        // The compressed manifests
        Path archive = Paths.get("src/test/resources/archives").resolve("flux-manifests.tgz");
        String configMediaType = "application/vnd.cncf.flux.config.v1+json";
        String contentMediaType = "application/vnd.cncf.flux.content.v1.tar+gzip";

        Map<String, String> annotations = Map.of(
                Const.ANNOTATION_REVISION, "@sha1:6d63912ed9a9443dd01fbfd2991173a246050079",
                Const.ANNOTATION_SOURCE, "git@github.com:jonesbusy/oras-java.git",
                Const.ANNOTATION_CREATED, Const.currentTimestamp());

        // Create objects
        Config config = Config.empty().withMediaType(configMediaType);
        Layer layer = Layer.fromFile(archive).withMediaType(contentMediaType);
        Manifest manifest =
                Manifest.empty().withConfig(config).withLayers(List.of(layer)).withAnnotations(annotations);

        // Push config, layers and manifest to registry
        Registry registry = Registry.builder()
                .insecure()
                .withRegistry(unsecureRegistry.getRegistry())
                .build();
        ContainerRef containerRef = ContainerRef.parse("manifests:latest");

        registry.pushConfig(containerRef, config);
        registry.pushBlob(containerRef, archive);
        registry.pushManifest(containerRef, manifest);

        // Ensure we can pull
        Manifest createdManifest = registry.getManifest(containerRef);
        assertNotNull(createdManifest);

        // We can test pull with flux pull artifact oci://localhost:<port>/manifests:latest --output .

    }
}
