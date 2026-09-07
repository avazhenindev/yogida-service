package com.yogida.meditation;

import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real Liquibase changelog against a real PostgreSQL container, then makes Hibernate
 * validate every entity mapping against the schema Liquibase actually produced.
 *
 * <p>This is the test that would have caught the defect that reached production:
 * {@code profile.two_factor_enabled} was declared {@code number(1)} by changelog 003 while
 * {@code ProfileEntity} mapped it as {@code Boolean}. Nothing noticed, because
 * {@code ddl-auto=none} skips validation and no test had ever booted against Postgres. It was
 * found only when server-side provisioning became the first code to write a profile — in
 * production. The same class of defect had already been fixed once, for
 * {@code user_subscription.auto_renew} in changeset 013, and was never checked for elsewhere.
 *
 * <p>H2 cannot substitute for the container. Its type system accepts mappings Postgres rejects,
 * so the very drift this guards against is invisible there.
 *
 * <p>It also proves the changelog replays cleanly from nothing, which is the other thing no
 * other test covers: a changeset that only works against an already-migrated database passes
 * every local check and fails on a fresh environment.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("schema-validation")
// Only the persistence slice. Booting the full application would drag in the S3 client, the
// RevenueCat client and the security filter chain, none of which say anything about schema
// drift and all of which need credentials.
@ImportAutoConfiguration({
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        LiquibaseAutoConfiguration.class
})
@DisplayName("Liquibase changelog and JPA entities agree on real PostgreSQL")
class SchemaValidationIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private DataSource dataSource;

    /**
     * The assertion is the context starting at all: {@code ddl-auto=validate} fails startup on
     * any mismatch between an entity mapping and the migrated schema. The body then confirms the
     * test is talking to the container rather than a developer's local database — without that
     * check, a silent {@code @ServiceConnection} binding failure would turn this into a
     * destructive no-op that migrates and validates against real data.
     */
    @Test
    void changelogMatchesEntityMappings() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertThat(url)
                    .as("must run against the Testcontainers instance, not a local database")
                    .contains(String.valueOf(POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)));
        }
    }
}
