package com.yogida.meditation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT and OAuth2 resource server configuration properties.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
    String issuer,
    String audience
) {
    /**
     * Creates a JwtProperties instance with default values.
     */
    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "https://auth.yogida.example";
        }
        if (audience == null || audience.isBlank()) {
            audience = "yogida";
        }
    }
}
