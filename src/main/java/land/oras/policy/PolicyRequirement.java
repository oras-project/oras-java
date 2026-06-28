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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import land.oras.OrasModel;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single requirement entry inside a containers policy scope.
 *
 * <p>Each requirement is a JSON object whose {@code "type"} field selects the concrete
 * subtype. A scope's requirement list is a logical AND: every requirement must pass for
 * an image to be allowed.
 */
@NullMarked
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = PolicyRequirement.InsecureAcceptAnything.class, name = "insecureAcceptAnything"),
    @JsonSubTypes.Type(value = PolicyRequirement.Reject.class, name = "reject"),
    @JsonSubTypes.Type(value = PolicyRequirement.SignedBy.class, name = "signedBy"),
    @JsonSubTypes.Type(value = PolicyRequirement.SigstoreSigned.class, name = "sigstoreSigned"),
})
@OrasModel
public abstract sealed class PolicyRequirement
        permits PolicyRequirement.InsecureAcceptAnything,
                PolicyRequirement.Reject,
                PolicyRequirement.SignedBy,
                PolicyRequirement.SigstoreSigned {

    /** Package-private constructor; only the sealed subtypes may extend this class. */
    PolicyRequirement() {}

    /**
     * Return the type name of this requirement as it appears in the policy JSON.
     *
     * @return the type string, e.g. {@code "insecureAcceptAnything"}.
     */
    public abstract String getType();

    /**
     * Evaluate this requirement for the given transport and scope.
     *
     * @param transport the transport name (e.g., {@code "docker"}).
     * @param scope the image scope (e.g., {@code "docker.io/library/nginx"}).
     * @return {@code true} if the requirement passes, {@code false} otherwise.
     */
    public abstract boolean evaluate(String transport, String scope);

    @Override
    public String toString() {
        return getType();
    }

    /**
     * Accept any image unconditionally – no signature or digest verification is performed.
     *
     * <p>JSON example:
     * <pre>{@code {"type": "insecureAcceptAnything"}}</pre>
     */
    @OrasModel
    public static final class InsecureAcceptAnything extends PolicyRequirement {

        /**
         * Constructor
         */
        public InsecureAcceptAnything() {}

        @Override
        public String getType() {
            return "insecureAcceptAnything";
        }

        @Override
        public boolean evaluate(String transport, String scope) {
            return true;
        }
    }

    /**
     * Reject every image unconditionally.
     *
     * <p>JSON example:
     * <pre>{@code {"type": "reject"}}</pre>
     */
    @OrasModel
    public static final class Reject extends PolicyRequirement {

        /**
         * Logger
         */
        private static final Logger LOG = LoggerFactory.getLogger(Reject.class);

        /**
         * Constructor
         */
        public Reject() {}

        @Override
        public String getType() {
            return "reject";
        }

        @Override
        public boolean evaluate(String transport, String scope) {
            LOG.debug("Policy: reject for transport='{}' scope='{}'", transport, scope);
            return false;
        }
    }

    /**
     * Require a valid GPG "simple signing" signature.
     *
     * <p>JSON example:
     * <pre>{@code
     * {
     *   "type": "signedBy",
     *   "keyType": "GPGKeys",
     *   "keyPath": "/etc/pki/containers/my-key.gpg",
     *   "signedIdentity": {"type": "matchRepoDigestOrExact"}
     * }
     * }</pre>
     */
    @OrasModel
    public static final class SignedBy extends PolicyRequirement {

        private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SignedBy.class);

        private final @Nullable String keyType;
        private final @Nullable String keyPath;
        private final @Nullable List<String> keyPaths;
        private final @Nullable String keyData;
        private final @Nullable SignedIdentity signedIdentity;

        /**
         * Constructor
         *
         * @param keyType       the key type, currently always {@code "GPGKeys"}.
         * @param keyPath       path to a single GPG keyring file (mutually exclusive with keyPaths/keyData).
         * @param keyPaths      paths to multiple GPG keyring files (mutually exclusive with keyPath/keyData).
         * @param keyData       base64-encoded GPG keyring (mutually exclusive with keyPath/keyPaths).
         * @param signedIdentity identity matching rules; {@code null} defaults to matchRepoDigestOrExact.
         */
        @JsonCreator
        public SignedBy(
                @JsonProperty("keyType") @Nullable String keyType,
                @JsonProperty("keyPath") @Nullable String keyPath,
                @JsonProperty("keyPaths") @Nullable List<String> keyPaths,
                @JsonProperty("keyData") @Nullable String keyData,
                @JsonProperty("signedIdentity") @Nullable SignedIdentity signedIdentity) {
            this.keyType = keyType;
            this.keyPath = keyPath;
            this.keyPaths = keyPaths;
            this.keyData = keyData;
            this.signedIdentity = signedIdentity;
        }

        @Override
        public String getType() {
            return "signedBy";
        }

        @Override
        public boolean evaluate(String transport, String scope) {
            LOG.warn(
                    "Policy requirement 'signedBy' is not yet implemented for transport='{}' scope='{}' — accepting without verification",
                    transport,
                    scope);
            return true;
        }

        /**
         * Return the key type (currently always {@code "GPGKeys"}).
         *
         * @return the key type string, may be {@code null}.
         */
        public @Nullable String getKeyType() {
            return keyType;
        }

        /**
         * Return the path to a single GPG keyring file, or {@code null} if not set.
         *
         * @return the key path, may be {@code null}.
         */
        public @Nullable String getKeyPath() {
            return keyPath;
        }

        /**
         * Return the list of paths to GPG keyring files, or {@code null} if not set.
         *
         * @return the key paths, may be {@code null}.
         */
        public @Nullable List<String> getKeyPaths() {
            return keyPaths;
        }

        /**
         * Return the base64-encoded GPG keyring, or {@code null} if not set.
         *
         * @return the key data, may be {@code null}.
         */
        public @Nullable String getKeyData() {
            return keyData;
        }

        /**
         * Return the signed identity matching rules, or {@code null} to use the default
         * ({@code matchRepoDigestOrExact}).
         *
         * @return the signed identity, may be {@code null}.
         */
        public @Nullable SignedIdentity getSignedIdentity() {
            return signedIdentity;
        }
    }

    /**
     * Require a valid Sigstore/Cosign signature stored as an OCI artifact.
     *
     * <p>JSON example:
     * <pre>{@code
     * {
     *   "type": "sigstoreSigned",
     *   "keyPath": "/etc/pki/containers/cosign.pub",
     *   "signedIdentity": {"type": "matchRepoDigestOrExact"}
     * }
     * }</pre>
     *
     */
    @OrasModel
    public static final class SigstoreSigned extends PolicyRequirement {

        private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SigstoreSigned.class);

        private final @Nullable String keyPath;
        private final @Nullable List<String> keyPaths;
        private final @Nullable String keyData;
        private final @Nullable List<String> keyDatas;
        private final @Nullable SignedIdentity signedIdentity;

        /**
         * Creates a new {@link SigstoreSigned} requirement.
         *
         * @param keyPath        path to a Sigstore/Cosign public key (mutually exclusive with others).
         * @param keyPaths       paths to multiple public keys (mutually exclusive with others).
         * @param keyData        base64-encoded public key (mutually exclusive with others).
         * @param keyDatas       list of base64-encoded public keys (mutually exclusive with others).
         * @param signedIdentity identity matching rules; {@code null} defaults to matchRepoDigestOrExact.
         */
        @JsonCreator
        public SigstoreSigned(
                @JsonProperty("keyPath") @Nullable String keyPath,
                @JsonProperty("keyPaths") @Nullable List<String> keyPaths,
                @JsonProperty("keyData") @Nullable String keyData,
                @JsonProperty("keyDatas") @Nullable List<String> keyDatas,
                @JsonProperty("signedIdentity") @Nullable SignedIdentity signedIdentity) {
            this.keyPath = keyPath;
            this.keyPaths = keyPaths;
            this.keyData = keyData;
            this.keyDatas = keyDatas;
            this.signedIdentity = signedIdentity;
        }

        @Override
        public String getType() {
            return "sigstoreSigned";
        }

        @Override
        public boolean evaluate(String transport, String scope) {
            LOG.warn(
                    "Policy requirement 'sigstoreSigned' is not yet implemented for transport='{}' scope='{}' — accepting without verification",
                    transport,
                    scope);
            return true;
        }

        /**
         * Return the path to a Sigstore/Cosign public key file, or {@code null} if not set.
         *
         * @return the key path, may be {@code null}.
         */
        public @Nullable String getKeyPath() {
            return keyPath;
        }

        /**
         * Return the list of paths to public key files, or {@code null} if not set.
         *
         * @return the key paths, may be {@code null}.
         */
        public @Nullable List<String> getKeyPaths() {
            return keyPaths;
        }

        /**
         * Return the base64-encoded public key, or {@code null} if not set.
         *
         * @return the key data, may be {@code null}.
         */
        public @Nullable String getKeyData() {
            return keyData;
        }

        /**
         * Return the list of base64-encoded public keys, or {@code null} if not set.
         *
         * @return the key datas, may be {@code null}.
         */
        public @Nullable List<String> getKeyDatas() {
            return keyDatas;
        }

        /**
         * Return the signed identity matching rules, or {@code null} to use the default
         * ({@code matchRepoDigestOrExact}).
         *
         * @return the signed identity, may be {@code null}.
         */
        public @Nullable SignedIdentity getSignedIdentity() {
            return signedIdentity;
        }
    }
}
