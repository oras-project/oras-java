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

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class AnnotationsTest {

    @Test
    public void fromJson() {
        Annotations annotations = Annotations.fromJson(sampleAnnotations());
        assertEquals(1, annotations.configAnnotations().size());
        assertEquals(1, annotations.manifestAnnotations().size());
        assertEquals(1, annotations.filesAnnotations().size());
        assertEquals("world", annotations.configAnnotations().get("hello"));
        assertEquals("bar", annotations.manifestAnnotations().get("foo"));
        assertEquals(
                "more cream", annotations.filesAnnotations().get("cake.txt").get("fun"));
        assertEquals("more cream", annotations.getFileAnnotations("cake.txt").get("fun"));
    }

    @Test
    public void nullAnnotations() {
        Annotations annotations = Annotations.ofManifest(null);
        assertEquals(0, annotations.configAnnotations().size());
        assertEquals(0, annotations.manifestAnnotations().size());
        assertEquals(0, annotations.filesAnnotations().size());
    }

    @Test
    public void toJson() {
        Annotations annotations = new Annotations(
                Map.of("hello", "world"), Map.of("foo", "bar"), Map.of("cake.txt", Map.of("fun", "more cream")));
        assertEquals(1, annotations.configAnnotations().size());
        assertEquals(1, annotations.manifestAnnotations().size());
        assertEquals(1, annotations.filesAnnotations().size());
        assertEquals(Annotations.fromJson(sampleAnnotations()).toJson(), annotations.toJson());

        // Manifest annotations only
        annotations = Annotations.ofManifest(Map.of("foo", "bar"));
        assertEquals(0, annotations.configAnnotations().size());
        assertEquals(1, annotations.manifestAnnotations().size());
        assertEquals(0, annotations.filesAnnotations().size());

        // Config annotations only
        annotations = Annotations.ofConfig(Map.of("hello", "world"));
        assertEquals(1, annotations.configAnnotations().size());
        assertEquals(0, annotations.manifestAnnotations().size());
        assertEquals(0, annotations.filesAnnotations().size());
    }

    @Test
    void testEquals() {
        Annotations annotations1 = Annotations.fromJson(sampleAnnotations());
        Annotations annotations2 = Annotations.fromJson(sampleAnnotations());
        assertEquals(annotations1, annotations2);
        assertEquals(annotations1.hashCode(), annotations2.hashCode());
    }

    @Test
    void testToString() {
        Annotations annotations = Annotations.fromJson(sampleAnnotations());
        String expected =
                "{\"cake.txt\":{\"fun\":\"more cream\"},\"$config\":{\"hello\":\"world\"},\"$manifest\":{\"foo\":\"bar\"}}";
        assertEquals(expected, annotations.toString());
    }

    private String sampleAnnotations() {
        return """
                {
                    "$config": {
                      "hello": "world"
                    },
                    "$manifest": {
                      "foo": "bar"
                    },
                    "cake.txt": {
                      "fun": "more cream"
                    }
                  }
              """;
    }
}
