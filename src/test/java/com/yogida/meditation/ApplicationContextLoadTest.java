package com.yogida.meditation;

import com.yogida.meditation.config.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the application context.
 *
 * <p>The assertion is that it starts at all. Every other test in this module mocks its
 * collaborators, so nothing verified that the beans actually wire together — a missing bean, an
 * ambiguous {@code @ExceptionHandler} mapping, a bad derived query name, an unresolvable
 * {@code @Value} — was invisible to the whole suite and surfaced only on deploy. That is not
 * hypothetical: adding handlers to {@code GlobalExceptionHandler} for exception types
 * {@code ResponseEntityExceptionHandler} already claims produces exactly such a failure.
 *
 * <p>Cheap on purpose: it runs in the normal {@code test} phase and needs no Docker, unlike
 * {@code SchemaValidationIT}, which boots against a real PostgreSQL container under
 * {@code verify}. This one catches wiring; that one catches schema drift.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DisplayName("The Spring context wires together")
class ApplicationContextLoadTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
        assertThat(context.getBeanDefinitionCount()).isPositive();
    }
}
