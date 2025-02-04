package land.oras;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Execution(ExecutionMode.CONCURRENT)
public class ManifestTest {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ManifestTest.class);

    @Test
    void shouldReadManifest() {
        String json = sampleManifest();
        Manifest manifest = Manifest.fromJson(json);

        // Assert manifest
        assertEquals(2, manifest.getSchemaVersion());
        assertEquals("application/vnd.oci.image.manifest.v1+json", manifest.getMediaType());
        assertEquals(7023, manifest.getConfig().getSize());
        assertEquals(
                "sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                manifest.getConfig().getDigest());
        assertEquals(
                "application/vnd.oci.image.config.v1+json", manifest.getConfig().getMediaType());
        assertEquals(2, manifest.getLayers().size());
        assertEquals(0, manifest.getAnnotations().size());

        // Assert layer
        assertEquals(32654, manifest.getLayers().get(0).getSize());
        assertEquals(
                "sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                manifest.getLayers().get(0).getDigest());
        assertEquals(
                "application/vnd.oci.image.layer.v1.tar+gzip",
                manifest.getLayers().get(0).getMediaType());
        assertEquals(1048576, manifest.getLayers().get(1).getSize());
        assertEquals(
                "sha256:1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                manifest.getLayers().get(1).getDigest());
        assertEquals(
                "application/vnd.oci.image.layer.v1.tar+gzip",
                manifest.getLayers().get(1).getMediaType());
        manifest.toJson();
    }

    @Test
    void shouldHaveEmptyManifest() {
        assertEquals(
                Manifest.fromJson(emptyManifest()).toJson(), Manifest.empty().toJson());
    }

    private String emptyManifest() {
        return """
            {
              "schemaVersion": 2,
              "mediaType": "application/vnd.oci.image.manifest.v1+json",
              "config": {
                "mediaType": "application/vnd.oci.empty.v1+json",
                "digest": "sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a",
                "size": 2,
                "annotations": {}
              },
              "layers": [],
              "annotations": {}
            }
             """;
    }

    /**
     * A sample manifest
     * @return The manifest
     */
    private String sampleManifest() {
        return """
                {
                  "schemaVersion": 2,
                  "mediaType": "application/vnd.oci.image.manifest.v1+json",
                  "config": {
                    "mediaType": "application/vnd.oci.image.config.v1+json",
                    "digest": "sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                    "size": 7023
                  },
                  "layers": [
                    {
                      "mediaType": "application/vnd.oci.image.layer.v1.tar+gzip",
                      "digest": "sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                      "size": 32654
                    },
                    {
                      "mediaType": "application/vnd.oci.image.layer.v1.tar+gzip",
                      "digest": "sha256:1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                      "size": 1048576
                    }
                  ]
                }
             """;
    }
}
