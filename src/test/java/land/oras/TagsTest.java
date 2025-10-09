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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import land.oras.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class TagsTest {

    @Test
    void shouldReadAndWriteTags() {
        String json = "{\"tags\":[\"tag1\",\"tag2\"]}";
        Tags tags = JsonUtils.fromJson(json, Tags.class);
        assertNotNull(tags, "Deserialized object should not be null");
        assertEquals(2, tags.tags().size(), "Should contain 2 tags");
        assertEquals(json, JsonUtils.toJson(tags), "Serialized JSON should match original");
    }
}
