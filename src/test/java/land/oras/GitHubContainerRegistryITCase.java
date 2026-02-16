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
import land.oras.utils.ArchiveUtils;
import land.oras.utils.Const;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class GitHubContainerRegistryITCase {

    @TempDir
    Path tempDir;

    @TempDir
    private static Path homeDir;

    @Test
    void shouldPullIndex() {
        Registry registry = Registry.builder().build();
        ContainerRef containerRef1 = ContainerRef.parse("ghcr.io/oras-project/oras:main");
        Index index = registry.getIndex(containerRef1);
        assertNotNull(index);
    }

    @Test
    void shouldPullIndexWithAlias() throws Exception {
        // language=toml
        String config =
                """
                [aliases]
                "oras"="ghcr.io/oras-project/oras"
                """;

        // Setup
        TestUtils.createRegistriesConfFile(tempDir, config);

        TestUtils.withHome(homeDir, () -> {
            Registry registry = Registry.builder().defaults().build();
            ContainerRef containerRef1 = ContainerRef.parse("oras:main");
            Index index = registry.getIndex(containerRef1);
            assertNotNull(index);
        });
    }

    @Test
    void shouldPUllManifest() {
        Registry registry = Registry.builder().build();
        ContainerRef containerRef1 = ContainerRef.parse(
                "ghcr.io/oras-project/oras@sha256:fd4c818e80ea594cbd39ca47dc05067c8c5690c4eee6c8aee48c508290a5a0c0");
        Manifest manifest = registry.getManifest(containerRef1);
        assertNotNull(manifest);
    }

    @Test
    void shouldPullOneBlob() {
        Registry registry = Registry.builder().build();
        ContainerRef containerRef1 = ContainerRef.parse("ghcr.io/oras-project/oras:main");
        Index index = registry.getIndex(containerRef1);
        Manifest manifest = registry.getManifest(
                containerRef1.withDigest(index.getManifests().get(1).getDigest())); // Just take first manifest
        Layer oneLayer = manifest.getLayers().get(0);
        registry.fetchBlob(containerRef1.withDigest(oneLayer.getDigest()), tempDir.resolve("my-blob"));
        assertNotNull(tempDir.resolve("my-blob"));
    }

    @Test
    void shouldPullArtifact() {
        Registry registry = Registry.builder().build();
        ContainerRef artifact = ContainerRef.parse("ghcr.io/aquasecurity/trivy-db:2");
        registry.pullArtifact(artifact, tempDir, false);
        assertNotNull(tempDir.resolve("db.tar.gz"));
        ArchiveUtils.uncompressuntar(
                tempDir.resolve("db.tar.gz"), tempDir.resolve("db"), Const.DEFAULT_BLOB_DIR_MEDIA_TYPE);
    }
}
