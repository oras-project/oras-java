package land.oras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class ContainerRefTest {

    @Test
    void shouldParseImageWithAllParts() {
        ContainerRef containerRef =
                ContainerRef.parse("docker.io/library/foo/hello-world:latest@sha256:1234567890abcdef");
        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("library/foo", containerRef.getNamespace());
        assertEquals("hello-world", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());
    }

    @Test
    void shouldParseImageWithNoNamespace() {
        ContainerRef containerRef = ContainerRef.parse("docker.io/hello-world:latest@sha256:1234567890abcdef");
        assertEquals("docker.io", containerRef.getRegistry());
        assertNull(containerRef.getNamespace());
        assertEquals("hello-world", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());
    }

    @Test
    void shouldParseImageWithNoTag() {
        ContainerRef containerRef = ContainerRef.parse("docker.io/hello-world@sha256:1234567890abcdef");
        assertEquals("docker.io", containerRef.getRegistry());
        assertNull(containerRef.getNamespace());
        assertEquals("hello-world", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());
    }

    @Test
    void shouldParseImageWithNoDigest() {
        ContainerRef containerRef = ContainerRef.parse("docker.io/hello-world:latest");
        assertEquals("docker.io", containerRef.getRegistry());
        assertNull(containerRef.getNamespace());
        assertEquals("hello-world", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertNull(containerRef.getDigest());
    }

    @Test
    void shouldParseImageWithNoRegistry() {
        ContainerRef containerRef = ContainerRef.parse("hello-world:latest");
        assertEquals("docker.io", containerRef.getRegistry());
        assertNull(containerRef.getNamespace());
        assertEquals("hello-world", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertNull(containerRef.getDigest());
    }

    @Test
    void shouldParseImageWithNoTagAndNoRegistry() {
        ContainerRef containerRef = ContainerRef.parse("hello-world");
        assertEquals("docker.io", containerRef.getRegistry());
        assertNull(containerRef.getNamespace());
        assertEquals("hello-world", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertNull(containerRef.getDigest());
    }

    @Test
    void shouldGetTagsPath() {
        ContainerRef containerRef =
                ContainerRef.parse("docker.io/library/foo/hello-world:latest@sha256:1234567890abcdef");
        assertEquals("docker.io/v2/library/foo/hello-world/tags/list", containerRef.getTagsPath());
    }
}
