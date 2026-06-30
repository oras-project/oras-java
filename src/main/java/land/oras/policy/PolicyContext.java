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
 */
@NullMarked
public final class PolicyContext {

    private static final SigstoreSignatureFetcher NO_SIGNATURES = List::of;

    private final Transport transport;
    private final String scope;
    private final @Nullable String imageDigest;
    private final @Nullable String reference;
    private final SigstoreSignatureFetcher sigstoreSignatureFetcher;

    /**
     * Create a content-bound policy context for a resolved image.
     *
     * @param transport the transport (e.g. {@link Transport#DOCKER}).
     * @param scope the matched image scope (registry + path, without tag/digest).
     * @param imageDigest the resolved image digest, e.g. {@code "sha256:abc..."}.
     * @param reference the full image reference being pulled (for diagnostics).
     * @param sigstoreSignatureFetcher supplies the attached signatures for verification.
     */
    public PolicyContext(
            Transport transport,
            String scope,
            String imageDigest,
            String reference,
            SigstoreSignatureFetcher sigstoreSignatureFetcher) {
        this.transport = transport;
        this.scope = scope;
        this.imageDigest = imageDigest;
        this.reference = reference;
        this.sigstoreSignatureFetcher = sigstoreSignatureFetcher;
    }

    /**
     * Private constructor
     * @param transport The transport
     * @param scope the matched image scope (registry + path, without tag/digest).
     */
    private PolicyContext(Transport transport, String scope) {
        this.transport = transport;
        this.scope = scope;
        this.imageDigest = null;
        this.reference = null;
        this.sigstoreSignatureFetcher = NO_SIGNATURES;
    }

    /**
     * Create a content-free policy context that carries only the transport and scope. Signature-based
     * requirements cannot be verified against it.
     *
     * @param transport the transport (e.g. {@link Transport#DOCKER}).
     * @param scope     the matched image scope.
     * @return a content-free context.
     */
    public static PolicyContext forScope(Transport transport, String scope) {
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
     * Return the transport.
     *
     * @return the transport, e.g. {@link Transport#DOCKER}.
     */
    public Transport getTransport() {
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
     * Fetch the bundles attached to the image.
     *
     * @return the bundle blob bytes; empty if no signatures are attached.
     */
    public List<byte[]> fetchSignatureBundle() {
        return sigstoreSignatureFetcher.fetchBundle();
    }
}
