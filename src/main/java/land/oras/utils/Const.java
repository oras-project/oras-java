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
     * Default registry
     */
    public static final String DEFAULT_REGISTRY = "docker.io";

    /**
     * Default tag
     */
    public static final String DEFAULT_TAG = "latest";

    /**
     * The default blob directory media type
     */
    public static final String DEFAULT_BLOB_DIR_MEDIA_TYPE = "application/vnd.oci.image.layer.v1.tar+gzip";

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
     * The default manifest media type
     */
    public static final String DEFAULT_MANIFEST_MEDIA_TYPE = "application/vnd.oci.image.manifest.v1+json";

    /**
     * Annotation for the title
     */
    public static final String ANNOTATION_TITLE = "org.opencontainers.image.title";

    /**
     * Annotation for the crated date
     */
    public static final String ANNOTATION_CREATED = "org.opencontainers.image.created";

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
     * Content type header
     */
    public static final String CONTENT_TYPE_HEADER = "Content-Type";

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
}
