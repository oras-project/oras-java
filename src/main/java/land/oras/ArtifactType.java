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

import land.oras.exception.OrasException;
import land.oras.utils.Const;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Class for artifact type
 */
@NullMarked
public class ArtifactType {

    private final String mediaType;

    private ArtifactType(String mediaType) {
        this.mediaType = mediaType;
    }

    /**
     * Get the media type
     * @return The media type
     */
    public String getMediaType() {
        return mediaType;
    }

    @Override
    public String toString() {
        return mediaType;
    }

    /**
     * Create an artifact type
     * @param artifactType The artifact type. Can be null
     * @return The artifact type
     */
    public static ArtifactType from(@Nullable String artifactType) {
        if (artifactType == null) {
            return unknown();
        }
        // Must match https://datatracker.ietf.org/doc/html/rfc6838
        if (!artifactType.matches("^[a-zA-Z0-9!#$&-^_]+/[a-zA-Z0-9!#$&-^_]+$")) {
            throw new OrasException("Invalid artifact type: %s".formatted(artifactType));
        }
        return new ArtifactType(artifactType);
    }

    /**
     * Create an unknown artifact type
     * @return The unknown artifact type
     */
    public static ArtifactType unknown() {
        return new ArtifactType(Const.DEFAULT_ARTIFACT_MEDIA_TYPE);
    }
}
