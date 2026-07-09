package ru.adiaphora.platform.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.adiaphora.platform.support.AbstractIntegrationTest;
import ru.adiaphora.platform.support.QuestionnaireTestSeed;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI-013: evaluation runs are immutable history. Verifies each run persists the ruleset version, the
 * input snapshot hash, the triggered/blocking findings, the route and timestamps — and that a
 * re-evaluation with changed answers creates a new row while the historical row (and its findings)
 * stays byte-identical. Rows are read straight from MySQL so nothing in the application layer can
 * mask a mutation.
 */
class RulesEvaluationPersistenceIntegrationTest extends AbstractIntegrationTest {

    private static final String EVALUATION_COLUMNS =
            "SELECT ruleset_version, questionnaire_version, input_snapshot_hash, started_at, "
                    + "completed_at, route, manual_review_required, missing_information "
                    + "FROM rule_evaluations WHERE id = UNHEX(?)";

    private static final String FINDING_COLUMNS =
            "SELECT rule_code, outcome, severity, internal_reason, user_message, "
                    + "blocks_automatic_decision "
                    + "FROM rule_evaluation_findings WHERE evaluation_id = UNHEX(?) ORDER BY rule_code";

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seedRulesQuestionnaire() {
        QuestionnaireTestSeed.seedRulesQuestionnaire(jdbc);
    }

    @Test
    void historicalEvaluationRemainsUnchangedAfterReevaluation() throws Exception {
        String token = authenticatedUser();
        String applicationId = createSubmittedApplication(token);

        // First run: mortgaged home -> manual review with a blocking finding.
        saveAnswer(token, applicationId, "totalDebtAmount", "100000");
        saveAnswer(token, applicationId, "hasRegularIncome", "true");
        saveAnswer(token, applicationId, "ownsMortgagedHome", "true");
        saveAnswer(token, applicationId, "previousBankruptcy", "false");
        saveAnswer(token, applicationId, "recentPropertyTransaction", "none");
        saveAnswer(token, applicationId, "mfcStatutoryGround", "enforcement_ended");

        String firstResponse = evaluate(token, applicationId);
        String firstId = objectMapper.readTree(firstResponse).get("evaluationId").asText();
        String firstHash = objectMapper.readTree(firstResponse).get("inputSnapshotHash").asText();

        assertThat(firstHash).matches("[0-9a-f]{64}");
        assertThat(objectMapper.readTree(firstResponse).get("route").asText()).isEqualTo("MANUAL_REVIEW");

        Map<String, Object> firstRowBefore = readEvaluationRow(firstId);
        List<Map<String, Object>> firstFindingsBefore = readFindingRows(firstId);

        assertThat(firstRowBefore.get("ruleset_version").toString()).isNotBlank();
        assertThat(firstRowBefore.get("input_snapshot_hash")).isEqualTo(firstHash);
        assertThat(firstRowBefore.get("started_at")).isNotNull();
        assertThat(firstRowBefore.get("completed_at")).isNotNull();
        assertThat(firstFindingsBefore)
                .anyMatch(f -> f.get("rule_code").equals("MANUAL-REVIEW-MORTGAGE")
                        && toBoolean(f.get("blocks_automatic_decision")));

        // Second run: answers changed -> new row, first row untouched.
        saveAnswer(token, applicationId, "ownsMortgagedHome", "false");
        saveAnswer(token, applicationId, "totalDebtAmount", "2000000");

        String secondResponse = evaluate(token, applicationId);
        String secondId = objectMapper.readTree(secondResponse).get("evaluationId").asText();
        String secondHash = objectMapper.readTree(secondResponse).get("inputSnapshotHash").asText();

        assertThat(secondId).isNotEqualTo(firstId);
        assertThat(secondHash).isNotEqualTo(firstHash);
        assertThat(objectMapper.readTree(secondResponse).get("route").asText())
                .isEqualTo("COURT_PRELIMINARY");

        Integer rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM rule_evaluations WHERE application_id = UNHEX(?)",
                Integer.class, hex(applicationId));
        assertThat(rows).isEqualTo(2);

        assertThat(readEvaluationRow(firstId)).isEqualTo(firstRowBefore);
        assertThat(readFindingRows(firstId)).isEqualTo(firstFindingsBefore);

        // The latest endpoint serves the new run; the historical one stays retrievable by its id.
        mockMvc.perform(get("/api/v1/applications/" + applicationId + "/evaluations/latest")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationId").value(secondId))
                .andExpect(jsonPath("$.inputSnapshotHash").value(secondHash));
    }

    @Test
    void identicalAnswersProduceTheSameSnapshotHashAcrossRuns() throws Exception {
        String token = authenticatedUser();
        String applicationId = createSubmittedApplication(token);
        saveAnswer(token, applicationId, "totalDebtAmount", "100000");

        String firstHash = objectMapper.readTree(evaluate(token, applicationId))
                .get("inputSnapshotHash").asText();
        String secondHash = objectMapper.readTree(evaluate(token, applicationId))
                .get("inputSnapshotHash").asText();

        assertThat(firstHash).isEqualTo(secondHash);
    }

    private Map<String, Object> readEvaluationRow(String evaluationId) {
        return jdbc.queryForMap(EVALUATION_COLUMNS, hex(evaluationId));
    }

    private List<Map<String, Object>> readFindingRows(String evaluationId) {
        return jdbc.queryForList(FINDING_COLUMNS, hex(evaluationId));
    }

    private static String hex(String uuid) {
        return uuid.replace("-", "");
    }

    private static boolean toBoolean(Object bitColumn) {
        return bitColumn instanceof Boolean b ? b : ((Number) bitColumn).intValue() == 1;
    }

    private String evaluate(String token, String applicationId) throws Exception {
        return mockMvc.perform(post("/api/v1/applications/" + applicationId + "/evaluate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private void saveAnswer(String token, String applicationId, String questionCode, String value)
            throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("value", value));
        mockMvc.perform(put("/api/v1/applications/" + applicationId + "/answers/" + questionCode)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private String authenticatedUser() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.test";
        register(email, "Password123!");
        return login(email, "Password123!");
    }

    private String createSubmittedApplication(String token) throws Exception {
        String response = mockMvc.perform(post("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String applicationId = objectMapper.readTree(response).get("applicationId").asText();
        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        return applicationId;
    }
}
