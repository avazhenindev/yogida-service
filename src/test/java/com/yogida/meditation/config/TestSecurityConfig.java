package com.yogida.meditation.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

import java.time.Instant;
import java.util.Collections;

/**
 * Test configuration that provides a mock authentication manager resolver to avoid
 * OIDC discovery during tests. This is loaded when the "test" profile is active.
 */
@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    /**
     * Provides a mock AuthenticationManagerResolver for tests.
     * It accepts any Bearer token via a fake JwtDecoder, bypassing real issuer
     * resolution and JWKS retrieval.
     */
    @Bean
    @Primary
    public AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver() {
        AuthenticationManager manager = new JwtAuthenticationProvider(testJwtDecoder())::authenticate;
        return request -> manager;
    }

    /**
     * A fake JwtDecoder that decodes any token into a JWT with minimal test claims.
     */
    private JwtDecoder testJwtDecoder() {
        return token -> Jwt.withTokenValue(token)
            .header("alg", "none")
            .claim("sub", "test-user")
            .claim("aud", "test-audience")
            .claim("iss", "https://localhost:8080/auth/realms/test")
            .claim("iat", Instant.now().getEpochSecond())
            .claim("exp", Instant.now().getEpochSecond() + 3600)
            .claim("scope", "profile email")
            .claim("realm_access", Collections.singletonMap("roles", Collections.emptyList()))
            .build();
    }
}


