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
    String clientId
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
}
