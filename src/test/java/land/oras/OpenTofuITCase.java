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
import land.oras.utils.SupportedAlgorithm;
import land.oras.utils.ZotUnsecureContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Execution(ExecutionMode.CONCURRENT)
class OpenTofuITCase {

    @Container
    private final ZotUnsecureContainer unsecureRegistry = new ZotUnsecureContainer().withStartupAttempts(3);

    /**
     * This test demonstrate how to assemble a Flux CD OCI Artifact
     */
    @Test
    void shouldAssembleProviderArtifact() {

        // The compressed manifests
        Path archive =
                Paths.get("src/test/resources/archives").resolve("terraform-provider-random_3.8.1_linux_amd64.zip");

        Annotations annotations = Annotations.empty();
        ArtifactType indexArtifactType = ArtifactType.from("application/vnd.opentofu.provider");
        ArtifactType manifestArtifactType = ArtifactType.from("application/vnd.opentofu.provider-target");
        String contentMediaType = "archive/zip";

        Path image = Paths.get("src/test/resources/img").resolve("opentofu.png");

        Platform linuxAmd64 = Platform.linuxAmd64();

        // Create objects
        Config config = Config.empty();
        Layer layer = Layer.fromFile(archive).withMediaType(contentMediaType);
        Layer imageLayer = Layer.fromFile(image)
                .withMediaType("image/png")
                .withAnnotations(Map.of("io.goharbor.artifact.v1alpha1.icon", ""));
        Manifest manifest = Manifest.empty()
                .withArtifactType(manifestArtifactType)
                .withConfig(config)
                .withLayers(List.of(layer, imageLayer));

        // Index with given platform
        ManifestDescriptor manifestDescriptor =
                ManifestDescriptor.of(manifest, linuxAmd64, annotations, SupportedAlgorithm.SHA256);
        Index index = Index.fromManifests(List.of(manifestDescriptor)).withArtifactType(indexArtifactType);

        // Push config, layers and manifest to registry
        Registry registry = Registry.builder()
                .defaults()
                .insecure()
                .withRegistry(unsecureRegistry.getRegistry())
                .build();
        ContainerRef containerRef = ContainerRef.parse("oras/opentofu-providers/terraform-provider-random:3.8.1");

        registry.pushConfig(containerRef, config);
        registry.pushBlob(containerRef, archive);
        registry.pushBlob(containerRef, image);
        registry.pushManifest(containerRef.withDigest(manifestDescriptor.getDigest()), manifest);
        registry.pushIndex(containerRef, index);

        // Ensure we can pull
        Index createdIndex = registry.getIndex(containerRef);
        assertNotNull(createdIndex);
    }
}
