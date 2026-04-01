package com.yogida.meditation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

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
                new Server().url("/").description("Current environment")));
    }
}

