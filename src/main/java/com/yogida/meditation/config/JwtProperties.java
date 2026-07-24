package com.yogida.meditation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT and OAuth2 resource server configuration properties.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
    String issuer,
    String adminIssuer,
    String audience,
    String adminClientId,
    String clientId,
    String jwkSetUri,
    String adminJwkSetUri
) {
    /**
     * Creates a JwtProperties instance with default values.
     */
    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "https://auth.yogida.example";
        }
        if (adminIssuer == null || adminIssuer.isBlank()) {
            adminIssuer = "https://yogida.org/zxcasdqwe/realms/yogida-admin";
        }
        if (audience == null || audience.isBlank()) {
            audience = "yogida";
        }
        if (adminClientId == null || adminClientId.isBlank()) {
            adminClientId = "yogida-admin";
        }
        if (clientId == null || clientId.isBlank()) {
            clientId = "yogida";
        }
    }

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
