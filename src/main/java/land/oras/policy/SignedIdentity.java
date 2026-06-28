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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Controls how the image identity claimed inside a signature must match the reference being pulled.
 */
@NullMarked
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SignedIdentity.MatchExact.class, name = "matchExact"),
    @JsonSubTypes.Type(value = SignedIdentity.MatchRepoDigestOrExact.class, name = "matchRepoDigestOrExact"),
    @JsonSubTypes.Type(value = SignedIdentity.MatchRepository.class, name = "matchRepository"),
    @JsonSubTypes.Type(value = SignedIdentity.ExactReference.class, name = "exactReference"),
    @JsonSubTypes.Type(value = SignedIdentity.ExactRepository.class, name = "exactRepository"),
    @JsonSubTypes.Type(value = SignedIdentity.RemapIdentity.class, name = "remapIdentity"),
})
public abstract sealed class SignedIdentity
        permits SignedIdentity.MatchExact,
                SignedIdentity.MatchRepoDigestOrExact,
                SignedIdentity.MatchRepository,
                SignedIdentity.ExactReference,
                SignedIdentity.ExactRepository,
                SignedIdentity.RemapIdentity {

    /**
     * Constructor
     */
    SignedIdentity() {}

    /**
     * Return the type name as it appears in the policy JSON.
     *
     * @return the type string, e.g. {@code "matchRepoDigestOrExact"}.
     */
    public abstract String getType();

    /**
     * The signature's claimed Docker reference must exactly equal the reference being pulled,
     * including tag or digest. Digest-based pulls using an image that was only signed with a tag
     * reference will fail.
     *
     * <p>JSON example: {@code {"type": "matchExact"}}
     */
    public static final class MatchExact extends SignedIdentity {

        /** Creates a new {@link MatchExact} identity. */
        public MatchExact() {}

        @Override
        public String getType() {
            return "matchExact";
        }
    }

    /**
     * Default identity matching behaviour when no {@code signedIdentity} is specified.
     *
     * <ul>
     *   <li>Tag-based pull: the claimed reference must exactly equal the pulled reference.</li>
     *   <li>Digest-based pull: only the repository (registry + path, without tag/digest) needs to
     *       match.</li>
     * </ul>
     */
    public static final class MatchRepoDigestOrExact extends SignedIdentity {

        /**
         * Constructor
         */
        public MatchRepoDigestOrExact() {}

        @Override
        public String getType() {
            return "matchRepoDigestOrExact";
        }
    }

    /**
     * Only the repository path (registry host + image path, without any tag or digest) of the
     * claimed reference needs to match. Allows pulling {@code :latest} when the image was signed
     * with a versioned tag.
     */
    public static final class MatchRepository extends SignedIdentity {

        /**
         * Constructor
         */
        public MatchRepository() {}

        @Override
        public String getType() {
            return "matchRepository";
        }
    }

    /**
     * The signature must claim a specific, hardcoded Docker reference. Used when pulling from a
     * mirror that stores images under a different name.
     *
     * <p>JSON example:
     * <pre>{@code {"type": "exactReference", "dockerReference": "registry.example.com/nginx:latest"}}</pre>
     */
    public static final class ExactReference extends SignedIdentity {

        private final @Nullable String dockerReference;

        /**
         * Creates a new {@link ExactReference} identity.
         *
         * @param dockerReference the exact Docker reference the signature must claim.
         */
        @JsonCreator
        public ExactReference(@JsonProperty("dockerReference") @Nullable String dockerReference) {
            this.dockerReference = dockerReference;
        }

        @Override
        public String getType() {
            return "exactReference";
        }

        /**
         * Return the exact Docker reference that the signature must claim.
         *
         * @return the docker reference, may be {@code null} if not configured.
         */
        public @Nullable String getDockerReference() {
            return dockerReference;
        }
    }

    /**
     * The signature must claim an image whose repository matches the given value. The tag or digest
     * in the claimed reference is ignored.
     *
     * <p>JSON example:
     * <pre>{@code {"type": "exactRepository", "dockerRepository": "registry.example.com/nginx"}}</pre>
     */
    public static final class ExactRepository extends SignedIdentity {

        private final @Nullable String dockerRepository;

        /**
         * Creates a new {@link ExactRepository} identity.
         *
         * @param dockerRepository the repository the signature must claim.
         */
        @JsonCreator
        public ExactRepository(@JsonProperty("dockerRepository") @Nullable String dockerRepository) {
            this.dockerRepository = dockerRepository;
        }

        @Override
        public String getType() {
            return "exactRepository";
        }

        /**
         * Return the Docker repository that the signature must claim.
         *
         * @return the docker repository, may be {@code null} if not configured.
         */
        public @Nullable String getDockerRepository() {
            return dockerRepository;
        }
    }

    /**
     * If the image reference starts with {@code prefix}, rewrite it to {@code signedPrefix} before
     * applying {@code matchRepoDigestOrExact} matching rules. Used when a registry mirrors images
     * from another location and signatures were created against the original location.
     *
     * <p>JSON example:
     * <pre>{@code
     * {
     *   "type": "remapIdentity",
     *   "prefix": "mirror.example.com/proxy",
     *   "signedPrefix": "docker.io/library"
     * }
     * }</pre>
     */
    public static final class RemapIdentity extends SignedIdentity {

        private final @Nullable String prefix;
        private final @Nullable String signedPrefix;

        /**
         * Creates a new {@link RemapIdentity} signed identity.
         *
         * @param prefix       the prefix to match against the pulled image reference.
         * @param signedPrefix the replacement prefix used when evaluating the signature identity.
         */
        @JsonCreator
        public RemapIdentity(
                @JsonProperty("prefix") @Nullable String prefix,
                @JsonProperty("signedPrefix") @Nullable String signedPrefix) {
            this.prefix = prefix;
            this.signedPrefix = signedPrefix;
        }

        @Override
        public String getType() {
            return "remapIdentity";
        }

        /**
         * Return the prefix to match against the pulled image reference.
         *
         * @return the prefix, may be {@code null} if not configured.
         */
        public @Nullable String getPrefix() {
            return prefix;
        }

        /**
         * Return the signed prefix used to rewrite the image identity before matching.
         *
         * @return the signed prefix, may be {@code null} if not configured.
         */
        public @Nullable String getSignedPrefix() {
            return signedPrefix;
        }
    }
}
