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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import land.oras.utils.Const;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Test disabled
 */
@Execution(ExecutionMode.CONCURRENT)
class NexusITCase {

    @Test
    @Disabled
    void shouldPushHelmArtifact() {

        // The compressed manifests
        Path archive = Paths.get("src/test/resources/archives").resolve("jenkins-chart.tgz");
        String configMediaType = "application/vnd.cncf.helm.config.v1+json";
        String contentMediaType = "application/vnd.cncf.helm.chart.content.v1.tar+gzip";

        Map<String, String> annotations = Map.of(
                Const.ANNOTATION_DESCRIPTION, "Test helm chart", Const.ANNOTATION_CREATED, Const.currentTimestamp());

        // Create objects
        Config config = Config.empty().withMediaType(configMediaType);
        Layer layer = Layer.fromFile(archive).withMediaType(contentMediaType);
        Manifest manifest =
                Manifest.empty().withConfig(config).withLayers(List.of(layer)).withAnnotations(annotations);

        // Push config, layers and manifest to registry
        Registry registry = Registry.builder().defaults().build();
        ContainerRef containerRef = ContainerRef.parse("localhost:8081/docker/helm:0.1.0");

        assertDoesNotThrow(() -> {
            registry.pushConfig(containerRef, config);
            registry.pushBlob(containerRef, archive);
            registry.pushManifest(containerRef, manifest);
        });
    }
}
