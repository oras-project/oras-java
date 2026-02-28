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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class PublicAzureCRITCase {

    @TempDir
    private Path tempDir;

    @Test
    void shouldContainsCommonWindowsPlatform() {

        // Source registry
        Registry sourceRegistry = Registry.Builder.builder().defaults().build();

        ContainerRef containerSource = ContainerRef.parse(
                "mcr.microsoft.com/windows@sha256:755e998e6f63e40f709deb731eee9b1d219673bfb21149cccf29aba5dfd32e0f");

        Index index = sourceRegistry.getIndex(containerSource);

        assertTrue(index.getManifests().stream()
                .anyMatch(m -> m.getPlatform().equals(Platform.windowsAmd64().withOsVersion("10.0.17763.8389"))));

        containerSource = ContainerRef.parse(
                "mcr.microsoft.com/windows/servercore@sha256:79aa6a176b2e4f1786eb29c4facd33077769eddde4c4a650aea552f6320893c7");
        index = sourceRegistry.getIndex(containerSource);
        assertTrue(index.getManifests().stream().anyMatch(m -> m.getPlatform()
                .equals(Platform.windowsAmd64()
                        .withOsVersion("10.0.26100.32370")
                        .withOsFeatures(List.of("win32k")))));
    }

    @Test
    void shouldPullAnonymousIndex() {

        // Via tag
        Registry registry = Registry.builder().build();
        ContainerRef containerRef1 = ContainerRef.parse("mcr.microsoft.com/azurelinux/busybox:1.36");
        Index index = registry.getIndex(containerRef1);
        assertNotNull(index);

        // Via digest
        ContainerRef containerRef2 = ContainerRef.parse(
                "mcr.microsoft.com/azurelinux/busybox@sha256:330ce7e940f7476089c4c74565c63767988e188b06988e58e76b54890e7f621b");
        Index index2 = registry.getIndex(containerRef2);
        assertNotNull(index2);
    }

    @Test
    void shouldPullAnonymousManifest() {

        // Via tag
        Registry registry = Registry.builder().build();

        // Via digest
        ContainerRef containerRef1 = ContainerRef.parse(
                "mcr.microsoft.com/azurelinux/busybox@sha256:ec5adfc87f57633da1feedb2361c06374020ab9c99d4a14b19319e57284b6656");
        Manifest manifest1 = registry.getManifest(containerRef1);
        assertNotNull(manifest1);
    }

    @Test
    void shouldPullOneBlob() throws IOException {
        Registry registry = Registry.builder().build();
        ContainerRef containerRef1 = ContainerRef.parse(
                "mcr.microsoft.com/azurelinux/busybox@sha256:ec5adfc87f57633da1feedb2361c06374020ab9c99d4a14b19319e57284b6656");
        Manifest manifest = registry.getManifest(containerRef1);
        Layer oneLayer = manifest.getLayers().get(0);
        registry.fetchBlob(containerRef1.withDigest(oneLayer.getDigest()), tempDir.resolve("my-blob"));
        assertNotNull(tempDir.resolve("my-blob"));
    }
}
