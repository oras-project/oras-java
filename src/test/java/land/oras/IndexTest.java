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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import land.oras.utils.Const;
import land.oras.utils.SupportedAlgorithm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class IndexTest {

    @Test
    void shouldReadAndWriteIndex() {
        String json =
                "{\"schemaVersion\":2,\"manifests\":[{\"mediaType\":\"application/vnd.oci.image.manifest.v1+json\",\"digest\":\"sha256:f381775b1f558b02165b5dfe1b2f973387d995e18302c4039daabd32f938cb27\",\"size\":559}]}";
        Index index = Index.fromJson(json);
        assertNull(index.getMediaType());
        assertEquals(2, index.getSchemaVersion());
        assertEquals(1, index.getManifests().size());
        assertNull(index.getArtifactType());
        assertNull(index.getDescriptor());
        assertEquals(
                "sha256:f381775b1f558b02165b5dfe1b2f973387d995e18302c4039daabd32f938cb27",
                index.getManifests().get(0).getDigest());
        assertEquals(559, index.getManifests().get(0).getSize());
        assertEquals(json, index.toJson());
        index.toJson();
    }

    @Test
    void shouldAddManifest() {
        Index index = Index.fromManifests(List.of());
        index = index.withNewManifests(Manifest.empty().getDescriptor());
        assertEquals(1, index.getManifests().size());

        Manifest newManifest = Manifest.empty().withAnnotations(Map.of("foo", "bar"));
        String digest =
                SupportedAlgorithm.getDefault().digest(newManifest.toJson().getBytes());
        int size = newManifest.toJson().getBytes().length;
        ManifestDescriptor descriptor = ManifestDescriptor.of(Const.DEFAULT_MANIFEST_MEDIA_TYPE, digest, size);
        newManifest.withDescriptor(descriptor);
        index = index.withNewManifests(descriptor);
        assertEquals(2, index.getManifests().size());
    }

    @Test
    void shouldNotAddIfSameDigest() {
        Index index = Index.fromManifests(List.of());
        index = index.withNewManifests(Manifest.empty().getDescriptor());
        assertEquals(1, index.getManifests().size());
        index = index.withNewManifests(Manifest.empty().getDescriptor());
        assertEquals(1, index.getManifests().size());
        index = index.withNewManifests(Manifest.empty().getDescriptor());
        assertEquals(1, index.getManifests().size());
    }

    @Test
    void shouldMoveRefAndSetNullAnnotations() {
        Index index = Index.fromManifests(List.of());
        index = index.withNewManifests(
                Manifest.empty().getDescriptor().withAnnotations(Map.of(Const.ANNOTATION_REF, "latest")));
        assertEquals(1, index.getManifests().size());
        index = index.withNewManifests(ManifestDescriptor.of(Const.DEFAULT_MANIFEST_MEDIA_TYPE, "sha256:123", 123)
                .withAnnotations(Map.of(Const.ANNOTATION_REF, "latest")));
        assertEquals(2, index.getManifests().size());

        // Ensure 1st descriptor has null annotations
        assertNull(index.getManifests().get(0).getAnnotations());

        // Ensure 2nd descriptor has ref
        assertEquals("latest", index.getManifests().get(1).getAnnotations().get(Const.ANNOTATION_REF));
    }

    @Test
    void shouldNotMoveRefIfDifferent() {
        Index index = Index.fromManifests(List.of());
        index = index.withNewManifests(
                Manifest.empty().getDescriptor().withAnnotations(Map.of(Const.ANNOTATION_REF, "latest")));
        assertEquals(1, index.getManifests().size());
        index = index.withNewManifests(ManifestDescriptor.of(Const.DEFAULT_MANIFEST_MEDIA_TYPE, "sha256:123", 123)
                .withAnnotations(Map.of(Const.ANNOTATION_REF, "stable")));
        assertEquals(2, index.getManifests().size());

        // No change
        assertEquals("latest", index.getManifests().get(0).getAnnotations().get(Const.ANNOTATION_REF));

        // Added ref
        assertEquals("stable", index.getManifests().get(1).getAnnotations().get(Const.ANNOTATION_REF));
    }

    @Test
    void shouldKeepExistingAnnotation() {
        Index index = Index.fromManifests(List.of());
        index = index.withNewManifests(
                Manifest.empty().getDescriptor().withAnnotations(Map.of(Const.ANNOTATION_REF, "latest", "foo", "bar")));
        assertEquals(1, index.getManifests().size());
        index = index.withNewManifests(ManifestDescriptor.of(Const.DEFAULT_MANIFEST_MEDIA_TYPE, "sha256:123", 123)
                .withAnnotations(Map.of(Const.ANNOTATION_REF, "latest")));
        assertEquals(2, index.getManifests().size());

        // One annotation
        assertEquals(1, index.getManifests().get(0).getAnnotations().size());
        assertEquals("bar", index.getManifests().get(0).getAnnotations().get("foo"));

        // Added ref
        assertEquals("latest", index.getManifests().get(1).getAnnotations().get(Const.ANNOTATION_REF));

        // Add one more
        index = index.withNewManifests(ManifestDescriptor.of(Const.DEFAULT_MANIFEST_MEDIA_TYPE, "sha256:532", 123)
                .withAnnotations(Map.of("test", "hello")));
        assertEquals(3, index.getManifests().size());
        assertEquals(1, index.getManifests().get(0).getAnnotations().size());
        assertEquals("bar", index.getManifests().get(0).getAnnotations().get("foo"));
        assertEquals(1, index.getManifests().get(1).getAnnotations().size());
        assertEquals("latest", index.getManifests().get(1).getAnnotations().get(Const.ANNOTATION_REF));
        assertEquals(1, index.getManifests().get(2).getAnnotations().size());
        assertEquals("hello", index.getManifests().get(2).getAnnotations().get("test"));

        // With null annotations
        index = index.withNewManifests(ManifestDescriptor.of(Const.DEFAULT_MANIFEST_MEDIA_TYPE, "sha256:789", 123)
                .withAnnotations(null));
        assertEquals(4, index.getManifests().size());
        assertEquals(1, index.getManifests().get(0).getAnnotations().size());
        assertEquals("bar", index.getManifests().get(0).getAnnotations().get("foo"));
        assertEquals(1, index.getManifests().get(1).getAnnotations().size());
        assertEquals("latest", index.getManifests().get(1).getAnnotations().get(Const.ANNOTATION_REF));
        assertEquals(1, index.getManifests().get(2).getAnnotations().size());
        assertEquals("hello", index.getManifests().get(2).getAnnotations().get("test"));
        assertNull(index.getManifests().get(3).getAnnotations());
    }
}
