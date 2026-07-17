package com.yogida.meditation.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Spring Security configuration for OAuth2 Resource Server with JWT validation.
 * Accepts JWTs from both Keycloak realms (mobile + admin) via per-issuer
 * authentication manager resolution and protects all business endpoints.
 */
@Slf4j
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
     * Note: matchers are relative to the servlet context path (/api).
     * - Swagger, API docs, actuator health and the RevenueCat webhook are public
     * - /admin/** requires the ADMIN realm role
     * - All other endpoints require an authenticated JWT (deny-by-default)
     */
    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver) throws Exception {
        if (corsConfigurationSource.isPresent()) {
            http.cors(cors -> cors.configurationSource(corsConfigurationSource.get()));
        }
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                // RevenueCat webhook authenticates with a shared secret header, not a JWT
                .requestMatchers("/webhooks/revenuecat").permitAll()
                // Admin endpoints require the Keycloak realm role 'admin'
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // User-facing endpoints require a valid JWT
                .requestMatchers("/media/**", "/entitlement/**", "/media-categories/**", "/users/**",
                    "/favourites/**", "/profiles/**", "/subscriptions/**", "/media-subscriptions/**")
                .authenticated()
                // Deny-by-default for any future endpoints
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(authenticationManagerResolver));

        return http.build();
    }

    /**
     * Resolves the authentication manager per JWT issuer, supporting both the
     * mobile realm ({@code yogida}) and the admin realm ({@code yogida-admin}).
     * Tokens from any other issuer are rejected.
     */
    @Bean
    @ConditionalOnMissingBean(AuthenticationManagerResolver.class)
    public AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver(
            JwtAuthenticationConverter jwtAuthenticationConverter) {
        Map<String, AuthenticationManager> managersByIssuer = Stream
            .of(jwtProperties.issuer(), jwtProperties.adminIssuer())
            .distinct()
            .collect(Collectors.toMap(Function.identity(),
                issuer -> jwtAuthenticationManager(issuer, jwtAuthenticationConverter)));

        return new JwtIssuerAuthenticationManagerResolver(issuer -> {
            log.debug("Resolving JWT authentication manager for issuer: {}", issuer);
            return managersByIssuer.get(issuer);
        });
    }

    /**
     * Builds an authentication manager for a single issuer with issuer/timestamp
     * validation plus the shared audience validation and role mapping.
     */
    private AuthenticationManager jwtAuthenticationManager(String issuer, JwtAuthenticationConverter converter) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withIssuerLocation(issuer)
            .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer),
            new AudienceValidator(jwtProperties.audience())
        ));

        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(converter);
        return provider::authenticate;
    }

    /**
     * Maps Keycloak realm roles (realm_access.roles) to Spring Security ROLE_* authorities,
     * in addition to the default SCOPE_* authorities.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
                roles.stream()
                    .map(String::valueOf)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .forEach(authorities::add);
            }
            return authorities;
        });
        return converter;
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
