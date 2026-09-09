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

import com.yogida.meditation.repository.S3ObjectRepository;
import org.springframework.data.jpa.repository.Query;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Set;

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

    /**
     * Every column that references {@code s3_object} must appear in both reference queries.
     *
     * <p>{@code countReferents} and {@code findOrphans} each enumerate the referencing columns by
     * hand, in native SQL. That duplication is survivable; the failure mode behind it is not.
     * {@code deleteObjectAfterCommit} deletes the R2 object when {@code countReferents} returns 0,
     * so a column missing from that list means a row still in use is read as an orphan and its
     * object is destroyed after commit — silently, and only for the entity nobody remembered.
     *
     * <p>Nothing enforced the list. Adding a fifth table with a foreign key into {@code s3_object}
     * compiles, migrates, passes every other test, and takes the storage layer with it. This asks
     * PostgreSQL's own catalog which columns exist and checks the SQL against that, so the schema
     * is the source of truth rather than whoever last edited the repository.
     *
     * <p>Reads the query text off the {@code @Query} annotations rather than restating the column
     * list here, which would just be a third copy to keep in sync.
     */
    @Test
    @DisplayName("both s3_object reference queries cover every foreign key into it")
    void referenceQueriesCoverEveryForeignKey() throws Exception {
        Set<String> referencingColumns = new LinkedHashSet<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("""
                     SELECT tc.table_name, kcu.column_name
                     FROM information_schema.table_constraints tc
                     JOIN information_schema.key_column_usage kcu
                       ON tc.constraint_name = kcu.constraint_name
                      AND tc.table_schema = kcu.table_schema
                     JOIN information_schema.constraint_column_usage ccu
                       ON tc.constraint_name = ccu.constraint_name
                      AND tc.table_schema = ccu.table_schema
                     WHERE tc.constraint_type = 'FOREIGN KEY'
                       AND ccu.table_name = 's3_object'
                     """)) {
            while (rs.next()) {
                referencingColumns.add(rs.getString("table_name") + "." + rs.getString("column_name"));
            }
        }

        assertThat(referencingColumns)
                .as("the changelog should create foreign keys into s3_object; finding none means "
                        + "this test is passing vacuously")
                .isNotEmpty();

        String countSql = queryOf("countReferents", Long.class);
        String orphanSql = queryOf("findOrphans");

        for (String qualified : referencingColumns) {
            String table = qualified.substring(0, qualified.indexOf('.'));
            String column = qualified.substring(qualified.indexOf('.') + 1);
            assertThat(countSql)
                    .as("S3ObjectRepository.countReferents must count %s, or deleting an object "
                            + "still referenced by it will destroy the R2 object", qualified)
                    .contains(table)
                    .contains(column);
            assertThat(orphanSql)
                    .as("S3ObjectRepository.findOrphans must exclude %s, or the reconciliation "
                            + "report will list objects that are still in use", qualified)
                    .contains(table)
                    .contains(column);
        }
    }

    private static String queryOf(String method, Class<?>... parameterTypes) throws Exception {
        return S3ObjectRepository.class.getMethod(method, parameterTypes)
                .getAnnotation(Query.class)
                .value();
    }
}
