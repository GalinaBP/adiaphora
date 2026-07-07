package ru.adiaphora.platform.questionnaire.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import ru.adiaphora.platform.questionnaire.domain.QuestionType;
import ru.adiaphora.platform.support.AbstractIntegrationTest;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestionnaireApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private QuestionnaireVersionJpaRepository versions;
    @Autowired
    private QuestionSectionJpaRepository sections;
    @Autowired
    private QuestionDefinitionJpaRepository questions;
    @Autowired
    private QuestionOptionJpaRepository options;
    @Autowired
    private QuestionnaireResponseJpaRepository responses;
    @Autowired
    private QuestionAnswerJpaRepository answers;

    // Reset the questionnaire definition/response tables before each test, then seed this class's own
    // single-question version. Needed because the integration tests share one database (singleton
    // container) and another class seeds a different active version.
    @BeforeEach
    void seedActiveVersion() {
        answers.deleteAll();
        responses.deleteAll();
        options.deleteAll();
        questions.deleteAll();
        sections.deleteAll();
        versions.deleteAll();

        UUID versionId = UUID.randomUUID();
        versions.save(new QuestionnaireVersionEntity(versionId, "test-v1", "Test questionnaire",
                VersionStatus.ACTIVE));
        sections.save(new QuestionSectionEntity(UUID.randomUUID(), versionId, "debts", "Debts", 1));
        questions.save(new QuestionDefinitionEntity(UUID.randomUUID(), versionId, "debts",
                "totalDebtAmount", QuestionType.MONEY, "Total debts", null, true, 1, null));
    }

    @Test
    void currentQuestionnaireIsAvailableToAuthenticatedUsers() throws Exception {
        String token = authenticatedUser();
        mockMvc.perform(get("/api/v1/questionnaires/current").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionCode").value("test-v1"))
                .andExpect(jsonPath("$.questions.length()").value(1));
    }

    @Test
    void savingAnswerIncrementallyCompletesTheQuestionnaire() throws Exception {
        String token = authenticatedUser();
        String applicationId = createApplication(token);

        mockMvc.perform(put("/api/v1/applications/" + applicationId + "/answers/totalDebtAmount")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", "100000"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complete").value(true))
                .andExpect(jsonPath("$.requiredAnswered").value(1));

        // Resume: the saved answer is persisted and the response records which version was answered.
        mockMvc.perform(get("/api/v1/applications/" + applicationId + "/questionnaire")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers.totalDebtAmount").value("100000"))
                .andExpect(jsonPath("$.versionCode").value("test-v1"));

        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/questionnaire/validate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complete").value(true));
    }

    @Test
    void invalidAnswerIsRejected() throws Exception {
        String token = authenticatedUser();
        String applicationId = createApplication(token);

        mockMvc.perform(put("/api/v1/applications/" + applicationId + "/answers/totalDebtAmount")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", "-5"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("QUESTIONNAIRE_VALIDATION_ERROR"));
    }

    private String authenticatedUser() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.test";
        register(email, "Password123!");
        return login(email, "Password123!");
    }

    private String createApplication(String token) throws Exception {
        String response = mockMvc.perform(post("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("applicationId").asText();
    }
}
