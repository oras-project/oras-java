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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import land.oras.exception.OrasException;
import land.oras.utils.TomlUtils;
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

        // Take the first path found: https://github.com/containers/image/blob/main/docs/containers-registries.conf.5.md
        for (Path configPath : configPaths) {
            LOG.debug("Checking for registries config file at: {}", configPath);
            if (Files.exists(configPath)) {
                ConfigFile configFile = TomlUtils.fromToml(configPath, ConfigFile.class);
                LOG.debug("Loaded registries config file: {}", configPath);
                return new RegistriesConf(Config.load(configFile));
            }
        }
        // Empty config
        return new RegistriesConf(new Config());
    }

    /**
     * Create a new RegistriesConf instance by loading configuration from standard paths.
     * @return A new RegistriesConf instance.
     */
    public static RegistriesConf newConf() {
        Path globalPath = Path.of("/etc/containers/registries.conf");
        List<Path> paths = List.of(
                System.getenv("HOME") != null
                        ? Path.of(System.getenv("HOME"), ".config", "containers", "registries.conf")
                        : globalPath,
                globalPath);
        return newConf(paths);
    }

    /**
     * The model of the config file
     *
     */
    record ConfigFile(
            @JsonProperty("aliases") @Nullable Map<String, String> aliases,
            @JsonProperty("unqualified-search-registries") @Nullable List<String> unqualifiedRegistries) {}

    /**
     * Get the list of unqualified registries.
     * @return an unmodifiable list of unqualified registries.
     */
    public List<String> getUnqualifiedRegistries() {
        return Collections.unmodifiableList(config.unqualifiedRegistries);
    }

    /**
     * Return the aliases
     * @return an unmodifiable map of aliases, where the key is the alias and the value is the actual registry URL.
     */
    public Map<String, String> getAliases() {
        return Collections.unmodifiableMap(config.aliases);
    }

    /**
     * Check if the given alias exists in the configuration.
     * @param alias the alias to check for existence.
     * @return true if the alias exists, false otherwise.
     */
    public boolean hasAlias(String alias) {
        return config.aliases.containsKey(alias);
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
         * Map of registry aliases, where the key is the alias and the value is the actual registry URL.
         */
        private final Map<String, String> aliases = new HashMap<>();

        /**
         * Loads the configuration from a TOML file at the specified path and populates registries configuration
         *
         * @param configFile The config file
         * @return A {@code Config} object populated with config
         * @throws OrasException If an error occurs while reading or parsing the TOML file.
         */
        public static Config load(ConfigFile configFile) throws OrasException {
            Config config = new Config();
            if (configFile.unqualifiedRegistries != null) {
                LOG.trace("Loading unqualified registries: {}", configFile.unqualifiedRegistries);
                config.unqualifiedRegistries.addAll(configFile.unqualifiedRegistries);
            }
            if (configFile.aliases != null) {
                LOG.trace("Loading registry aliases: {}", configFile.aliases);
                config.aliases.putAll(configFile.aliases);
            }
            return config;
        }
    }
}
