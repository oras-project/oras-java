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

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import org.jspecify.annotations.NullMarked;
import org.testcontainers.containers.wait.strategy.Wait;

@NullMarked
public class ZotTlsContainer extends ZotBaseContainer<ZotTlsContainer> {

    private final Path caCertPath;

    /**
     * Create a new TLS-enabled Zot registry container.
     * Generates a self-signed CA and server certificate at construction time.
     */
    public ZotTlsContainer() {
        setWaitStrategy(Wait.forHttps("/v2/_catalog")
                .forPort(ZOT_PORT)
                .forStatusCode(200)
                .allowInsecure());

        try {
            Path certDir = Files.createTempDirectory("zot-tls");
            caCertPath = certDir.resolve("ca.pem");
            Path serverCertPath = certDir.resolve("server.pem");
            Path serverKeyPath = certDir.resolve("server-key.pem");

            // Generate CA
            KeyPair caKeyPair = TlsUtils.generateKeyPair();
            X509Certificate caCert = TlsUtils.generateCaCertificate("test-zot-ca", caKeyPair);

            // Generate server certificate signed by CA
            KeyPair serverKeyPair = TlsUtils.generateKeyPair();
            X509Certificate serverCert =
                    TlsUtils.generateSignedCertificate("localhost", serverKeyPair, caCert, caKeyPair.getPrivate());

            // Write PEM files
            Files.writeString(caCertPath, TlsUtils.toPem(caCert));
            Files.writeString(serverCertPath, TlsUtils.toPem(serverCert));
            Files.writeString(serverKeyPath, TlsUtils.toPem(serverKeyPair.getPrivate()));

            copyFileToContainer(serverCertPath, "/etc/zot/server.pem");
            copyFileToContainer(serverKeyPath, "/etc/zot/server-key.pem");

            // language=JSON
            String configJson =
                    """
                    {
                      "storage": { "rootDirectory": "/var/lib/registry" },
                      "http": {
                        "address": "0.0.0.0",
                        "port": %s,
                        "tls": {
                          "cert": "/etc/zot/server.pem",
                          "key": "/etc/zot/server-key.pem"
                        }
                      },
                      "extensions": {
                        "search": { "enable": true }
                      }
                    }
                    """
                            .formatted(ZOT_PORT);
            writeConfig(configJson);

        } catch (Exception e) {
            throw new RuntimeException("Failed to set up TLS for Zot container", e);
        }
    }

    /**
     * Get the path to the CA certificate PEM file
     * @return The CA certificate path
     */
    public Path getCaCertPath() {
        return caCertPath;
    }

    /**
     * Get the CA certificate PEM content as a string
     * @return The CA certificate content
     */
    public String getCaCertContent() {
        try {
            return Files.readString(caCertPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read CA certificate", e);
        }
    }
}
