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
import org.jspecify.annotations.NullMarked;
import org.testcontainers.containers.wait.strategy.Wait;

@NullMarked
public class ZotContainer extends ZotBaseContainer<ZotContainer> {

    // myuser:mypass
    public static final String AUTH_STRING = "myuser:$2y$05$M1VYs6EzFkXBmuS.BrIreObAnJcWCgzSPeT9/Rh3aVEqTqtSL8XN.";

    /**
     * Create a new Zot registry container with htpasswd authentication.
     */
    public ZotContainer() {
        setWaitStrategy(Wait.forHttp("/v2/_catalog").forPort(ZOT_PORT).forStatusCode(401));

        try {
            // Auth file
            Path authFile = Files.createTempFile("auth", ".htpasswd");
            Files.writeString(authFile, AUTH_STRING);
            copyFileToContainer(authFile, "/etc/zot/auth.htpasswd");

            // language=JSON
            String configJson =
                    """
                    {
                      "storage": { "rootDirectory": "/var/lib/registry" },
                      "http": {
                        "address": "0.0.0.0",
                        "port": %s,
                        "auth": {
                          "htpasswd": { "path": "/etc/zot/auth.htpasswd" }
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
            throw new RuntimeException("Failed to create auth.htpasswd", e);
        }
    }
}
