package com.yogida.meditation.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT and OAuth2 resource server configuration.
 *
 * <p>The issuers previously defaulted to the production Keycloak URLs inside this record's
 * compact constructor. That meant a service started without the environment set would
 * quietly validate tokens against production instead of failing — the kind of default that
 * only ever surprises. Every required value is now validated, so a missing one stops
 * startup and names itself.
 *
 * <p>{@code issuer} must match Keycloak's {@code KC_HOSTNAME} exactly. Any difference,
 * including a trailing slash or {@code http} versus {@code https}, makes every token fail
 * validation with an error that points at the token rather than at the configuration.
 *
 * <p>{@code previousIssuer} and {@code previousAdminIssuer} exist for a change of Keycloak's
 * public URL, such as the move from {@code https://yogida.org/zxcasdqwe} to
 * {@code https://yogida.org/yogida/zxcasdqwe}. Keycloak mints the new {@code iss} from the moment
 * {@code KC_HOSTNAME} changes, while tokens issued before it stay valid until they expire. Set the
 * new URL as the issuer and the old one here, deploy, switch Keycloak over, and clear these once
 * the longest token lifetime has passed. The realm's signing keys do not depend on its URL, so
 * a previous issuer's tokens are verified against the same JWK set as the current one.
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(

    @NotBlank(message = "app.security.jwt.issuer must be set (APP_SECURITY_JWT_ISSUER)")
    String issuer,

    @NotBlank(message = "app.security.jwt.admin-issuer must be set (APP_SECURITY_JWT_ADMIN_ISSUER)")
    String adminIssuer,

    @NotBlank(message = "app.security.jwt.audience must be set (APP_SECURITY_JWT_AUDIENCE)")
    String audience,

    @NotBlank(message = "app.security.jwt.admin-client-id must be set")
    String adminClientId,

    @NotBlank(message = "app.security.jwt.client-id must be set")
    String clientId,

    /** Optional. Derived from {@link #issuer} when blank. */
    String jwkSetUri,

    /** Optional. Derived from {@link #adminIssuer} when blank. */
    String adminJwkSetUri,

    /** Optional. An earlier URL of the mobile realm, still accepted. Blank means none. */
    String previousIssuer,

    /** Optional. An earlier URL of the admin realm, still accepted. Blank means none. */
    String previousAdminIssuer
) {

    public String resolvedJwkSetUri() {
        return resolveJwkSetUri(issuer, jwkSetUri);
    }

    public String resolvedAdminJwkSetUri() {
        return resolveJwkSetUri(adminIssuer, adminJwkSetUri);
    }

    private static String resolveJwkSetUri(String issuer, String configuredJwkSetUri) {
        if (configuredJwkSetUri != null && !configuredJwkSetUri.isBlank()) {
            return configuredJwkSetUri;
        }
        String normalizedIssuer = issuer.endsWith("/")
            ? issuer.substring(0, issuer.length() - 1)
            : issuer;
        return normalizedIssuer + "/protocol/openid-connect/certs";
    }
}
