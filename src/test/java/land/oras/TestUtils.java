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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

/**
 * Several tests utils
 */
public final class TestUtils {

    /**
     * Create a registries.conf file in the given home directory with the given content.
     * @param homeDir the home directory where the .config/containers/registries.conf file will be created
     * @param content the content of the registries.conf file
     */
    public static void createRegistriesConfFile(Path homeDir, String content) {
        try {
            Files.createDirectory(homeDir.resolve(".config"));
            Files.createDirectory(homeDir.resolve(".config").resolve("containers"));
            Files.writeString(homeDir.resolve(".config").resolve("containers").resolve("registries.conf"), content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Execute the given action with the HOME environment variable set to the given home directory.
     * @param homeDir the home directory to set in the HOME environment variable
     * @param action the action to execute with the HOME environment variable set
     * @throws Exception if any exception occurs during the execution of the action
     */
    public static void withHome(Path homeDir, Runnable action) throws Exception, IOException {
        new EnvironmentVariables()
                .set("HOME", homeDir.toAbsolutePath().toString())
                .execute(() -> {
                    try {
                        action.run();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
