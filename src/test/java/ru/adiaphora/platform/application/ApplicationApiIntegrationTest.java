package ru.adiaphora.platform.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import ru.adiaphora.platform.support.AbstractIntegrationTest;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void unauthenticatedCreateIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanCreateSubmitAndReadOwnCase() throws Exception {
        String token = authenticatedUser();
        String applicationId = createApplication(token);

        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/applications/" + applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_EVALUATION"));

        mockMvc.perform(get("/api/v1/applications/" + applicationId + "/status-history")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)); // created + submitted
    }

    @Test
    void userCannotReadAnotherUsersCase() throws Exception {
        String ownerToken = authenticatedUser();
        String applicationId = createApplication(ownerToken);

        String otherToken = authenticatedUser();
        mockMvc.perform(get("/api/v1/applications/" + applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ordinaryUserIsForbiddenFromAuditEndpoints() throws Exception {
        String token = authenticatedUser();
        mockMvc.perform(get("/api/v1/audit/events").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
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
