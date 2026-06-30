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

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import land.oras.utils.Const;

/**
 * Test support for Sigstore policy verification: generates cosign-style EC P-256 key pairs and signs
 * minimal Sigstore DSSE bundles using only the JDK's native cryptography (mirroring how
 * {@code cosign generate-key-pair} / {@code cosign sign --new-bundle-format} would).
 */
final class SigstoreTestSupport {

    private SigstoreTestSupport() {}

    /** A fixed image digest used across tests. */
    static final String IMAGE_DIGEST = "sha256:0c8c08023a23cfb81a65a895c0b70d41be8e54d52eda6a67c18487cc56ffd45d";

    /**
     * Generate an EC P-256 (secp256r1) key pair, the curve used by cosign.
     *
     * @return the generated key pair.
     */
    static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate EC key pair", e);
        }
    }

    /**
     * Encode a public key as a PEM {@code PUBLIC KEY} block, the format produced by
     * {@code cosign generate-key-pair}.
     *
     * @param key the public key.
     * @return the PEM document.
     */
    static String publicKeyPem(PublicKey key) {
        String b64 =
                Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(key.getEncoded());
        return """
                -----BEGIN PUBLIC KEY-----
                %s
                -----END PUBLIC KEY-----
                """
                .formatted(b64);
    }

    /**
     * Return the value of a policy {@code keyData} field for the given key: the base64-encoded
     * content of its PEM file.
     *
     * @param key the public key.
     * @return the base64-encoded PEM, suitable as a {@code keyData} value.
     */
    static String keyData(PublicKey key) {
        return Base64.getEncoder().encodeToString(publicKeyPem(key).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Build a Sigstore bundle (DSSE envelope over an in-toto statement) signed by the given private
     * key, binding the given image digest.
     *
     * @param privateKey  the private key to sign with.
     * @param imageDigest the full image digest, e.g. {@code "sha256:abc..."}.
     * @return the bundle JSON bytes, as they would appear as a bundle blob in the registry.
     */
    static byte[] signedBundle(PrivateKey privateKey, String imageDigest) {
        String hex = imageDigest.substring(imageDigest.indexOf(':') + 1);
        byte[] payload = inTotoPayload(hex).getBytes(StandardCharsets.UTF_8);
        byte[] pae = SigstoreVerifier.preAuthEncoding(Const.IN_TOTO_PAYLOAD_TYPE, payload);
        byte[] signature = sign(privateKey, pae);
        return bundleJson(payload, signature).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] sign(PrivateKey privateKey, byte[] content) {
        try {
            Signature signer = Signature.getInstance(Const.KEY_SHA256_ECDSA_SIGNATURE_ALGORITHM);
            signer.initSign(privateKey);
            signer.update(content);
            return signer.sign();
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign DSSE payload", e);
        }
    }

    private static String inTotoPayload(String imageHex) {
        // language=json
        return """
                {
                  "_type": "https://in-toto.io/Statement/v1",
                  "subject": [{"digest": {"sha256": "%s"}, "annotations": {}}],
                  "predicateType": "https://sigstore.dev/cosign/sign/v1",
                  "predicate": {}
                }"""
                .formatted(imageHex);
    }

    private static String bundleJson(byte[] payload, byte[] signature) {
        Base64.Encoder b64 = Base64.getEncoder();
        // language=json
        return """
                {
                  "mediaType": "%s",
                  "dsseEnvelope": {
                    "payload": "%s",
                    "payloadType": "%s",
                    "signatures": [{"sig": "%s"}]
                  }
                }"""
                .formatted(
                        Const.SIGSTORE_BUNDLE_MEDIA_TYPE,
                        b64.encodeToString(payload),
                        Const.IN_TOTO_PAYLOAD_TYPE,
                        b64.encodeToString(signature));
    }
}
