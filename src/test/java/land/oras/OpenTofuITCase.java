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
     * This test demonstrates how to assemble an OpenTofu provider OCI artifact.
     * See https://opentofu.org/docs/cli/oci_registries/provider-mirror/ for the specification.
     */
    @Test
    void shouldAssembleOpenTofuProviderArtifact() {

        // The provider zip archive for linux/amd64
        Path providerZip = Paths.get("src/test/resources/archives")
                .resolve("terraform-provider-aws_5.0.0_linux_amd64.zip");
        String configMediaType = "application/vnd.opentofu.provider";
        String contentMediaType = "application/zip";

        // Create objects for the linux/amd64 platform manifest
        Config config = Config.empty().withMediaType(configMediaType);
        Layer layer = Layer.fromFile(providerZip).withMediaType(contentMediaType);
        Manifest manifest = Manifest.empty().withConfig(config).withLayers(List.of(layer));

        // Push config, layer and manifest to registry
        Registry registry = Registry.builder()
                .insecure()
                .withRegistry(unsecureRegistry.getRegistry())
                .build();

        // The manifest is pushed without a version tag; the index will be tagged
        ContainerRef manifestRef = ContainerRef.parse("terraform-provider-aws:5.0.0-linux-amd64");
        registry.pushConfig(manifestRef, config);
        registry.pushBlob(manifestRef, providerZip);
        Manifest pushedManifest = registry.pushManifest(manifestRef, manifest);

        // Ensure the manifest was pushed successfully
        assertNotNull(pushedManifest);

        // Build the index with the manifest descriptor, annotated with the linux/amd64 platform
        ManifestDescriptor manifestDescriptor =
                pushedManifest.getDescriptor().withPlatform(Platform.linuxAmd64());
        Index index = Index.fromManifests(List.of(manifestDescriptor));

        // Push the index with the provider version tag
        ContainerRef indexRef = ContainerRef.parse("terraform-provider-aws:5.0.0");
        Index pushedIndex = registry.pushIndex(indexRef, index);

        // Ensure the index was pushed and contains the linux/amd64 manifest
        assertNotNull(pushedIndex);
        assertNotNull(pushedIndex.findUnique(Platform.linuxAmd64()));
    }
}
