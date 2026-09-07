package com.yogida.meditation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration, driven by the {@code cors.allowed-origins} property.
 *
 * <p>Previously this was gated on {@code @Profile("local")} and allowed every origin
 * unconditionally. No {@code application-local.properties} existed, so the profile was
 * never usefully activated and the bean was effectively dead — while still being a
 * wildcard if it ever did switch on.
 *
 * <p>Now the origin list is configuration. The bean only exists when the property is
 * non-empty, so a deployment that sets nothing gets no CORS bean at all and
 * {@code SecurityConfig} leaves CORS disabled. Set {@code CORS_ALLOWED_ORIGINS} to a
 * comma-separated list of origins, or to {@code *} for a permissive local setup.
 */
@Configuration
@ConditionalOnExpression("!'${cors.allowed-origins:}'.isBlank()")
public class LocalCorsConfig {

    private final List<String> allowedOrigins;

    public LocalCorsConfig(@Value("${cors.allowed-origins:}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .toList();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        // Patterns rather than plain origins, so a wildcard stays legal alongside
        // allowCredentials — setAllowedOrigins("*") with credentials is rejected by Spring.
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
