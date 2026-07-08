package ru.adiaphora.platform.support;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

/**
 * Resets the questionnaire tables and seeds an ACTIVE definition covering every question the rules
 * module reads. Done in SQL because the questionnaire module's repositories are package-private
 * (module boundary). Integration tests share one singleton MySQL, so tests that save answers must
 * reseed in {@code @BeforeEach} — mirrors the reset-and-seed pattern of the questionnaire module's
 * own tests.
 */
public final class QuestionnaireTestSeed {

    private QuestionnaireTestSeed() {
    }

    public static void seedRulesQuestionnaire(JdbcTemplate jdbc) {
        for (String table : new String[]{"question_answers", "questionnaire_responses",
                "question_options", "question_definitions", "question_sections",
                "questionnaire_versions"}) {
            jdbc.update("DELETE FROM " + table);
        }

        String versionId = newId();
        jdbc.update("INSERT INTO questionnaire_versions (id, code, label, status, created_at, updated_at) "
                + "VALUES (UNHEX(?), 'rules-test-v1', 'Rules test questionnaire', 'ACTIVE', NOW(6), NOW(6))",
                versionId);
        jdbc.update("INSERT INTO question_sections (id, version_id, code, title, display_order) "
                + "VALUES (UNHEX(?), UNHEX(?), 'debts', 'Debts', 1)", newId(), versionId);

        insertQuestion(jdbc, versionId, "totalDebtAmount", "MONEY", 1);
        insertQuestion(jdbc, versionId, "hasRegularIncome", "BOOLEAN", 2);
        insertQuestion(jdbc, versionId, "ownsMortgagedHome", "BOOLEAN", 3);
        insertQuestion(jdbc, versionId, "previousBankruptcy", "BOOLEAN", 4);
        String choiceId = insertQuestion(jdbc, versionId, "recentPropertyTransaction", "SINGLE_CHOICE", 5);
        int order = 1;
        for (String option : new String[]{"none", "sold", "gifted"}) {
            jdbc.update("INSERT INTO question_options (id, question_definition_id, value, label, display_order) "
                    + "VALUES (UNHEX(?), UNHEX(?), ?, ?, ?)", newId(), choiceId, option, option, order++);
        }
    }

    private static String insertQuestion(JdbcTemplate jdbc, String versionId, String code, String type,
                                         int order) {
        String id = newId();
        jdbc.update("INSERT INTO question_definitions (id, version_id, section_code, code, type, label, "
                + "help_text, required, display_order, validation_configuration) "
                + "VALUES (UNHEX(?), UNHEX(?), 'debts', ?, ?, ?, NULL, 1, ?, NULL)",
                id, versionId, code, type, code, order);
        return id;
    }

    private static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
