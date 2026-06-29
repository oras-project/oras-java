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
import org.jspecify.annotations.Nullable;

/**
 * Carries the data a {@link PolicyRequirement} needs to evaluate an image.
 *
 * <p>A context is either:
 * <ul>
 *   <li><strong>content-free</strong> (created with {@link #forScope(String, String)}): only the
 *       transport and scope are known. This is the lightweight gate used by
 *       {@link ContainersPolicy#isAllowed(String, String)} on any operation, including push.
 *       Signature-based requirements cannot be enforced and allow the operation to proceed.</li>
 *   <li><strong>content-bound</strong> (created with the full constructor): the resolved manifest
 *       digest and a {@link SignatureFetcher} are available, so signatures can be verified. This is
 *       used at pull time once the image has been resolved.</li>
 * </ul>
 */
@NullMarked
public final class PolicyContext {

    private static final SignatureFetcher NO_SIGNATURES = List::of;

    private final String transport;
    private final String scope;
    private final @Nullable String imageDigest;
    private final @Nullable String reference;
    private final SignatureFetcher signatureFetcher;

    /**
     * Create a content-bound policy context for a resolved image.
     *
     * @param transport        the transport name (e.g. {@code "docker"}).
     * @param scope            the matched image scope (registry + path, without tag/digest).
     * @param imageDigest      the resolved image digest, e.g. {@code "sha256:abc..."}.
     * @param reference        the full image reference being pulled (for diagnostics).
     * @param signatureFetcher supplies the attached signatures for verification.
     */
    public PolicyContext(
            String transport, String scope, String imageDigest, String reference, SignatureFetcher signatureFetcher) {
        this.transport = transport;
        this.scope = scope;
        this.imageDigest = imageDigest;
        this.reference = reference;
        this.signatureFetcher = signatureFetcher;
    }

    private PolicyContext(String transport, String scope) {
        this.transport = transport;
        this.scope = scope;
        this.imageDigest = null;
        this.reference = null;
        this.signatureFetcher = NO_SIGNATURES;
    }

    /**
     * Create a content-free policy context that carries only the transport and scope. Signature-based
     * requirements cannot be verified against it.
     *
     * @param transport the transport name (e.g. {@code "docker"}).
     * @param scope     the matched image scope.
     * @return a content-free context.
     */
    public static PolicyContext forScope(String transport, String scope) {
        return new PolicyContext(transport, scope);
    }

    /**
     * Whether this context carries resolved image content (a digest and a signature fetcher) and can
     * therefore be used to verify signatures.
     *
     * @return {@code true} if a resolved digest is available.
     */
    public boolean hasContent() {
        return imageDigest != null;
    }

    /**
     * Return the transport name.
     *
     * @return the transport, e.g. {@code "docker"}.
     */
    public String getTransport() {
        return transport;
    }

    /**
     * Return the matched image scope.
     *
     * @return the scope (registry + path, without tag or digest).
     */
    public String getScope() {
        return scope;
    }

    /**
     * Return the resolved image digest, or {@code null} for a content-free context.
     *
     * @return the digest, e.g. {@code "sha256:abc..."}, or {@code null}.
     */
    public @Nullable String getImageDigest() {
        return imageDigest;
    }

    /**
     * Return the full image reference being pulled, or {@code null} for a content-free context.
     *
     * @return the reference for diagnostics, or {@code null}.
     */
    public @Nullable String getReference() {
        return reference;
    }

    /**
     * Fetch the Sigstore bundles attached to the image.
     *
     * @return the bundle blob bytes; empty if no signatures are attached.
     */
    public List<byte[]> fetchSigstoreBundles() {
        return signatureFetcher.fetchSigstoreBundles();
    }
}
