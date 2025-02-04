package land.oras.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class DigestUtilsTest {

    /**
     * Blob temporary dir
     */
    @TempDir
    private Path blobDir;

    @Test
    void testByteArray() {
        assertEquals(
                "sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                DigestUtils.sha256("hello".getBytes()));
    }

    @Test
    void testFile() throws IOException {
        Files.createFile(blobDir.resolve("hello.txt"));
        Files.writeString(blobDir.resolve("hello.txt"), "hello");
        assertEquals(
                "sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                DigestUtils.digest("SHA-256", blobDir.resolve("hello.txt")));
    }

    @Test
    void testLargeFile() throws IOException {
        Files.createFile(blobDir.resolve("large.txt"));
        Files.writeString(blobDir.resolve("large.txt"), "a".repeat(1000000));
        assertEquals(
                "sha256:cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
                DigestUtils.digest("SHA-256", blobDir.resolve("large.txt")));
    }
}
