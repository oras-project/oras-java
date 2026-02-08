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

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD) // Avoid 429 Too Many Requests for unauthenticated requests to public ECR
class PublicECRITCase {

    @Test
    void shouldPullAnonymousIndexAndFilterPlatform() {

        // Via tag
        Registry registry = Registry.builder().build();
        ContainerRef containerRef = ContainerRef.parse("public.ecr.aws/docker/library/alpine:latest");
        Index index = registry.getIndex(containerRef);
        assertNotNull(index);

        // Find a specific platform
        List<ManifestDescriptor> manifests = index.filter(Platform.linuxAmd64());
        assertNotNull(manifests);
        assertEquals(1, manifests.size());
        assertNotNull(manifests.get(0));

        // Find unique
        ManifestDescriptor manifest = index.findUnique(Platform.linuxAmd64());
        assertNotNull(manifest);

        // Find unknown platforms
        List<ManifestDescriptor> unknownManifests = index.filter(Platform.unknown());
        assertNotNull(unknownManifests);
        assertTrue(
                unknownManifests.size() > 1,
                "Expected more than 1 manifest with unknown platform, but got " + unknownManifests.size());
    }

    @Test
    void shouldPullAnonymousIndexViaDigest() {

        Registry registry = Registry.builder().build();

        // Via digest
        ContainerRef containerRef2 = ContainerRef.parse(
                "public.ecr.aws/docker/library/alpine@sha256:25109184c71bdad752c8312a8623239686a9a2071e8825f20acb8f2198c3f659");
        Index index2 = registry.getIndex(containerRef2);
        assertNotNull(index2);
    }

    @Test
    void shouldPullManifest() {

        // Via tag
        Registry registry1 = Registry.builder().build();
        ContainerRef containerRef1 = ContainerRef.parse("public.ecr.aws/bitnami/contour-operator:latest");
        Manifest manifest1 = registry1.getManifest(containerRef1);
        assertNotNull(manifest1);

        // Via digest
        Registry registry2 = Registry.builder().build();
        ContainerRef containerRef2 = ContainerRef.parse(
                "public.ecr.aws/docker/library/alpine@sha256:59855d3dceb3ae53991193bd03301e082b2a7faa56a514b03527ae0ec2ce3a95");
        Manifest manifest2 = registry2.getManifest(containerRef2);
        assertNotNull(manifest2);
    }

    @Test
    void shouldPullLayer() {
        Registry registry = Registry.builder().build();
        ContainerRef containerRef = ContainerRef.parse(
                "public.ecr.aws/docker/library/alpine@sha256:589002ba0eaed121a1dbf42f6648f29e5be55d5c8a6ee0f8eaa0285cc21ac153");
        registry.getBlob(containerRef);
    }
}
