/*-
 * =LICENSE=
 * ORAS Java SDK
 * ===
 * Copyright (C) 2024 - 2025 ORAS
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

@NullMarked
public class ZotContainer extends GenericContainer<ZotContainer> {

    /**
     * Logger
     */
    private Logger LOG = LoggerFactory.getLogger(ZotContainer.class);

    // myuser:mypass
    public static final String AUTH_STRING = "myuser:$2y$05$M1VYs6EzFkXBmuS.BrIreObAnJcWCgzSPeT9/Rh3aVEqTqtSL8XN.";
    public static final int ZOT_PORT = 5000;

    /**
     * Create a new registry container
     */
    public ZotContainer() {
        super("ghcr.io/project-zot/zot-linux-amd64:v2.1.10");
        addExposedPort(ZOT_PORT);
        setWaitStrategy(Wait.forHttp("/v2/_catalog").forPort(ZOT_PORT).forStatusCode(401));

        try {
            // Auth file
            Path authFile = Files.createTempFile("auth", ".htpasswd");
            Files.writeString(authFile, AUTH_STRING);

            // Zot config file
            Path configFile = Files.createTempFile("zot-config", ".json");
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

            Files.writeString(configFile, configJson);

            // Copy it into the container
            withCopyFileToContainer(
                    MountableFile.forHostPath(authFile.toAbsolutePath().toString()), "/etc/zot/auth.htpasswd");
            withCopyFileToContainer(
                    MountableFile.forHostPath(configFile.toAbsolutePath().toString()), "/etc/zot/config.json");

        } catch (Exception e) {
            throw new RuntimeException("Failed to create auth.htpasswd", e);
        }
    }

    /**
     * Get the registry URL
     * @return The registry URL
     */
    public String getRegistry() {
        return getHost() + ":" + getMappedPort(ZOT_PORT);
    }

    public ZotContainer withFollowOutput() {
        followOutput(new Slf4jLogConsumer(LOG));
        return this;
    }
}
