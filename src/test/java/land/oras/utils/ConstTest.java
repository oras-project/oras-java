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

package land.oras.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class ConstTest {

    @Test
    void shouldValidateAnnotations() {
        assertEquals("org.opencontainers.image.title", Const.ANNOTATION_TITLE);
        assertEquals("org.opencontainers.image.source", Const.ANNOTATION_SOURCE);
        assertEquals("org.opencontainers.image.revision", Const.ANNOTATION_REVISION);
        assertEquals("org.opencontainers.image.created", Const.ANNOTATION_CREATED);
        assertEquals("org.opencontainers.image.ref.name", Const.ANNOTATION_REF);
        assertEquals("application/vnd.oci.image.config.v1+json", Const.CONFIG_RUNNING_CONTAINER_MEDIA_TYPE);
        assertEquals("application/vnd.docker.container.image.v1+json", Const.CONFIG_RUNNING_DOCKER_MEDIA_TYPE);
        assertEquals("dev.sigstore.bundle.content", Const.ANNOTATION_SIGSTORE_BUNDLE_CONTENT);
        assertEquals("dsse-envelope", Const.SIGSTORE_BUNDLE_CONTENT_DSSE);
        assertEquals("dev.sigstore.bundle.predicateType", Const.ANNOTATION_SIGSTORE_BUNDLE_PREDICATE_TYPE);
    }
}
