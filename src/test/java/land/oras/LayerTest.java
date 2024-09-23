package land.oras;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class LayerTest {

    @Test
    void shouldReadLayer() {
        String json = sampleLayer();
        Layer layer = Layer.fromJson(json);
        assertEquals("application/vnd.oci.image.layer.v1.tar+gzip", layer.getMediaType());
        assertEquals("sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890", layer.getDigest());
        assertEquals(32654, layer.getSize());
        assertEquals(json, layer.toJson());
    }

    @Test
    void shouldHaveEmptyLayer() {
        String json = emptyLayer();
        assertEquals(Layer.fromJson(json).toJson(), Layer.empty().toJson());
    }

    private String emptyLayer() {
        return """
            {
              "mediaType": "application/vnd.oci.empty.v1+json",
              "digest": "sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a",
              "size": 2,
              "data": "e30=",
              "annotations": {}
            }
        """;
    }

    /**
     * A sample manifest
     * @return The manifest
     */
    private String sampleLayer() {
        return Layer.fromJson(
                        """
                            {
                              "mediaType": "application/vnd.oci.image.layer.v1.tar+gzip",
                              "digest": "sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                              "size": 32654
                            }
                        """)
                .toJson();
    }
}
