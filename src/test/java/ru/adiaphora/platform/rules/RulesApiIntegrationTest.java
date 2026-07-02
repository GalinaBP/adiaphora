package ru.adiaphora.platform.rules;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import ru.adiaphora.platform.support.AbstractIntegrationTest;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the full evaluate chain (HTTP → engine → persistence → application status → audit) for a
 * case with no answers: the engine reports missing information and the case moves to NEEDS_INFORMATION.
 * The route-selection branches are covered exhaustively by {@code RuleEngineTest}.
 */
class RulesApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void evaluatingACaseWithoutAnswersReportsMissingInformation() throws Exception {
        String token = authenticatedUser();
        String applicationId = createSubmittedApplication(token);

        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/evaluate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route").value("INSUFFICIENT_INFORMATION"))
                .andExpect(jsonPath("$.missingInformation.length()").value(org.hamcrest.Matchers.greaterThan(0)));

        mockMvc.perform(get("/api/v1/applications/" + applicationId + "/evaluations/latest")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route").value("INSUFFICIENT_INFORMATION"));

        mockMvc.perform(get("/api/v1/applications/" + applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_INFORMATION"))
                .andExpect(jsonPath("$.route").value("INSUFFICIENT_INFORMATION"));
    }

    @Test
    void latestEvaluationIsNotFoundBeforeAnyEvaluation() throws Exception {
        String token = authenticatedUser();
        String applicationId = createSubmittedApplication(token);

        mockMvc.perform(get("/api/v1/applications/" + applicationId + "/evaluations/latest")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotEvaluateAnotherUsersCase() throws Exception {
        String ownerToken = authenticatedUser();
        String applicationId = createSubmittedApplication(ownerToken);

        String otherToken = authenticatedUser();
        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/evaluate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isForbidden());
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
