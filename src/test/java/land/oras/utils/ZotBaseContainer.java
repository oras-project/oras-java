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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;

@NullMarked
public abstract class ZotBaseContainer<T extends ZotBaseContainer<T>> extends GenericContainer<T> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    static final int ZOT_PORT = 5000;

    protected ZotBaseContainer() {
        super(TestImages.ZOT);
        addExposedPort(ZOT_PORT);
    }

    /**
     * Get the registry host:port
     * @return The registry URL
     */
    public String getRegistry() {
        return getHost() + ":" + getMappedPort(ZOT_PORT);
    }

    @SuppressWarnings("unchecked")
    public T withFollowOutput() {
        followOutput(new Slf4jLogConsumer(log));
        return (T) this;
    }

    /**
     * Write a Zot config JSON to a temp file and copy it into the container.
     * @param configJson The JSON config content
     */
    protected void writeConfig(String configJson) {
        try {
            Path configFile = Files.createTempFile("zot-config", ".json");
            Files.writeString(configFile, configJson);
            withCopyFileToContainer(
                    MountableFile.forHostPath(configFile.toAbsolutePath().toString()), "/etc/zot/config.json");
        } catch (Exception e) {
            throw new RuntimeException("Failed to write Zot config", e);
        }
    }

    /**
     * Copy a host file into the container.
     * @param hostPath The path on the host
     * @param containerPath The path inside the container
     */
    protected void copyFileToContainer(Path hostPath, String containerPath) {
        withCopyFileToContainer(
                MountableFile.forHostPath(hostPath.toAbsolutePath().toString()), containerPath);
    }
}
