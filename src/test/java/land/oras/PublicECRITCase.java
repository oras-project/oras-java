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
class PublicECRITCase {

    @Test
    void shouldPullAnonymousIndex() {

        // Via tag
        Registry registry = Registry.builder().build();
        ContainerRef containerRef1 = ContainerRef.parse("public.ecr.aws/docker/library/alpine:latest");
        Index index = registry.getIndex(containerRef1);
        assertNotNull(index);

        // Via digest
        ContainerRef containerRef2 = ContainerRef.parse(
                "public.ecr.aws/docker/library/alpine@sha256:25109184c71bdad752c8312a8623239686a9a2071e8825f20acb8f2198c3f659");
        Index index2 = registry.getIndex(containerRef2);
        assertNotNull(index2);
    }
}
