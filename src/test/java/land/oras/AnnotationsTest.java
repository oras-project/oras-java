package land.oras;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class AnnotationsTest {

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
