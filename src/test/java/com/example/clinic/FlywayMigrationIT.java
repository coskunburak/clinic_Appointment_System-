package com.example.clinic;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class FlywayMigrationIT extends AbstractIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("TC-039 / NFR-003: Flyway V1, V2, and V3 migrations execute cleanly and create expected tables")
    void migrations_createExpectedTables() {
        MigrationInfo current = flyway.info().current();
        assertThat(current).isNotNull();
        assertThat(current.getVersion().getVersion()).isEqualTo("3");
        assertThat(current.getDescription()).isEqualTo("create staff and availability");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<String> tableNames = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name",
                String.class
        );

        assertThat(tableNames).contains(
                "flyway_schema_history",
                "organization",
                "branch",
                "branch_opening_hours",
                "department",
                "user_account",
                "membership",
                "patient_profile",
                "staff_profile",
                "service_definition",
                "branch_service",
                "staff_assignment",
                "staff_service_eligibility",
                "weekly_availability",
                "availability_exception"
        );
    }

    @Test
    @DisplayName("TC-039 / NFR-003: Subsequent migration call is idempotent and does not re-execute migrations")
    void subsequentMigration_isIdempotent() {
        MigrateResult result = flyway.migrate();
        assertThat(result.migrationsExecuted).isZero();

        MigrationInfo current = flyway.info().current();
        assertThat(current).isNotNull();
        assertThat(current.getVersion().getVersion()).isEqualTo("3");
    }

    @Test
    @DisplayName("TC-039 / NFR-003: Flyway validation succeeds without schema drift or checksum mismatch")
    void flywayValidation_succeeds() {
        assertThatCode(() -> flyway.validate()).doesNotThrowAnyException();
    }
}
