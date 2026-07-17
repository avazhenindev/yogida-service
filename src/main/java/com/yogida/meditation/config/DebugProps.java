package com.yogida.meditation.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Debug bean that logs multipart upload configuration when enabled.
 * Activated by setting PRINT_PROPS=true or app.debug.print-props=true in the environment.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.debug.print-props", havingValue = "true")
@RequiredArgsConstructor
public class DebugProps {

    private final Environment environment;

    @PostConstruct
    void logMultipartConfig() {
        log.debug("=== Multipart Upload Configuration ===");
        log.debug("spring.servlet.multipart.max-file-size: {}",
                environment.getProperty("spring.servlet.multipart.max-file-size"));
        log.debug("spring.servlet.multipart.max-request-size: {}",
                environment.getProperty("spring.servlet.multipart.max-request-size"));
        log.debug("server.tomcat.max-http-form-post-size: {}",
                environment.getProperty("server.tomcat.max-http-form-post-size"));
        log.debug("=====================================");
    }
}



