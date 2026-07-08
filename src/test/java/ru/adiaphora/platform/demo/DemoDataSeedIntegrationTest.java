package ru.adiaphora.platform.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import ru.adiaphora.platform.support.AbstractIntegrationTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI-017 fixture load test: boots the application with seeding enabled (as a local demo would) and
 * verifies the fixtures load without any manual DB editing — role accounts, the boundary-case demo
 * applications with their evaluated routes, an open review task for the staff demo, and a partial
 * draft. Also proves all seeded identities are obviously synthetic and the seeder is idempotent.
 *
 * <p>The initializer resets the questionnaire tables <em>before</em> this context boots: earlier test
 * classes may have replaced the definition with a minimal one (shared singleton MySQL), and the demo
 * seeder needs the full seeded {@code v1} questionnaire — exactly what a fresh local boot gets.
 */
@TestPropertySource(properties = "adiaphora.seed.enabled=true")
@ContextConfiguration(initializers = DemoDataSeedIntegrationTest.ResetQuestionnaireTables.class)
class DemoDataSeedIntegrationTest extends AbstractIntegrationTest {

    static class ResetQuestionnaireTables
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            try (Connection connection = DriverManager.getConnection(
                    MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
                 Statement statement = connection.createStatement()) {
                for (String table : List.of("question_answers", "questionnaire_responses",
                        "question_options", "question_definitions", "question_sections",
                        "questionnaire_versions")) {
                    statement.executeUpdate("DELETE FROM " + table);
                }
            } catch (SQLException ex) {
                // 1146 = table doesn't exist: this context is the first to boot, Flyway will create
                // the schema and the questionnaire seed runs against an empty database anyway.
                if (ex.getErrorCode() != 1146) {
                    throw new IllegalStateException("Failed to reset questionnaire tables", ex);
                }
            }
        }
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DemoDataSeeder seeder;

    @Test
    void demoFixturesLoadOnStartupWithoutManualDbEditing() {
        // One account per role, all on the reserved example.test domain.
        for (String email : List.of("user@example.test", "operator@example.test",
                "lawyer@example.test", "admin@example.test", "auditor@example.test")) {
            assertThat(userCount(email)).as(email).isEqualTo(1);
        }
        Integer nonSynthetic = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email NOT LIKE '%@example.test'", Integer.class);
        assertThat(nonSynthetic).as("every account is obviously synthetic").isZero();

        // The demo user's cases cover the AI-012 boundary routes plus manual review and a draft.
        List<String> routes = jdbc.queryForList(
                "SELECT a.route FROM bankruptcy_applications a JOIN users u ON a.owner_id = u.id "
                        + "WHERE u.email = 'user@example.test'", String.class);
        assertThat(routes).hasSizeGreaterThanOrEqualTo(6);
        assertThat(routes)
                .contains("MFC_PRELIMINARY", "COURT_PRELIMINARY", "NOT_CURRENTLY_RECOMMENDED",
                        "MANUAL_REVIEW", "NOT_EVALUATED");
        assertThat(routes.stream().filter("MFC_PRELIMINARY"::equals).count())
                .as("both MFC boundary amounts (25,000 and 1,000,000)").isGreaterThanOrEqualTo(2);

        // Evaluations were really run and persisted; the mortgage case opened a review task.
        Integer evaluations = jdbc.queryForObject(
                "SELECT COUNT(*) FROM rule_evaluations e JOIN bankruptcy_applications a "
                        + "ON e.application_id = a.id JOIN users u ON a.owner_id = u.id "
                        + "WHERE u.email = 'user@example.test'", Integer.class);
        assertThat(evaluations).isGreaterThanOrEqualTo(5);

        Integer openReviews = jdbc.queryForObject(
                "SELECT COUNT(*) FROM reviews r JOIN bankruptcy_applications a "
                        + "ON r.application_id = a.id JOIN users u ON a.owner_id = u.id "
                        + "WHERE u.email = 'user@example.test' AND r.status = 'OPEN'", Integer.class);
        assertThat(openReviews).isGreaterThanOrEqualTo(1);
    }

    @Test
    void reseedingIsIdempotent() {
        Integer before = demoApplicationCount();
        seeder.run(null);
        assertThat(demoApplicationCount()).isEqualTo(before);
    }

    private Integer userCount(String email) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
    }

    private Integer demoApplicationCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM bankruptcy_applications a JOIN users u ON a.owner_id = u.id "
                        + "WHERE u.email = 'user@example.test'", Integer.class);
    }
}
