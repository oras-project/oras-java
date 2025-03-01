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

import land.oras.utils.JsonUtils;

/**
 * Index from an OCI layout
 */
public class OciLayout {

    private String imageLayoutVersion;

    /**
     * Private constructor
     */
    private OciLayout() {}

    /**
     * Get the image layout version
     * @return The image layout version
     */
    public String getImageLayoutVersion() {
        return imageLayoutVersion;
    }

    /**
     * Return the JSON representation of the referrers
     * @return The JSON string
     */
    public String toJson() {
        return JsonUtils.toJson(this);
    }

    /**
     * Create the OCI layout file from a JSON string
     * @param json The JSON string
     * @return The OCI layout
     */
    public static OciLayout fromJson(String json) {
        return JsonUtils.fromJson(json, OciLayout.class);
    }
}
