package ru.adiaphora.platform.audit;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI-015: drives a user journey (login, edit, read, evaluation, denied foreign access) and then
 * verifies the audit trail straight from MySQL: every action in scope leaves a record carrying actor,
 * action, object and time — and no record anywhere contains the sensitive answer value that was
 * saved along the way.
 */
class AuditTrailIntegrationTest extends AbstractIntegrationTest {

    /** Distinctive value: if it leaks into any audit column, the content scan fails. */
    private static final String SENSITIVE_ANSWER = "74250.55";

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seedQuestionnaire() {
        QuestionnaireTestSeed.seedRulesQuestionnaire(jdbc);
    }

    @Test
    void journeyLeavesCompleteTrailWithoutSensitiveContent() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.test";
        String userId = register(email, "Password123!");
        String token = login(email, "Password123!");

        String applicationId = createApplication(token);
        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        saveAnswer(token, applicationId, "totalDebtAmount", SENSITIVE_ANSWER);

        mockMvc.perform(get("/api/v1/applications/" + applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/evaluate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        // A different ordinary user is denied access to the case.
        String intruderEmail = "user-" + UUID.randomUUID() + "@example.test";
        String intruderId = register(intruderEmail, "Password123!");
        String intruderToken = login(intruderEmail, "Password123!");
        mockMvc.perform(get("/api/v1/applications/" + applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(intruderToken)))
                .andExpect(status().isForbidden());

        // --- the trail, read straight from the audit table -----------------------

        assertThat(actionRow("USER_LOGIN_SUCCEEDED", "object_id", userId))
                .containsEntry("result", "SUCCESS")
                .hasEntrySatisfying("actor_id", v -> assertThat(v).isNotNull())
                .hasEntrySatisfying("occurred_at", v -> assertThat(v).isNotNull());

        Map<String, Object> viewed = actionRow("APPLICATION_VIEWED", "actor_id", userId);
        assertThat(viewed).containsEntry("object_type", "Application");
        assertThat(hex((byte[]) viewed.get("object_id"))).isEqualToIgnoringCase(plain(applicationId));

        Map<String, Object> edited = actionRow("ANSWER_UPDATED", "application_id", applicationId);
        assertThat((String) edited.get("metadata")).contains("totalDebtAmount");
        assertThat(edited.get("actor_id")).isNotNull();

        assertThat(actionRow("APPLICATION_STATUS_CHANGED", "application_id", applicationId))
                .isNotNull();
        assertThat(actionRow("RULES_EVALUATED", "application_id", applicationId)).isNotNull();

        Map<String, Object> denied = actionRow("ACCESS_DENIED", "actor_id", intruderId);
        assertThat(denied).containsEntry("result", "FAILURE");
        assertThat((String) denied.get("metadata")).contains(applicationId);

        // --- acceptance criterion: no sensitive answer content anywhere ----------

        Integer leaks = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE metadata LIKE ? "
                        + "OR object_type LIKE ? OR actor_role LIKE ? OR action LIKE ?",
                Integer.class,
                "%" + SENSITIVE_ANSWER + "%", "%" + SENSITIVE_ANSWER + "%",
                "%" + SENSITIVE_ANSWER + "%", "%" + SENSITIVE_ANSWER + "%");
        assertThat(leaks).isZero();

        // Every record in the trail has an action, a time and a result.
        Integer incomplete = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE action IS NULL "
                        + "OR occurred_at IS NULL OR result IS NULL",
                Integer.class);
        assertThat(incomplete).isZero();
    }

    private Map<String, Object> actionRow(String action, String uuidColumn, String uuid) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM audit_events WHERE action = ? AND " + uuidColumn + " = UNHEX(?) "
                        + "ORDER BY occurred_at DESC LIMIT 1",
                action, plain(uuid));
        assertThat(rows).as("audit row for action %s (%s=%s)", action, uuidColumn, uuid).isNotEmpty();
        return rows.get(0);
    }

    private static String plain(String uuid) {
        return uuid.replace("-", "");
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void saveAnswer(String token, String applicationId, String questionCode, String value)
            throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("value", value));
        mockMvc.perform(put("/api/v1/applications/" + applicationId + "/answers/" + questionCode)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private String createApplication(String token) throws Exception {
        String response = mockMvc.perform(post("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("applicationId").asText();
    }
}
