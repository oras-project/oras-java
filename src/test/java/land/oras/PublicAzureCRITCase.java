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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class PublicAzureCRITCase {

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
}
