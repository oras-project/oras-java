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

package land.oras.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import land.oras.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

/**
 * Test class of {@link RegistriesConf}.
 */
@Execution(ExecutionMode.SAME_THREAD)
class RegistriesConfTest {

    @TempDir
    private static Path homeDir;

    // language=toml
    public static final String HOME_REGISTRIES_CONF =
            """
            unqualified-search-registries = ["docker.io"]

            [aliases]
            "alpine"="docker.io/library/alpine"
            """;

    @BeforeAll
    static void init() {
        TestUtils.createRegistriesConfFile(homeDir, HOME_REGISTRIES_CONF);
    }

    @Test
    void shouldReadAlias() throws Exception {
        TestUtils.withHome(homeDir, () -> {
            RegistriesConf conf = RegistriesConf.newConf();
            assertNotNull(conf);
            // Use membership checks: /etc/containers/registries.conf may exist and contribute extra aliases
            assertTrue(conf.hasAlias("alpine"));
            assertEquals("docker.io/library/alpine", conf.getAliases().get("alpine"));
        });
    }

    @Test
    void shouldReadUnqualifiedSearchRegistriesFromHome() throws Exception {
        TestUtils.withHome(homeDir, () -> {
            RegistriesConf conf = RegistriesConf.newConf();
            assertNotNull(conf);
            // Use contains check: /etc/containers/registries.conf may exist and contribute extra registries
            assertTrue(conf.getUnqualifiedRegistries().contains("docker.io"));
        });
    }

    @Test
    void shouldLoadDropInConfFiles(@TempDir Path dropInHomeDir) throws Exception {
        // language=toml
        TestUtils.createRegistriesConfFile(dropInHomeDir, "unqualified-search-registries = [\"docker.io\"]");
        // language=toml
        TestUtils.createDropInConfFile(
                dropInHomeDir,
                "10-extra.conf",
                """
                [aliases]
                "myapp"="registry.example.com/myapp"
                """);

        TestUtils.withHome(dropInHomeDir, () -> {
            RegistriesConf conf = RegistriesConf.newConf();
            assertNotNull(conf);
            // Use contains check: /etc/containers/registries.conf may exist and contribute extra registries
            assertTrue(conf.getUnqualifiedRegistries().contains("docker.io"));
            assertTrue(conf.hasAlias("myapp"));
            assertEquals("registry.example.com/myapp", conf.getAliases().get("myapp"));
        });
    }

    @Test
    void shouldLoadDropInConfFilesInAlphaNumericalOrder(@TempDir Path dropInHomeDir) throws Exception {
        TestUtils.createRegistriesConfFile(dropInHomeDir, "");
        // language=toml
        TestUtils.createDropInConfFile(
                dropInHomeDir,
                "01-first.conf",
                """
                [aliases]
                "foo"="registry.first.com/foo"
                """);
        // language=toml
        TestUtils.createDropInConfFile(
                dropInHomeDir,
                "02-second.conf",
                """
                [aliases]
                "foo"="registry.second.com/foo"
                """);

        TestUtils.withHome(dropInHomeDir, () -> {
            RegistriesConf conf = RegistriesConf.newConf();
            // 02-second.conf is loaded after 01-first.conf so its alias wins (last-wins)
            assertEquals("registry.second.com/foo", conf.getAliases().get("foo"));
        });
    }

    @Test
    void shouldFallBackToGlobalPathWhenHomeIsAbsent() throws Exception {
        synchronized (TestUtils.class) {
            new EnvironmentVariables().remove("HOME").execute(() -> {
                RegistriesConf conf = RegistriesConf.newConf();
                assertNotNull(conf);
            });
        }
    }
}
