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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import land.oras.exception.OrasException;
import land.oras.utils.Const;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class ArtifactTypeTest {

    @Test
    void validateArtifactType() {
        ArtifactType unknown = ArtifactType.unknown();
        assertEquals(Const.DEFAULT_ARTIFACT_MEDIA_TYPE, unknown.getMediaType());
        assertThrows(OrasException.class, () -> ArtifactType.from("invalid"));
        assertEquals("foo/bar", ArtifactType.from("foo/bar").getMediaType());
    }

    @Test
    void validateUnknown() {
        ArtifactType unknown = ArtifactType.from(null);
        assertEquals(Const.DEFAULT_ARTIFACT_MEDIA_TYPE, unknown.getMediaType());
        assertEquals(Const.DEFAULT_ARTIFACT_MEDIA_TYPE, unknown.toString());
    }
}
