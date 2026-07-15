package com.yogida.meditation.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.Collections;

/**
 * Test configuration that provides a mock JWT decoder to avoid OIDC discovery during tests.
 * This is loaded when the "test" profile is active.
 */
@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    /**
     * Provides a mock JwtDecoder for tests.
     * This decoder simply decodes JWT tokens without validating against a real issuer.
     * In a real test, you would mock or provide a valid JWT token.
     */
    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        return token -> {
            // Return a mock JWT with minimal claims
            // In tests, you can override this by providing a valid JWT token
            return Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", "test-user")
                .claim("aud", "test-audience")
                .claim("iss", "https://localhost:8080/auth/realms/test")
                .claim("iat", Instant.now().getEpochSecond())
                .claim("exp", Instant.now().getEpochSecond() + 3600)
                .claim("scope", "profile email")
                .claim("realm_access", Collections.singletonMap("roles", Collections.emptyList()))
                .build();
        };
    }
}


