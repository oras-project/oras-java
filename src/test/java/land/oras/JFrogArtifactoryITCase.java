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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JFrogArtifactoryITCase {

    @TempDir
    Path tempDir;

    @Test
    void shouldPull() {
        Registry registry = Registry.builder().build();
        ContainerRef containerRef1 = ContainerRef.parse("releases-docker.jfrog.io/jfrog/jfrog-cli-v2-jf");
        Manifest manifest = registry.getManifest(containerRef1);
        assertNotNull(manifest);
    }

    @Test
    void shouldPullOneBlob() throws IOException {
        Registry registry = Registry.builder().build();
        ContainerRef containerRef1 = ContainerRef.parse("releases-docker.jfrog.io/jfrog/jfrog-cli-v2-jf");
        Manifest manifest = registry.getManifest(containerRef1);
        Layer oneLayer = manifest.getLayers().get(0);
        registry.fetchBlob(containerRef1.withDigest(oneLayer.getDigest()), tempDir.resolve("my-blob"));
        assertNotNull(tempDir.resolve("my-blob"));
    }
}
