package ru.adiaphora.platform.common.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.adiaphora.platform.support.AbstractIntegrationTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI-018 migration check: the full Flyway migration set applied cleanly to a real MySQL. A failing or
 * missing migration breaks this test (and, via {@code ddl-auto: validate}, any entity/schema drift
 * breaks context startup for every integration test), so a PR with a broken migration cannot go green.
 */
class FlywayMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void everyMigrationFileIsAppliedSuccessfully() throws IOException {
        long migrationFiles;
        try (Stream<Path> files = Files.list(Path.of("src/main/resources/db/migration"))) {
            migrationFiles = files.filter(p -> p.getFileName().toString().matches("V\\d+__.*\\.sql"))
                    .count();
        }
        assertThat(migrationFiles).isGreaterThan(0);

        Integer applied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version IS NOT NULL AND success = 1",
                Integer.class);
        Integer failed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0", Integer.class);

        assertThat(failed).as("no failed migrations").isZero();
        assertThat(applied).as("every migration file applied").isEqualTo((int) migrationFiles);
    }
}
