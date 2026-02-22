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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import land.oras.utils.Const;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Test disabled due to authentication
 * mkdir lib && touch lib/a
 * oras push demo.goharbor.io/oras/lib:foo lib
 */
@Execution(ExecutionMode.CONCURRENT)
class HarborS3ITCase {

    @TempDir
    Path tempDir;

    /**
     * This test demonstrate how to assemble a Flux CD OCI Artifact
     */
    @Test
    void shouldPushHelmArtifact() {

        // The compressed manifests
        Path archive = Paths.get("src/test/resources/archives").resolve("jenkins-chart.tgz");
        String configMediaType = "application/vnd.cncf.helm.config.v1+json";
        String contentMediaType = "application/vnd.cncf.helm.chart.content.v1.tar+gzip";

        Map<String, String> annotations = Map.of(
                Const.ANNOTATION_DESCRIPTION, "Test helm chart",
                Const.ANNOTATION_SOURCE, "git@github.com:jonesbusy/oras-java.git",
                Const.ANNOTATION_CREATED, Const.currentTimestamp());

        // Create objects
        Config config = Config.empty().withMediaType(configMediaType);
        Layer layer = Layer.fromFile(archive).withMediaType(contentMediaType);
        Manifest manifest =
                Manifest.empty().withConfig(config).withLayers(List.of(layer)).withAnnotations(annotations);

        // Push config, layers and manifest to registry
        Registry registry = Registry.builder().defaults().build();
        ContainerRef containerRef = ContainerRef.parse("demo.goharbor.io/oras/chart:0.1.0");

        registry.pushConfig(containerRef, config);
        registry.pushBlob(containerRef, archive);
        registry.pushManifest(containerRef, manifest);

        // Ensure we can pull
        Manifest createdManifest = registry.getManifest(containerRef);
        assertNotNull(createdManifest);

        // We can test pull with helm pull oci://demo.goharbor.io/oras/chart --version 0.1.0

    }

    @Test
    @Disabled
    void shouldPushFluxArtifact() {

        // The compressed manifests
        Path archive = Paths.get("src/test/resources/archives").resolve("flux-manifests.tgz");
        Path image = Paths.get("src/test/resources/img").resolve("flux-cd.png");
        String configMediaType = "application/vnd.cncf.flux.config.v1+json";
        String contentMediaType = "application/vnd.cncf.flux.content.v1.tar+gzip";

        Map<String, String> annotations = Map.of(
                Const.ANNOTATION_REVISION, "@sha1:6d63912ed9a9443dd01fbfd2991173a246050079",
                Const.ANNOTATION_SOURCE, "git@github.com:jonesbusy/oras-java.git",
                Const.ANNOTATION_CREATED, Const.currentTimestamp());

        // Create objects
        Config config = Config.empty().withMediaType(configMediaType);
        Layer layer = Layer.fromFile(archive).withMediaType(contentMediaType);
        Layer imageLayer = Layer.fromFile(image)
                .withMediaType("image/png")
                .withAnnotations(Map.of("io.goharbor.artifact.v1alpha1.icon", ""));
        Manifest manifest = Manifest.empty()
                .withConfig(config)
                .withLayers(List.of(layer, imageLayer))
                .withAnnotations(annotations);

        // Push config, layers and manifest to registry
        Registry registry = Registry.builder().defaults().build();
        ContainerRef containerRef = ContainerRef.parse("demo.goharbor.io/oras/flux:latest");

        registry.pushConfig(containerRef, config);
        registry.pushBlob(containerRef, archive);
        registry.pushBlob(containerRef, image);
        registry.pushManifest(containerRef, manifest);

        // Ensure we can pull
        Manifest createdManifest = registry.getManifest(containerRef);
        assertNotNull(createdManifest);

        // We can test pull with flux pull artifact oci://demo.goharbor.io/oras/flux:latest --output .

    }

    @Test
    @Disabled
    void shouldPushJenkinsScriptArtifact() {

        // language=groovy
        String jenkinsfile =
                """
                node {
                    stage('Build') {
                        echo 'Building...'
                    }
                    stage('Test') {
                        echo 'Testing...'
                    }
                    stage('Deploy') {
                        echo 'Deploying...'
                }
                """;

        // The compressed manifests
        Path image = Paths.get("src/test/resources/img").resolve("jenkins.png");
        String contentMediaType = "text/x-groovy";

        Map<String, String> annotations = Map.of(
                Const.ANNOTATION_REVISION, "@sha1:6d63912ed9a9443dd01fbfd2991173a246050079",
                Const.ANNOTATION_SOURCE, "git@github.com:jonesbusy/oras-java.git",
                Const.ANNOTATION_CREATED, Const.currentTimestamp());

        // Create objects
        ContainerRef containerRef = ContainerRef.parse("demo.goharbor.io/oras/jenkins-cps:latest");
        Config config = Config.empty();
        Layer layer = Layer.fromData(containerRef, jenkinsfile.getBytes(StandardCharsets.UTF_8))
                .withMediaType(contentMediaType);
        Layer imageLayer = Layer.fromFile(image)
                .withMediaType("image/png")
                .withAnnotations(Map.of("io.goharbor.artifact.v1alpha1.icon", ""));
        Manifest manifest = Manifest.empty()
                .withConfig(config)
                .withArtifactType(ArtifactType.from("application/vnd.jenkins.pipeline.manifest.v1+json"))
                .withLayers(List.of(layer, imageLayer))
                .withAnnotations(annotations);

        // Push config, layers and manifest to registry
        Registry registry = Registry.builder().defaults().build();

        registry.pushConfig(containerRef, config);
        registry.pushBlob(containerRef, jenkinsfile.getBytes(StandardCharsets.UTF_8));
        registry.pushBlob(containerRef, image);
        registry.pushManifest(containerRef, manifest);

        // Ensure we can pull
        Manifest createdManifest = registry.getManifest(containerRef);
        assertNotNull(createdManifest);
    }

    @Test
    @Disabled("Only to test with demo Harbor demo instance")
    void shouldGetManifest() {
        Registry registry = Registry.builder().defaults().build();
        ContainerRef containerRef1 = ContainerRef.parse("demo.goharbor.io/oras/lib:foo");
        Manifest manifest = registry.getManifest(containerRef1);
        assertNotNull(manifest);
    }

    @Test
    @Disabled("Only to test with demo Harbor demo instance")
    void shouldPullOneBlob() {
        Registry registry = Registry.builder().defaults().build();
        ContainerRef containerRef1 = ContainerRef.parse("demo.goharbor.io/oras/lib:foo");
        Manifest manifest = registry.getManifest(containerRef1);
        Layer oneLayer = manifest.getLayers().get(0);
        registry.fetchBlob(containerRef1.withDigest(oneLayer.getDigest()), tempDir.resolve("my-blob"));
        assertNotNull(tempDir.resolve("my-blob"));
        registry.pullArtifact(containerRef1, tempDir, true);
    }
}
