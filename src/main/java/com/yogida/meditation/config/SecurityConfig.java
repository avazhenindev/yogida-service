package com.yogida.meditation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Optional;

/**
 * Spring Security configuration for OAuth2 Resource Server with JWT validation.
 * Protects user-facing media endpoints while allowing public and admin endpoints.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    private final JwtProperties jwtProperties;
    private final Optional<CorsConfigurationSource> corsConfigurationSource;

    public SecurityConfig(JwtProperties jwtProperties, Optional<CorsConfigurationSource> corsConfigurationSource) {
        this.jwtProperties = jwtProperties;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    /**
     * Configures the security filter chain.
     * - Protects /api/media endpoints with JWT Bearer token validation
     * - Allows public access to /admin/* and /swagger-ui/* endpoints
     * - Uses stateless session management
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (corsConfigurationSource.isPresent()) {
            http.cors(cors -> cors.configurationSource(corsConfigurationSource.get()));
        }
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/admin/**").permitAll() // Admin endpoints managed separately
                // Protected endpoints - require JWT with Bearer token
                .requestMatchers("/api/media/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            );

        return http.build();
    }

    /**
     * Creates a JWT decoder that validates token signature and audience.
     * The decoder uses the issuer URI and validates the audience claim.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withIssuerLocation(jwtProperties.issuer())
            .build();

        // Set up validators: standard issuer/timestamp validation + custom audience validation
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(jwtProperties.issuer()),
            new AudienceValidator(jwtProperties.audience())
        ));

        return decoder;
    }

    /**
     * Custom OAuth2 token validator for audience (aud) claim.
     */
    private static class AudienceValidator implements org.springframework.security.oauth2.core.OAuth2TokenValidator<Jwt> {
        private final String expectedAudience;

        AudienceValidator(String expectedAudience) {
            this.expectedAudience = expectedAudience;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            Object aud = token.getClaim("aud");
            if (aud == null) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "Audience claim missing", null
                ));
            }

            // Check if audience claim contains the expected value
            // JWT RFC allows aud to be either a string or an array of strings
            boolean isValid = aud instanceof List<?> audList
                ? audList.stream()
                    .map(String::valueOf)
                    .anyMatch(expectedAudience::equals)
                : expectedAudience.equals(aud.toString());

            if (!isValid) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "Invalid audience claim", null
                ));
            }

            return OAuth2TokenValidatorResult.success();
        }
    }
}
