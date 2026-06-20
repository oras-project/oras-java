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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import land.oras.ContainerRef;
import land.oras.OrasModel;
import land.oras.Registry;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
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
     * If the {@code CONTAINERS_REGISTRIES_CONF} environment variable is set it is used exclusively.
     * Otherwise, the user-local config (under {@code $HOME}) is tried first, then {@code /etc/containers/registries.conf}.
     *
     * @return A new RegistriesConf instance.
     */
    public static RegistriesConf newConf() {
        String containersRegistriesConf = System.getenv("CONTAINERS_REGISTRIES_CONF");
        if (containersRegistriesConf != null) {
            LOG.debug("Using registries config from CONTAINERS_REGISTRIES_CONF: {}", containersRegistriesConf);
            return newConf(List.of(Path.of(containersRegistriesConf)));
        }
        return newConf(defaultConfPaths());
    }

    /**
     * Returns the ordered list of registries.conf paths to search when {@code CONTAINERS_REGISTRIES_CONF} is not set.
     * The user-local path (under {@code $HOME}) is tried first; the system-wide path is always included as fallback.
     *
     * @return list of candidate paths.
     */
    private static List<Path> defaultConfPaths() {
        Path globalPath = Path.of("/etc/containers/registries.conf");
        String home = System.getenv("HOME");
        if (home != null) {
            return List.of(Path.of(home, ".config", "containers", "registries.conf"), globalPath);
        }
        return List.of(globalPath);
    }

    /**
     * The model of a mirror entry within a [[registry]] table.
     * @param location The mirror registry location (host[:port][/path]).
     * @param insecure Whether the mirror is insecure.
     * @param pullFromMirror Controls which pull operations may use this mirror (default: {@link PullFromMirror#ALL}).
     */
    @OrasModel
    public record MirrorConfig(
            @Nullable @JsonProperty("location") String location,
            @Nullable @JsonProperty("insecure") Boolean insecure,
            @Nullable @JsonProperty("pull-from-mirror") PullFromMirror pullFromMirror) {
        /**
         * Return true if this mirror should be accessed over plain HTTP or with unverified TLS.
         * @return true if insecure
         */
        public boolean isInsecure() {
            return insecure != null && insecure;
        }

        /**
         * Return the effective pull-from-mirror setting, defaulting to {@link PullFromMirror#ALL}.
         * @return the pull-from-mirror setting
         */
        public PullFromMirror effectivePullFromMirror() {
            return pullFromMirror != null ? pullFromMirror : PullFromMirror.ALL;
        }
    }

    /**
     * The model of the registry configuration
     * @param prefix The prefix to match against container references.
     * @param location The registry location
     * @param blocked Whether the registry is blocked. If true, the registry is blocked and cannot be used for pulling or pushing images.
     * @param insecure Whether the registry is insecure. If true, the registry is considered insecure and may allow connections over HTTP or with invalid TLS certificates.
     * @param mirrorByDigestOnly If true, all mirrors for this registry are treated as digest-only (equivalent to setting pull-from-mirror=digest-only on every mirror).
     * @param mirrors Ordered list of mirror entries to try before the registry location.
     */
    @OrasModel
    record RegistryConfig(
            @Nullable @JsonProperty("prefix") String prefix,
            @Nullable @JsonProperty("location") String location,
            @Nullable @JsonProperty("blocked") Boolean blocked,
            @Nullable @JsonProperty("insecure") Boolean insecure,
            @Nullable @JsonProperty("mirror-by-digest-only") Boolean mirrorByDigestOnly,
            @Nullable @JsonProperty("mirror") List<MirrorConfig> mirrors) {
        /**
         * Return true if this registry is blocked and cannot be used for pulling or pushing images.
         * @return true if blocked
         */
        public boolean isBlocked() {
            return blocked != null && blocked;
        }

        /**
         * Return true if this registry should be accessed over plain HTTP or with unverified TLS.
         * @return true if insecure
         */
        public boolean isInsecure() {
            return insecure != null && insecure;
        }

        /**
         * Return true if all mirrors for this registry should only be used when the reference includes a digest.
         * @return true if mirror-by-digest-only is set
         */
        public boolean isMirrorByDigestOnly() {
            return mirrorByDigestOnly != null && mirrorByDigestOnly;
        }
    }

    /**
     * The model of the parsed prefix, which contains the host and path components of the prefix.
     * @param host The host component of the prefix, which can be a specific hostname or a wildcard pattern (e.g., *.example.com).
     * @param path The path component of the prefix, which can be a specific path or a path prefix (e.g., namespace/repo).
     */
    @OrasModel
    record ParsedPrefix(String host, String path) {

        static ParsedPrefix parse(String prefix) {
            int slash = prefix.indexOf('/');
            if (slash < 0) {
                return new ParsedPrefix(prefix, "");
            }
            return new ParsedPrefix(prefix.substring(0, slash), prefix.substring(slash + 1));
        }
    }

    /**
     * The for handling short name
     */
    @OrasModel
    enum ShortNameMode {

        /**
         * Use all unqualified-search registries without any restriction
         */
        DISABLED("disabled"),

        /**
         * If only one unqualified-search registry is set, use it as there is no ambiguity.
         * If there is more than one registry this throw an error (default)
         */
        ENFORCING("enforcing"),

        /**
         * Same as enforcing for ORAS Java SDK
         */
        PERMISSIVE("permissive");

        ShortNameMode(String value) {
            this.value = value;
        }

        @JsonCreator
        public static ShortNameMode fromString(String key) {
            return ShortNameMode.valueOf(key.toUpperCase());
        }

        @JsonValue
        public String getKey() {
            return value;
        }

        private String value;
    }

    /**
     * Controls which pull operations may use a mirror.
     * <ul>
     *   <li>{@link #ALL} – the mirror is used for all pull operations (default)</li>
     *   <li>{@link #DIGEST_ONLY} – mirror is only used when the reference includes a digest</li>
     *   <li>{@link #TAG_ONLY} – mirror is only used when the reference includes a tag</li>
     * </ul>
     */
    @OrasModel
    public enum PullFromMirror {

        /** Use this mirror for all pull operations (default). */
        ALL("all"),

        /** Use this mirror only when the image reference includes a digest. */
        DIGEST_ONLY("digest-only"),

        /** Use this mirror only when the image reference includes a tag. */
        TAG_ONLY("tag-only");

        PullFromMirror(String value) {
            this.value = value;
        }

        /**
         * Deserialize from the TOML string value (e.g. {@code "digest-only"}).
         * @param key the string value from the configuration file.
         * @return the matching enum constant.
         */
        @JsonCreator
        public static PullFromMirror fromString(String key) {
            for (PullFromMirror v : values()) {
                if (v.value.equalsIgnoreCase(key)) return v;
            }
            throw new IllegalArgumentException("Unknown pull-from-mirror value: " + key);
        }

        /**
         * Return the TOML string value for this constant.
         * @return the string value.
         */
        @JsonValue
        public String getKey() {
            return value;
        }

        private final String value;
    }

    /**
     * The model of the configuration file, which contains the list of registry configurations, aliases, and unqualified registries.
     * @param registries The list of registry configurations, each containing the registry location, whether it is blocked, and whether it is insecure.
     * @param aliases The map of registry aliases, where the key is the alias and the value is the actual registry URL.
     * @param unqualifiedRegistries The list of unqualified registries, which are registries that can be used without specifying a registry.
     */
    @OrasModel
    record ConfigFile(
            @JsonProperty("short-name-mode") @Nullable ShortNameMode shortNameMode,
            @JsonProperty("registry") @Nullable List<RegistryConfig> registries,
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
     * Return the key of the alias
     * @param ref the container reference to get the alias key for.
     * @return the alias key for the given container reference, which is either the repository name
     */
    public String getAliasKey(ContainerRef ref) {
        return ref.getRegistry().equals(Const.DEFAULT_REGISTRY) && "library".equals(ref.getNamespace())
                ? ref.getRepository()
                : ref.getFullRepository();
    }

    /**
     * Enforce the short name mode by checking the configuration. If the short name mode is set to ENFORCING or PERMISSIVE and there are multiple unqualified registries configured, this method throws an OrasException indicating that the configuration is invalid. If the configuration is valid, this method does nothing.
     * @throws OrasException if the short name mode is set to ENFORCING or PERMISSIVE and there are multiple unqualified registries configured, indicating that the configuration is invalid.
     */
    public void enforceShortNameMode() throws OrasException {
        if ((config.shortNameMode == ShortNameMode.ENFORCING || config.shortNameMode == ShortNameMode.PERMISSIVE)
                && config.unqualifiedRegistries.size() > 1) {
            throw new OrasException(
                    "Short name mode is set to ENFORCING/PERMISSION but multiple unqualified registries are configured: "
                            + config.unqualifiedRegistries);
        }
        LOG.debug(
                "Short name mode '{}' is valid with unqualified registries: {}",
                config.shortNameMode,
                config.unqualifiedRegistries);
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
     * Check if the given registry is marked as blocked in the configuration.
     * @param location the registry location to check for blocking.
     * @return true if the registry is marked as blocked, false otherwise.
     */
    public boolean isBlocked(ContainerRef location) {
        return selectMatchingTable(location).map(RegistryConfig::isBlocked).orElse(false);
    }

    /**
     * Check if the given registry is marked as insecure in the configuration.
     * If no entry found, we fall back to Registry configuration
     * If the entry is found we use the insecure flag (or default true)
     * @param registry The registry object
     * @param location the registry location to check for insecurity.
     * @return true if the registry is marked as insecure, false otherwise.
     */
    public boolean isInsecure(Registry registry, ContainerRef location) {
        return selectMatchingTable(location).map(RegistryConfig::isInsecure).orElse(registry.isInsecure());
    }

    /**
     * Rewrite the given container reference according to the matching registry configuration.
     * @param ref the container reference to rewrite.
     * @return the rewritten container reference.
     */
    public ContainerRef rewrite(ContainerRef ref) {

        Optional<RegistryConfig> matchingConfig = selectMatchingTable(ref);
        if (matchingConfig.isEmpty()) {
            return ref;
        }
        // No rewrite possible if location and prefix are not set
        String location = matchingConfig.get().location();
        String prefix = matchingConfig.get().prefix();
        if (location == null || location.isBlank() || prefix == null || prefix.isBlank()) {
            return ref;
        }
        String currentRefString = ref.toString();
        String rewrittenRefString;

        // Replace all subdomain if prefix starts with "*." (e.g., *.example.com → my-registry.com)
        if (prefix.startsWith("*.")) {

            // The subdomain replacement can include an optional path
            int firtSlashIndex = prefix.indexOf('/');
            String prefixPath = firtSlashIndex < 0 ? "" : prefix.substring(firtSlashIndex);

            // Remove matched host + optional prefixPath
            String remainder = currentRefString.substring(ref.getRegistry().length());
            if (!prefixPath.isEmpty() && remainder.startsWith(prefixPath)) {
                remainder = remainder.substring(prefixPath.length());
            }

            rewrittenRefString = location + remainder;
        } else {

            // For unqualified references, we need to construct the rewritten reference properly
            // by replacing the registry component and preserving the namespace/repository/tag structure
            if (ref.isUnqualified()) {
                // For unqualified references, build the new reference with the location as the new registry
                String namespace = ref.getNamespace();
                String repository = ref.getRepository();
                String tag = ref.getTag();
                String digest = ref.getDigest();

                StringBuilder rewritten = new StringBuilder(location);
                if (namespace != null && !namespace.isEmpty()) {
                    rewritten.append("/").append(namespace);
                }
                rewritten.append("/").append(repository);
                rewritten.append(":").append(tag);
                if (digest != null && !digest.isEmpty()) {
                    rewritten.append("@").append(digest);
                }
                rewrittenRefString = rewritten.toString();
            }

            // For qualified references, use the original string manipulation approach
            else {
                rewrittenRefString = location + currentRefString.substring(prefix.length());
            }
        }

        LOG.debug(
                "Rewriting container reference from '{}' to '{}' using registry config with prefix '{}' and location '{}'",
                currentRefString,
                rewrittenRefString,
                prefix,
                location);
        return ContainerRef.parse(rewrittenRefString);
    }

    /**
     * Return the ordered list of mirrors configured for the registry that matches the given reference.
     * @param ref the container reference to look up mirrors for.
     * @return an unmodifiable list of mirror configs (may be empty).
     */
    public List<MirrorConfig> getMirrors(ContainerRef ref) {
        Optional<RegistryConfig> matchingConfig = selectMatchingTable(ref);
        if (matchingConfig.isEmpty() || matchingConfig.get().mirrors() == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(matchingConfig.get().mirrors());
    }

    /**
     * Return the ordered list of mirrors that are applicable for the given reference, filtering out mirrors
     * whose {@code pull-from-mirror} setting does not match the reference type (tag vs. digest).
     * The registry-level {@code mirror-by-digest-only} flag, when true, overrides all per-mirror settings
     * and restricts every mirror to digest-only pulls.
     * @param ref the container reference to look up applicable mirrors for.
     * @return an unmodifiable list of applicable mirror configs (may be empty).
     */
    public List<MirrorConfig> getApplicableMirrors(ContainerRef ref) {
        Optional<RegistryConfig> matchingConfig = selectMatchingTable(ref);
        if (matchingConfig.isEmpty() || matchingConfig.get().mirrors() == null) {
            return Collections.emptyList();
        }
        boolean registryDigestOnly = matchingConfig.get().isMirrorByDigestOnly();
        boolean refHasDigest = ref.getDigest() != null && !ref.getDigest().isEmpty();
        boolean refHasTag = ref.getTag() != null && !ref.getTag().isEmpty();
        return matchingConfig.get().mirrors().stream()
                .filter(mirror -> {
                    PullFromMirror effective =
                            registryDigestOnly ? PullFromMirror.DIGEST_ONLY : mirror.effectivePullFromMirror();
                    return switch (effective) {
                        case DIGEST_ONLY -> refHasDigest;
                        case TAG_ONLY -> refHasTag;
                        case ALL -> true;
                    };
                })
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Rewrite the given container reference to use the mirror's location, replacing the registry host.
     * @param ref the original container reference.
     * @param mirror the mirror configuration to apply.
     * @return the rewritten reference pointing at the mirror.
     */
    public ContainerRef rewriteForMirror(ContainerRef ref, MirrorConfig mirror) {
        String mirrorLocation = mirror.location();
        if (mirrorLocation == null || mirrorLocation.isBlank()) {
            return ref;
        }
        // Strip trailing slashes to prevent double-slash segments in the rewritten ref
        mirrorLocation = mirrorLocation.replaceAll("/+$", "");
        if (mirrorLocation.isBlank()) {
            return ref;
        }
        // Always build from components to correctly handle:
        // - unqualified refs (toString() omits the registry)
        // - digest-only refs (tag is null, toString() would produce ":null")
        String namespace = ref.getNamespace();
        String repository = ref.getRepository();
        String tag = ref.getTag();
        String digest = ref.getDigest();
        StringBuilder sb = new StringBuilder(mirrorLocation);
        if (namespace != null && !namespace.isEmpty()) {
            sb.append("/").append(namespace);
        }
        sb.append("/").append(repository);
        if (tag != null && !tag.isEmpty()) {
            sb.append(":").append(tag);
        }
        if (digest != null && !digest.isEmpty()) {
            sb.append("@").append(digest);
        }
        String rewrittenRefString = sb.toString();
        LOG.debug("Rewriting '{}' to mirror '{}'", ref, rewrittenRefString);
        return ContainerRef.parse(rewrittenRefString);
    }

    /**
     * Select the matching registry configuration table for the container reference.
     * @param ref the container reference to find the matching registry configuration for.
     * @return an Optional containing the matching RegistryConfig if found, or an empty Optional if no matching configuration is found.
     */
    private Optional<RegistryConfig> selectMatchingTable(ContainerRef ref) {
        return config.registries.stream()
                .filter(cfg -> matches(ref, effectivePrefix(cfg)))
                .max(Comparator.comparingInt(cfg -> effectivePrefix(cfg).length()));
    }

    private @Nullable String effectivePrefix(RegistryConfig cfg) {
        return cfg.prefix() != null ? cfg.prefix() : cfg.location();
    }

    /**
     * Check if the given container reference matches the specified prefix.
     * @param ref the container reference to check for a match against the prefix.
     * @param prefix the prefix to match against the container reference, which can be a specific hostname or a wildcard pattern (e.g., *.example.com) and an optional path component (e.g., namespace/repo).
     * @return true if the container reference matches the prefix, false otherwise.
     */
    private boolean matches(ContainerRef ref, @Nullable String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return false;
        }

        ParsedPrefix p = ParsedPrefix.parse(prefix);

        // Host match (supports *.example.com)
        if (!hostMatches(ref.getRegistry(), p.host())) {
            return false;
        }

        // No path restriction → host-only match
        if (p.path().isEmpty()) {
            LOG.trace("Found registry table '{}'", p);
            return true;
        }

        // Path prefix match (namespace/repo)
        String refPath = String.join("/", ref.getNamespace()) + "/" + ref.getRepository();
        boolean result = refPath.equals(p.path()) || refPath.startsWith(p.path() + "/");
        if (result) {
            LOG.trace("Found registry table '{}' matching path '{}'", p, refPath);
        }
        return result;
    }

    /**
     * Check if the given host matches the specified prefix host, which can be a specific hostname or a wildcard pattern (e.g., *.example.com).
     * @param host the host to check for a match against the prefix host, which is the host component of the prefix.
     * @param prefixHost the prefix host to match against, which can be a specific hostname or a wildcard pattern (e.g., *.example.com).
     * @return true if the host matches the prefix host, false otherwise.
     */
    private boolean hostMatches(String host, String prefixHost) {
        if (prefixHost.startsWith("*.")) {
            String domain = prefixHost.substring(2);
            return host.endsWith("." + domain);
        }
        return host.equals(prefixHost);
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
         * Constructor for Config that takes a RegistryConfig and adds it to the list of registries.
         * @param registryConfigs The registry configuration to add to the list of registries.
         */
        Config(List<RegistryConfig> registryConfigs) {
            this.registries.addAll(registryConfigs);
        }

        /**
         * Default to enforcing
         */
        private ShortNameMode shortNameMode = ShortNameMode.ENFORCING;

        /**
         * List of unqualified registries.
         */
        private final List<String> unqualifiedRegistries = new LinkedList<>();

        /**
         * Map of registry aliases, where the key is the alias and the value is the actual registry URL.
         */
        private final Map<String, String> aliases = new HashMap<>();

        /**
         * List of registry configurations, each containing the registry location, whether it is blocked, and whether it is insecure.
         */
        private final List<RegistryConfig> registries = new LinkedList<>();

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
            if (configFile.registries != null) {
                LOG.trace("Loading registry configurations: {}", configFile.registries);
                config.registries.addAll(configFile.registries);
            }
            if (configFile.shortNameMode != null) {
                LOG.trace("Loading short name mode: {}", configFile.shortNameMode);
                config.shortNameMode = configFile.shortNameMode;
            }
            return config;
        }
    }
}
