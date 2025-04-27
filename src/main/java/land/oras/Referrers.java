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

import java.util.List;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;

/**
 * List of referrers
 */
@OrasModel
public class Referrers {

    private String mediaType;
    private List<ManifestDescriptor> manifests;

    /**
     * Private constructor
     */
    private Referrers(List<ManifestDescriptor> manifests) {
        this.mediaType = Const.DEFAULT_INDEX_MEDIA_TYPE;
        this.manifests = manifests;
    }

    /**
     * Get the media type
     * @return The media type
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * Get the list of manifests
     * @return The list of manifests
     */
    public List<ManifestDescriptor> getManifests() {
        return manifests;
    }

    /**
     * Return the JSON representation of the referrers
     * @return The JSON string
     */
    public String toJson() {
        return JsonUtils.toJson(this);
    }

    /**
     * Create a manifest from a JSON string
     * @param json The JSON string
     * @return The referrers
     */
    public static Referrers fromJson(String json) {
        return JsonUtils.fromJson(json, Referrers.class);
    }

    /**
     * Create a referrers object from a list of descriptors
     * @param descriptors The list of descriptors
     * @return The referrers object
     */
    public static Referrers from(List<ManifestDescriptor> descriptors) {
        return new Referrers(descriptors);
    }
}
