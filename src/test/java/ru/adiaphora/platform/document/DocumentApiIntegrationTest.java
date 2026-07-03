package ru.adiaphora.platform.document;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import ru.adiaphora.platform.support.AbstractIntegrationTest;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void requestGeneratesAndAllowsDownloadByOwner() throws Exception {
        String token = authenticatedUser();
        String applicationId = createApplication(token);

        String createResponse = mockMvc.perform(post("/api/v1/applications/" + applicationId + "/documents")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("READY_FOR_DOWNLOAD"))
                .andReturn().getResponse().getContentAsString();
        String documentId = objectMapper.readTree(createResponse).get("documentId").asText();

        mockMvc.perform(get("/api/v1/applications/" + applicationId + "/documents")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/v1/documents/" + documentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(documentId));

        mockMvc.perform(get("/api/v1/documents/" + documentId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(content().string(containsString("PLACEHOLDER DOCUMENT")));
    }

    @Test
    void anotherUserCannotDownload() throws Exception {
        String ownerToken = authenticatedUser();
        String applicationId = createApplication(ownerToken);
        String createResponse = mockMvc.perform(post("/api/v1/applications/" + applicationId + "/documents")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String documentId = objectMapper.readTree(createResponse).get("documentId").asText();

        String otherToken = authenticatedUser();
        mockMvc.perform(get("/api/v1/documents/" + documentId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
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
