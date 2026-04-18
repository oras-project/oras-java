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

package land.oras.utils;

import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Utilities for generating TLS certificates and keys in tests.
 */
public final class TlsUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TlsUtils() {}

    /**
     * Generate an RSA key pair.
     * @return The generated key pair
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048, SECURE_RANDOM);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate key pair", e);
        }
    }

    /**
     * Generate a self-signed CA certificate.
     * @param cn The common name for the CA
     * @param keyPair The key pair to use
     * @return The self-signed CA certificate
     */
    public static X509Certificate generateCaCertificate(String cn, KeyPair keyPair) {
        try {
            X500Name subject = new X500Name("CN=" + cn);
            Instant now = Instant.now();
            Date notBefore = Date.from(now);
            Date notAfter = Date.from(now.plus(365, ChronoUnit.DAYS));

            ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
            X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                    subject, BigInteger.valueOf(now.toEpochMilli()), notBefore, notAfter, subject, keyPair.getPublic());
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));

            return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CA certificate", e);
        }
    }

    /**
     * Generate a certificate signed by a CA, with Subject Alternative Names for localhost.
     * @param cn The common name for the certificate
     * @param keyPair The key pair for the certificate
     * @param caCert The CA certificate used as issuer
     * @param caPrivateKey The CA private key used to sign
     * @return The signed certificate
     */
    public static X509Certificate generateSignedCertificate(
            String cn, KeyPair keyPair, X509Certificate caCert, PrivateKey caPrivateKey) {
        try {
            X500Name issuer = new X500Name(caCert.getSubjectX500Principal().getName());
            X500Name subject = new X500Name("CN=" + cn);
            Instant now = Instant.now();
            Date notBefore = Date.from(now);
            Date notAfter = Date.from(now.plus(365, ChronoUnit.DAYS));

            ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(caPrivateKey);
            X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                    issuer,
                    BigInteger.valueOf(now.toEpochMilli() + 1),
                    notBefore,
                    notAfter,
                    subject,
                    keyPair.getPublic());
            builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName[] {
                new GeneralName(GeneralName.dNSName, "localhost"), new GeneralName(GeneralName.iPAddress, "127.0.0.1")
            }));

            return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signed certificate", e);
        }
    }

    /**
     * Convert a security object (certificate, private key, etc.) to PEM format.
     * @param object The object to convert
     * @return The PEM-encoded string
     */
    public static String toPem(Object object) {
        try {
            StringWriter writer = new StringWriter();
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
                pemWriter.writeObject(object);
            }
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert to PEM", e);
        }
    }
}
