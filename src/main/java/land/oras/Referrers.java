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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Objects;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;

/**
 * List of referrers
 */
@OrasModel
@JsonPropertyOrder({Const.JSON_PROPERTY_MEDIA_TYPE, Const.JSON_PROPERTY_MANIFESTS})
@JsonInclude(JsonInclude.Include.NON_NULL) // We need to serialize empty list of manifests
public class Referrers {

    private String mediaType;
    private List<ManifestDescriptor> manifests;

    /**
     * Private constructor
     */
    @JsonCreator
    private Referrers(@JsonProperty(Const.JSON_PROPERTY_MANIFESTS) List<ManifestDescriptor> manifests) {
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Referrers referrers = (Referrers) o;
        return Objects.equals(toJson(), referrers.toJson());
    }

    @Override
    public int hashCode() {
        return Objects.hash(toJson());
    }

    @Override
    public String toString() {
        return toJson();
    }
}
