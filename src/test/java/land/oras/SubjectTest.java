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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class SubjectTest {

    @Test
    void shouldReadSubject() {
        String json = sampleSubject();
        Subject subject = Subject.fromJson(json);

        // Assert subject
        assertEquals("application/vnd.oci.image.layer.v1.tar+gzip", subject.getMediaType());
        assertEquals(32654, subject.getSize());
        assertEquals("sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890", subject.getDigest());

        subject.toJson();
    }

    /**
     * A sample subject
     * @return The subject
     */
    private String sampleSubject() {
        // language=JSON
        return """
            {
              "mediaType": "application/vnd.oci.image.layer.v1.tar+gzip",
              "digest": "sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
              "size": 32654
            }
             """;
    }
}
