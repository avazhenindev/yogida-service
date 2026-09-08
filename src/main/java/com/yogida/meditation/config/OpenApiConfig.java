package com.yogida.meditation.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI meditationServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Meditation Service API")
                .description("""
                    REST API for the Yogida meditation platform.\s
                    Provides presigned Cloudflare R2 streaming URLs \
                    and a catalog of available meditation media grouped by bucket.""")
                .version("v1")
                .contact(new Contact()
                    .name("Yogida Team")
                    .url("https://github.com/yogida")))
            .servers(List.of(
                new Server().url("/api").description("Current environment")))
            // Declared so the document says how to authenticate. Without it the spec presented
            // every endpoint as anonymous, Swagger UI offered no way to send a token, and a
            // generated client had nothing to attach credentials to — for an API that is
            // deny-by-default almost everywhere.
            .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Keycloak-issued access token. Two realms are accepted, the "
                        + "mobile realm and the admin realm; the token's issuer selects which. "
                        + "Endpoints under /admin additionally require the ADMIN role.")))
            // Applied globally rather than per operation, because deny-by-default is the rule
            // and the genuinely public endpoints (the RevenueCat webhook, health, the docs
            // themselves) are the exception.
            .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
