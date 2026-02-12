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

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

/**
 * Test class of {@link RegistriesConf}.
 */
@Execution(ExecutionMode.CONCURRENT)
class RegistriesConfTest {

    @TempDir
    private static Path homeDir;

    // language=toml
    public static final String HOME_REGISTRIES_CONF =
            """
            unqualified-search-registries = ["docker.io"]
            """;

    @BeforeAll
    static void init() throws Exception {

        // Write home registries.conf on the temp home directory
        Files.createDirectory(homeDir.resolve(".config"));
        Files.createDirectory(homeDir.resolve(".config").resolve("containers"));
        Files.writeString(
                homeDir.resolve(".config").resolve("containers").resolve("registries.conf"), HOME_REGISTRIES_CONF);
    }

    @Test
    void shouldReadUnqualifiedSearchRegistriesFromHome() throws Exception {
        new EnvironmentVariables()
                .set("HOME", homeDir.toAbsolutePath().toString())
                .execute(() -> {
                    RegistriesConf conf = RegistriesConf.newConf();
                    assertNotNull(conf);
                    assertEquals(1, conf.getUnqualifiedRegistries().size());
                    assertEquals("docker.io", conf.getUnqualifiedRegistries().get(0));
                });
    }
}
