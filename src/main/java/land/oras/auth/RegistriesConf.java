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

package land.oras.auth;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import land.oras.exception.OrasException;
import land.oras.utils.JsonUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle registries.conf configuration
 */
@NullMarked
public class RegistriesConf {

    private static final Logger LOG = LoggerFactory.getLogger(RegistriesConf.class);

    /**
     * The internal config
     */
    private final Config config;

    /**
     * Constructor for RegistriesConf.
     *
     * @param config configuration instance.
     */
    RegistriesConf(Config config) {
        this.config = Objects.requireNonNull(config, "Config cannot be null");
    }

    /**
     * Create a new RegistriesConf instance by loading configuration from the specified paths.
     * @param configPaths The list of paths to load configuration from.
     * @return A new RegistriesConf instance.
     */
    public static RegistriesConf newConf(List<Path> configPaths) {
        List<ConfigFile> files = new ArrayList<>();
        for (Path configPath : configPaths) {
            if (Files.exists(configPath)) {
                ConfigFile configFile = JsonUtils.fromJson(configPath, ConfigFile.class);
                LOG.debug("Loaded registries config file: {}", configPath);
                files.add(configFile);
            }
        }
        return new RegistriesConf(Config.load(files));
    }

    /**
     * Create a new RegistriesConf instance by loading configuration from standard paths.
     * @return A new RegistriesConf instance.
     */
    public static RegistriesConf newConf() {
        Path globalPath = Path.of("/etc/containers/registries.conf");
        List<Path> paths = List.of(
                globalPath,
                System.getenv("HOME") != null
                        ? Path.of(System.getenv("HOME"), ".config", "containers", "registries.conf")
                        : globalPath);
        return newConf(paths);
    }

    /**
     * The model of the config file
     *
     */
    record ConfigFile(@Nullable List<String> unqualifiedRegistries) {}

    /**
     * Get the list of unclassified registries.
     * @return an unmodifiable list of unclassified registries.
     */
    public List<String> getUnclassifiedRegistries() {
        return Collections.unmodifiableList(config.unqualifiedRegistries);
    }

    /**
     * Nested Config class for configuration management.
     */
    static class Config {

        /**
         * Private constructor to prevent instantiation.
         */
        private Config() {}

        /**
         * List of unqualified registries.
         */
        private final List<String> unqualifiedRegistries = new LinkedList<>();

        /**
         * Loads the configuration from a JSON file at the specified path and populates registries configuration
         *
         * @param configFiles The config files
         * @return A {@code Config} object populated with config
         * @throws OrasException If an error occurs while reading or parsing the TOML file.
         */
        public static Config load(List<ConfigFile> configFiles) throws OrasException {
            Config config = new Config();
            for (ConfigFile configFile : configFiles) {
                if (configFile.unqualifiedRegistries != null) {
                    config.unqualifiedRegistries.addAll(configFile.unqualifiedRegistries);
                }
            }
            return config;
        }
    }
}
