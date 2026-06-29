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

package land.oras.policy;

import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * Supplies the raw signature artifacts attached to an image so that policy requirements can verify
 * them without the {@code land.oras.policy} package depending on the registry implementation.
 *
 * <p>The registry adapts itself to this interface, fetching Sigstore bundle blobs from the
 * referrers attached to the image being evaluated.
 */
@NullMarked
@FunctionalInterface
public interface SignatureFetcher {

    /**
     * Fetch the raw bytes of every Sigstore bundle ({@code application/vnd.dev.sigstore.bundle.v0.3+json})
     * attached to the image as a referrer.
     *
     * @return the bundle blob bytes; empty if the image has no attached Sigstore signatures.
     */
    List<byte[]> fetchSigstoreBundles();
}
