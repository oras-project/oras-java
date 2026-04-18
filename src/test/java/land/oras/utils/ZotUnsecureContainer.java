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

import org.jspecify.annotations.NullMarked;
import org.testcontainers.containers.wait.strategy.Wait;

@NullMarked
public class ZotUnsecureContainer extends ZotBaseContainer<ZotUnsecureContainer> {

    /**
     * Create a new unsecure Zot registry container (HTTP, no auth).
     */
    public ZotUnsecureContainer() {
        setWaitStrategy(Wait.forHttp("/v2/_catalog").forPort(ZOT_PORT).forStatusCode(200));

        // language=JSON
        String configJson =
                """
                {
                  "storage": { "rootDirectory": "/var/lib/registry" },
                  "http": {
                    "address": "0.0.0.0",
                    "port": %s
                  },
                  "extensions": {
                    "search": { "enable": true }
                  }
                }
                """
                        .formatted(ZOT_PORT);
        writeConfig(configJson);
    }
}
