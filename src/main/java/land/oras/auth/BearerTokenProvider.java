package land.oras.auth;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import land.oras.utils.JsonUtils;
import land.oras.utils.OrasHttpClient;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A provider for bearer token authentication
 */
@NullMarked
public class BearerTokenProvider implements AuthProvider {

    /**
     * The pattern for the WWW-Authenticate header value
     */
    private static final Pattern WWW_AUTH_VALUE_PATTERN =
            Pattern.compile("Bearer realm=\"([^\"]+)\",service=\"([^\"]+)\",scope=\"([^\"]+)\"(,error=\"([^\"]+)\")?");

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(BearerTokenProvider.class);

    /**
     * The refreshed token
     */
    private @Nullable TokenResponse token;

    /**
     * The provider for username and password in case of refresh token done
     */
    private final AbstractUsernamePasswordProvider provider;

    /**
     * Create a new bearer token provider
     * @param provider The provider for username and password
     */
    public BearerTokenProvider(AbstractUsernamePasswordProvider provider) {
        this.provider = provider;
    }

    /**
     * Retrieve
     * @param response The response
     * @return The token
     */
    public BearerTokenProvider refreshToken(OrasHttpClient.ResponseWrapper<String> response) {

        String wwwAuthHeader = response.headers().getOrDefault(Const.WWW_AUTHENTICATE_HEADER.toLowerCase(), "");
        LOG.debug("WWW-Authenticate header: {}", wwwAuthHeader);
        if (wwwAuthHeader.isEmpty()) {
            throw new OrasException("No WWW-Authenticate header found in response");
        }

        Matcher matcher = WWW_AUTH_VALUE_PATTERN.matcher(wwwAuthHeader);
        if (!matcher.matches()) {
            throw new OrasException("Invalid WWW-Authenticate header value: " + wwwAuthHeader);
        }

        // Extract parts
        String realm = matcher.group(1);
        String service = matcher.group(2);
        String scope = matcher.group(3);
        String error = matcher.group(5);

        LOG.debug("WWW-Authenticate header: realm={}, service={}, scope={}, error={}", realm, service, scope, error);

        URI uri = URI.create(realm + "?scope=" + scope + "&service=" + service);

        // Perform the request to get the token
        OrasHttpClient httpClient =
                OrasHttpClient.Builder.builder().withAuthentication(provider).build();
        OrasHttpClient.ResponseWrapper<String> responseWrapper =
                httpClient.get(uri, Map.of(Const.AUTHORIZATION_HEADER, provider.getAuthHeader()));

        // Log the response
        LOG.debug(
                "Response: {}",
                responseWrapper
                        .response()
                        .replaceAll("\"token\"\\s*:\\s*\"([A-Za-z0-9\\-_\\.]+)\"", "\"token\":\"<redacted>\"")
                        .replaceAll(
                                "\"access_token\"\\s*:\\s*\"([A-Za-z0-9\\-_\\.]+)\"",
                                "\"access_token\":\"<redacted>\""));
        LOG.debug(
                "Headers: {}",
                responseWrapper.headers().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> Const.AUTHORIZATION_HEADER.equalsIgnoreCase(entry.getKey())
                                        ? "<redacted" // Replace value with ****
                                        : entry.getValue())));

        this.token = JsonUtils.fromJson(responseWrapper.response(), TokenResponse.class);
        return this;
    }

    /**
     * Get the token
     * @return The token
     */
    public @Nullable TokenResponse getToken() {
        return token;
    }

    @Override
    public String getAuthHeader() {
        if (token == null) {
            throw new OrasException("No token available");
        }
        return "Bearer " + token.token;
    }

    /**
     * The token response
     * @param token The token
     * @param access_token The access token
     * @param expire_in The expire in
     * @param issued_at The issued at
     */
    @NullMarked
    public record TokenResponse(String token, String access_token, Integer expire_in, ZonedDateTime issued_at) {}
}
