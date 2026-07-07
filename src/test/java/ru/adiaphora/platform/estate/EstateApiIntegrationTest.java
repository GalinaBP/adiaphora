package ru.adiaphora.platform.estate;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import ru.adiaphora.platform.support.AbstractIntegrationTest;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API tests for the estate module (creditors & assets): CRUD works, duplicates are warned (not
 * rejected), and a normal user cannot touch another user's case.
 */
class EstateApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void creditorCanBeAddedEditedAndDeleted() throws Exception {
        String token = authenticatedUser();
        String appId = createApplication(token);
        String base = "/api/v1/applications/" + appId + "/creditors";

        // Add
        String created = mockMvc.perform(post(base).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Sberbank", "type", "BANK", "totalAmount", 100000))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sberbank"))
                .andExpect(jsonPath("$.duplicateWarning").value(false))
                .andReturn().getResponse().getContentAsString();
        String creditorId = objectMapper.readTree(created).get("creditorId").asText();

        // Edit
        mockMvc.perform(put(base + "/" + creditorId).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Sberbank (updated)", "type", "BANK", "totalAmount", 120000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sberbank (updated)"));

        // Read back
        mockMvc.perform(get(base + "/" + creditorId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(120000));

        // Delete
        mockMvc.perform(delete(base + "/" + creditorId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(base + "/" + creditorId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void assetCanBeAddedAndListed() throws Exception {
        String token = authenticatedUser();
        String appId = createApplication(token);
        String base = "/api/v1/applications/" + appId + "/assets";

        mockMvc.perform(post(base).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("type", "VEHICLE", "description", "Lada Vesta 2019",
                                "estimatedValue", 650000))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("VEHICLE"));

        mockMvc.perform(get(base).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Lada Vesta 2019"));
    }

    @Test
    void duplicateCreditorIsWarnedNotRejected() throws Exception {
        String token = authenticatedUser();
        String appId = createApplication(token);
        String base = "/api/v1/applications/" + appId + "/creditors";
        String body = json(Map.of("name", "Tinkoff", "type", "BANK", "inn", "7710140679",
                "totalAmount", 50000));

        mockMvc.perform(post(base).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duplicateWarning").value(false));

        // Same name + INN again: created, but flagged as a possible duplicate.
        mockMvc.perform(post(base).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duplicateWarning").value(true));
    }

    @Test
    void anotherUserCannotAccessTheCasesCreditors() throws Exception {
        String ownerToken = authenticatedUser();
        String appId = createApplication(ownerToken);
        String base = "/api/v1/applications/" + appId + "/creditors";

        String otherToken = authenticatedUser();
        // Listing another user's creditors is forbidden.
        mockMvc.perform(get(base).header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isForbidden());
        // Adding to another user's case is forbidden.
        mockMvc.perform(post(base).header(HttpHeaders.AUTHORIZATION, bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "X", "type", "OTHER", "totalAmount", 1))))
                .andExpect(status().isForbidden());
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
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
