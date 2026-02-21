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

package land.oras.utils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.jspecify.annotations.NullMarked;

/**
 * Constants used in the SDK.
 */
@NullMarked
public final class Const {

    /**
     * Hidden constructor
     */
    private Const() {
        // Private constructor
    }

    /**
     * JSON property for media type
     */
    public static final String JSON_PROPERTY_MEDIA_TYPE = "mediaType";

    /**
     * JSON property for artifact type
     */
    public static final String JSON_PROPERTY_ARTIFACT_TYPE = "artifactType";

    /**
     * JSON property for schema version
     */
    public static final String JSON_PROPERTY_SCHEMA_VERSION = "schemaVersion";

    /**
     * JSON property for subject
     */
    public static final String JSON_PROPERTY_SUBJECT = "subject";

    /**
     * JSON property for config
     */
    public static final String JSON_PROPERTY_CONFIG = "config";

    /**
     * JSON property for layers
     */
    public static final String JSON_PROPERTY_LAYERS = "layers";

    /**
     * JSON property for digest
     */
    public static final String JSON_PROPERTY_DIGEST = "digest";

    /**
     * JSON property for size
     */
    public static final String JSON_PROPERTY_SIZE = "size";

    /**
     * JSON property for annotations
     */
    public static final String JSON_PROPERTY_ANNOTATIONS = "annotations";

    /**
     * JSON property for platform
     */
    public static final String JSON_PROPERTY_PLATFORM = "platform";

    /**
     * JSON property for manifests
     */
    public static final String JSON_PROPERTY_MANIFESTS = "manifests";

    /**
     * JSON property for data (base64 encoded)
     */
    public static final String JSON_PROPERTY_DATA = "data";

    /**
     * Default registry when no unqualified-search-registries is set in the config
     */
    public static final String DEFAULT_REGISTRY = "docker.io";

    /**
     * Default tag
     */
    public static final String DEFAULT_TAG = "latest";

    /**
     * Index file in OCI layout
     */
    public static final String OCI_LAYOUT_INDEX = "index.json";

    /**
     * Layout folder in OCI layout
     */
    public static final String OCI_LAYOUT_FILE = "oci-layout";

    /**
     * Blobs folder in OCI layout
     */
    public static final String OCI_LAYOUT_BLOBS = "blobs";

    /**
     * The default blob directory media type
     */
    public static final String DEFAULT_BLOB_DIR_MEDIA_TYPE = "application/vnd.oci.image.layer.v1.tar+gzip";

    /**
     * The blob directory media type for zstd compression
     */
    public static final String BLOB_DIR_ZSTD_MEDIA_TYPE = "application/vnd.oci.image.layer.v1.tar+zstd";

    /**
     * The default artifact media type if not specified
     */
    public static final String DEFAULT_ARTIFACT_MEDIA_TYPE = "application/vnd.unknown.artifact.v1";

    /**
     * The default blob media type if file type cannot be determined
     */
    public static final String DEFAULT_BLOB_MEDIA_TYPE = "application/vnd.oci.image.layer.v1.tar";

    /**
     * The default descriptor media type
     */
    public static final String DEFAULT_DESCRIPTOR_MEDIA_TYPE = "application/octet-stream";

    /**
     * The default JSON media type
     */
    public static final String DEFAULT_JSON_MEDIA_TYPE = "application/json";

    /**
     * The default empty media type
     */
    public static final String DEFAULT_EMPTY_MEDIA_TYPE = "application/vnd.oci.empty.v1+json";

    /**
     * Default index media type
     */
    public static final String DEFAULT_INDEX_MEDIA_TYPE = "application/vnd.oci.image.index.v1+json";

    /**
     * The artifact manifest media type
     */
    public static final String ARTIFACT_MANIFEST_MEDIA_TYPE = "application/vnd.oci.artifact.manifest.v1+json";

    /**
     * Docker distribution manifest type
     */
    public static final String DOCKER_MANIFEST_MEDIA_TYPE = "application/vnd.docker.distribution.manifest.v2+json";

    /**
     * Docker index media type (manifest list or fat manifest)
     */
    public static final String DOCKER_INDEX_MEDIA_TYPE = "application/vnd.docker.distribution.manifest.list.v2+json";

    /**
     * The default manifest media type
     */
    public static final String DEFAULT_MANIFEST_MEDIA_TYPE = "application/vnd.oci.image.manifest.v1+json";

    /**
     * The default accept type for the manifest
     */
    public static final String MANIFEST_ACCEPT_TYPE = "%s, %s, %s, %s, %s"
            .formatted(
                    DEFAULT_INDEX_MEDIA_TYPE,
                    DEFAULT_MANIFEST_MEDIA_TYPE,
                    ARTIFACT_MANIFEST_MEDIA_TYPE,
                    DOCKER_INDEX_MEDIA_TYPE,
                    DOCKER_MANIFEST_MEDIA_TYPE);

    /**
     * Annotation for the title
     */
    public static final String ANNOTATION_TITLE = "org.opencontainers.image.title";

    /**
     * Annotation for the description
     */
    public static final String ANNOTATION_DESCRIPTION = "org.opencontainers.image.description";

    /**
     * Annotation for the crated date
     */
    public static final String ANNOTATION_CREATED = "org.opencontainers.image.created";

    /**
     * Annotation for the ref name
     */
    public static final String ANNOTATION_REF = "org.opencontainers.image.ref.name";

    /**
     * Annotation for the source
     */
    public static final String ANNOTATION_SOURCE = "org.opencontainers.image.source";

    /**
     * Annotation for the revision
     */
    public static final String ANNOTATION_REVISION = "org.opencontainers.image.revision";

    /**
     * Annotation for the base image name
     */
    public static final String ANNOTATION_IMAGE_BASE_NAME = "org.opencontainers.image.base.name";

    /**
     * Annotation for the image URL
     */
    public static final String ANNOTATION_IMAGE_URL = "org.opencontainers.image.url";

    /**
     * Annotation for the image version
     */
    public static final String ANNOTATION_IMAGE_VERSION = "org.opencontainers.image.version";

    /**
     * The platform OS key
     */
    public static final String PLATFORM_OS = "os";

    /**
     * The platform architecture key
     */
    public static final String PLATFORM_ARCHITECTURE = "architecture";

    /**
     * The platform variant key, which can be used in the annotation "variant" to specify the variant of the architecture, such as armv7 or armv8
     */
    public static final String PLATFORM_VARIANT = "variant";

    /**
     * The default value for unknown platform information
     */
    public static final String PLATFORM_UNKNOWN = "unknown";

    /**
     * The platform value for linux os
     */
    public static final String PLATFORM_LINUX = "linux";

    /**
     * The platform value for windows OS
     */
    public static final String PLATFORM_WINDOWS = "windows";

    /**
     * The platform value for amd64 architecture
     */
    public static final String PLATFORM_ARCHITECTURE_AMD64 = "amd64";

    /**
     * The platform value for 386 architecture
     */
    public static final String PLATFORM_ARCHITECTURE_386 = "386";

    /**
     * The platform value for arm architecture
     */
    public static final String PLATFORM_ARCHITECTURE_ARM = "arm";

    /**
     * The platform value for arm64 architecture
     */
    public static final String PLATFORM_ARCHITECTURE_ARM64 = "arm64";

    /**
     * The v5 variant for arm architecture, which can be used in the annotation "variant" to specify the variant of the arm architecture
     */
    public static final String VARIANT_V5 = "v5";

    /**
     * The v6 variant for arm architecture, which can be used in the annotation "variant" to specify the variant of the arm architecture
     */
    public static final String VARIANT_V6 = "v6";

    /**
     * The v7 variant for arm architecture, which can be used in the annotation "variant" to specify the variant of the arm architecture
     */
    public static final String VARIANT_V7 = "v7";

    /**
     * The v8 variant for arm architecture, which can be used in the annotation "variant" to specify the variant of the arm architecture
     */
    public static final String VARIANT_V8 = "v8";

    /**
     * Get the current timestamp for the created annotation
     * @return The current timestamp
     */
    public static String currentTimestamp() {
        return Instant.now()
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * Annotation of the uncompressed dir content
     */
    public static final String ANNOTATION_ORAS_CONTENT_DIGEST = "io.deis.oras.content.digest";

    /**
     * Annotation to unpack the content
     */
    public static final String ANNOTATION_ORAS_UNPACK = "io.deis.oras.content.unpack";

    /**
     * Authorization header
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * User agent header
     */
    public static final String USER_AGENT_HEADER = "User-Agent";

    /**
     * Content type header
     */
    public static final String CONTENT_TYPE_HEADER = "Content-Type";

    /**
     * Content length header
     */
    public static final String CONTENT_LENGTH_HEADER = "Content-Length";

    /**
     * The Docker content digest header
     */
    public static final String DOCKER_CONTENT_DIGEST_HEADER = "Docker-Content-Digest";

    /**
     * OCI subject header
     */
    public static final String OCI_SUBJECT_HEADER = "OCI-Subject";

    /**
     * Accept header
     */
    public static final String ACCEPT_HEADER = "Accept";

    /**
     * Location header
     */
    public static final String LOCATION_HEADER = "Location";

    /**
     * WWW-Authenticate header
     */
    public static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

    /**
     * Application octet stream header value
     */
    public static final String APPLICATION_OCTET_STREAM_HEADER_VALUE = "application/octet-stream";

    /**
     * Content Range header
     */
    public static final String CONTENT_RANGE_HEADER = "Content-Range";

    /**
     * Range header
     */
    public static final String RANGE_HEADER = "Range";

    /**
     * OCI Chunk Minimum Length header
     */
    public static final String OCI_CHUNK_MIN_LENGTH_HEADER = "OCI-Chunk-Min-Length";
}
