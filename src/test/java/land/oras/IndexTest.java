/*-
 * =LICENSE=
 * ORAS Java SDK
 * ===
 * Copyright (C) 2024 - 2026 ORAS
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

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import land.oras.utils.Const;
import land.oras.utils.SupportedAlgorithm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class IndexTest {

    @Test
    void shouldReadAndWriteIndex() {
        String json =
                "{\"schemaVersion\":2,\"manifests\":[{\"mediaType\":\"application/vnd.oci.image.manifest.v1+json\",\"digest\":\"sha256:f381775b1f558b02165b5dfe1b2f973387d995e18302c4039daabd32f938cb27\",\"size\":559}]}";
        Index index = Index.fromJson(json);
        assertNull(index.getMediaType());
        assertEquals(2, index.getSchemaVersion());
        assertEquals(1, index.getManifests().size());
        assertNull(index.getArtifactTypeAsString());
        assertEquals(Const.DEFAULT_ARTIFACT_MEDIA_TYPE, index.getArtifactType().getMediaType());
        assertNull(index.getAnnotations());
        assertNull(index.getDescriptor());
        assertEquals(
                "sha256:f381775b1f558b02165b5dfe1b2f973387d995e18302c4039daabd32f938cb27",
                index.getManifests().get(0).getDigest());
        assertEquals(559, index.getManifests().get(0).getSize());
        assertEquals(json, index.getJson());
        String result = index.toJson();
        assertEquals(
                "{\"schemaVersion\":2,\"manifests\":[{\"mediaType\":\"application/vnd.oci.image.manifest.v1+json\",\"digest\":\"sha256:f381775b1f558b02165b5dfe1b2f973387d995e18302c4039daabd32f938cb27\",\"size\":559}]}",
                result);
    }

    @Test
    void shouldAddArtifactType() {
        Index index = Index.fromManifests(List.of());
        index = index.withArtifactType(ArtifactType.from("application/vnd.opentofu.provider"));
        assertNotNull(index.getArtifactType());
        assertEquals(
                "application/vnd.opentofu.provider", index.getArtifactType().getMediaType());
        assertEquals(
                "{\"schemaVersion\":2,\"mediaType\":\"application/vnd.oci.image.index.v1+json\",\"artifactType\":\"application/vnd.opentofu.provider\",\"manifests\":[]}",
                index.toJson());
    }

    @Test
    void shouldReadAndWriteIndexWithAnnotations() {
        String json =
                "{\"schemaVersion\":2,\"manifests\":[{\"mediaType\":\"application/vnd.oci.image.manifest.v1+json\",\"digest\":\"sha256:f381775b1f558b02165b5dfe1b2f973387d995e18302c4039daabd32f938cb27\",\"size\":559,\"annotations\":{\"foo\":\"bar\"}}]}";
        Index index = Index.fromJson(json);
        assertNull(index.getMediaType());
        assertEquals(2, index.getSchemaVersion());
        assertEquals(1, index.getManifests().size());
        assertNull(index.getArtifactTypeAsString());
        assertEquals(Const.DEFAULT_ARTIFACT_MEDIA_TYPE, index.getArtifactType().getMediaType());
        assertNull(index.getAnnotations());
        assertNull(index.getDescriptor());
        assertEquals(
                "sha256:f381775b1f558b02165b5dfe1b2f973387d995e18302c4039daabd32f938cb27",
                index.getManifests().get(0).getDigest());
        assertEquals(559, index.getManifests().get(0).getSize());
        assertEquals(json, index.getJson());
        String result = index.toJson();
        assertEquals(
                "{\"schemaVersion\":2,\"manifests\":[{\"mediaType\":\"application/vnd.oci.image.manifest.v1+json\",\"digest\":\"sha256:f381775b1f558b02165b5dfe1b2f973387d995e18302c4039daabd32f938cb27\",\"size\":559,\"annotations\":{\"foo\":\"bar\"}}]}",
                result);
    }

    @Test
    void shouldNotWriteEmptyAnnotations() {
        String json =
                "{\"schemaVersion\":2,\"manifests\":[{\"mediaType\":\"application/vnd.oci.image.manifest.v1+json\",\"digest\":\"sha256:f381775b1f558b02165b5dfe1b2f973387d995e18302c4039daabd32f938cb27\",\"size\":559,\"annotations\":{}}]}";
        Index index = Index.fromJson(json);
        assertNull(index.getAnnotations());
    }

    @Test
    void shouldReadAndWriteIndexWithArtifactType() {
        String json =
                "{\"schemaVersion\":2,\"artifactType\":\"foo/bar\",\"manifests\":[{\"mediaType\":\"application/vnd.oci.image.manifest.v1+json\",\"digest\":\"sha256:f381775b1f558b02165b5dfe1b2f973387d995e18302c4039daabd32f938cb27\",\"size\":559}]}";
        Index index = Index.fromJson(json);
        assertNull(index.getMediaType());
        assertEquals(2, index.getSchemaVersion());
        assertEquals(1, index.getManifests().size());
        assertNull(index.getAnnotations());
        assertNotNull(index.getArtifactType());
        assertEquals("foo/bar", index.getArtifactType().getMediaType());
        assertNull(index.getDescriptor());
        assertEquals(
                "sha256:f381775b1f558b02165b5dfe1b2f973387d995e18302c4039daabd32f938cb27",
                index.getManifests().get(0).getDigest());
        assertEquals(559, index.getManifests().get(0).getSize());
        assertEquals(json, index.toJson());
    }

    @Test
    void shouldAddManifests() {
        Index index = Index.fromManifests(List.of());
        index = index.withNewManifests(Manifest.empty().getDescriptor());
        assertEquals(1, index.getManifests().size());

        Manifest newManifest = Manifest.empty().withAnnotations(Map.of("foo", "bar"));
        String digest =
                SupportedAlgorithm.getDefault().digest(newManifest.toJson().getBytes());
        int size = newManifest.toJson().getBytes().length;
        ManifestDescriptor descriptor = ManifestDescriptor.of(Const.DEFAULT_MANIFEST_MEDIA_TYPE, digest, size)
                .withPlatform(Platform.unknown())
                .withAnnotations(newManifest.getAnnotations());
        newManifest.withDescriptor(descriptor);
        index = index.withNewManifests(descriptor);

        // Add new manifest with linux platform
        Manifest newManifestWithPlatform = Manifest.empty();
        String digest2 = SupportedAlgorithm.getDefault()
                .digest(newManifestWithPlatform.toJson().getBytes());
        int size2 = newManifestWithPlatform.toJson().getBytes().length;
        ManifestDescriptor descriptor2 = ManifestDescriptor.of(Const.DEFAULT_MANIFEST_MEDIA_TYPE, digest2, size2)
                .withPlatform(Platform.linuxAmd64());
        newManifestWithPlatform.withDescriptor(descriptor2);
        index = index.withNewManifests(descriptor2);

        assertEquals(3, index.getManifests().size());
        assertNotNull(index.getManifests().get(1).getAnnotations());
        assertEquals(1, index.getManifests().get(1).getAnnotations().size());

        // Filter unspecified platforms
        List<ManifestDescriptor> filtered = index.unspecifiedPlatforms();
        assertEquals(2, filtered.size());

        // Filter by platform
        ManifestDescriptor linuxManifest = index.findUnique(Platform.linuxAmd64());
        assertNotNull(linuxManifest);

        // Not found
        ManifestDescriptor notFound = index.findUnique(Platform.of("darwin", "arm64"));
        assertNull(notFound);
    }

    @Test
    void shouldAddSubject() {
        Index index = Index.fromManifests(List.of());
        index = index.withSubject(Subject.of(Const.DEFAULT_MANIFEST_MEDIA_TYPE, "sha256:123", 123));
        Subject subject = index.getSubject();
        assertNotNull(subject);
        assertEquals(Const.DEFAULT_MANIFEST_MEDIA_TYPE, subject.getMediaType());
        assertEquals("sha256:123", subject.getDigest());
        assertEquals(123, subject.getSize());
        assertEquals(
                "{\"schemaVersion\":2,\"mediaType\":\"application/vnd.oci.image.index.v1+json\",\"subject\":{\"mediaType\":\"application/vnd.oci.image.manifest.v1+json\",\"digest\":\"sha256:123\",\"size\":123},\"manifests\":[]}",
                index.toJson());
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
        assertNotNull(index.getManifests().get(1).getAnnotations());
        assertTrue(index.getManifests().get(1).getAnnotations().containsKey(Const.ANNOTATION_REF));
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

    @Test
    void testEquals() {

        String json1 =
                "{\"schemaVersion\":2,\"manifests\":[{\"mediaType\":\"application/vnd.oci.image.manifest.v1+json\",\"digest\":\"sha256:f381775b1f558b02165b5dfe1b2f973387d995e18302c4039daabd32f938cb27\",\"size\":559}]}";
        String json2 =
                "{\"schemaVersion\":2,\"manifests\":[{\"mediaType\":\"application/vnd.oci.image.manifest.v1+json\",\"digest\":\"sha256:f381775b1f558b02165b5dfe1b2f973387d995e18302c4039daabd32f938cb27\",\"size\":559}]}";

        // Data
        Index object1 = Index.fromJson(json1);
        Index object2 = Index.fromJson(json2);
        assertEquals(object1, object2);
        assertEquals(object1.hashCode(), object2.hashCode());
    }

    @Test
    void testToString() {
        String json =
                "{\"schemaVersion\":2,\"manifests\":[{\"mediaType\":\"application/vnd.oci.image.manifest.v1+json\",\"digest\":\"sha256:f381775b1f558b02165b5dfe1b2f973387d995e18302c4039daabd32f938cb27\",\"size\":559}]}";
        Index index = Index.fromJson(json);
        String json1 = index.toString();
        assertNotNull(json);
        assertEquals(
                "{\"schemaVersion\":2,\"manifests\":[{\"mediaType\":\"application/vnd.oci.image.manifest.v1+json\",\"digest\":\"sha256:f381775b1f558b02165b5dfe1b2f973387d995e18302c4039daabd32f938cb27\",\"size\":559}]}",
                json1);
    }
}
