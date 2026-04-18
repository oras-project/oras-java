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

import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.util.List;
import javax.net.ssl.SSLHandshakeException;
import land.oras.exception.OrasException;
import land.oras.utils.TlsUtils;
import land.oras.utils.ZotTlsContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Execution(ExecutionMode.CONCURRENT)
class RegistryTlsTest {

    private static final String UNRELATED_CA_PEM;

    static {
        KeyPair keyPair = TlsUtils.generateKeyPair();
        UNRELATED_CA_PEM = TlsUtils.toPem(TlsUtils.generateCaCertificate("unrelated-ca", keyPair));
    }

    @Container
    private final ZotTlsContainer tlsRegistry = new ZotTlsContainer().withStartupAttempts(3);

    @BeforeEach
    void before() {
        tlsRegistry.withFollowOutput();
    }

    @Test
    void shouldConnectWithCaFile() {
        Registry registry = Registry.builder()
                .withRegistry(tlsRegistry.getRegistry())
                .withCaFile(tlsRegistry.getCaCertPath())
                .build();

        List<String> repositories = registry.getRepositories().repositories();
        assertNotNull(repositories);
    }

    @Test
    void shouldConnectWithCaContent() {
        Registry registry = Registry.builder()
                .withRegistry(tlsRegistry.getRegistry())
                .withCaContent(tlsRegistry.getCaCertContent())
                .build();

        List<String> repositories = registry.getRepositories().repositories();
        assertNotNull(repositories);
    }

    @Test
    void shouldFailWithoutCaCertificate() {
        Registry registry =
                Registry.builder().withRegistry(tlsRegistry.getRegistry()).build();

        OrasException exception = assertThrows(OrasException.class, registry::getRepositories);
        assertInstanceOf(SSLHandshakeException.class, exception.getCause());
    }

    @Test
    void shouldFailWithWrongCaCertificate() {
        Registry registry = Registry.builder()
                .withRegistry(tlsRegistry.getRegistry())
                .withCaContent(UNRELATED_CA_PEM)
                .build();

        OrasException exception = assertThrows(OrasException.class, registry::getRepositories);
        assertInstanceOf(SSLHandshakeException.class, exception.getCause());
    }

    @Test
    void shouldConnectWithSkipTlsVerify() {
        Registry registry = Registry.builder()
                .withRegistry(tlsRegistry.getRegistry())
                .withSkipTlsVerify(true)
                .build();

        List<String> repositories = registry.getRepositories().repositories();
        assertNotNull(repositories);
    }
}
