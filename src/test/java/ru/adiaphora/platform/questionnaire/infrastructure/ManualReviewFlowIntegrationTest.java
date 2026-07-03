package ru.adiaphora.platform.questionnaire.infrastructure;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.adiaphora.platform.auth.domain.User;
import ru.adiaphora.platform.auth.domain.UserRepository;
import ru.adiaphora.platform.auth.domain.UserRole;
import ru.adiaphora.platform.questionnaire.domain.QuestionType;
import ru.adiaphora.platform.support.AbstractIntegrationTest;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end manual-review happy path exercising rules → auto-created review → assign → approve →
 * document generation → download across modules. Lives in this package so it can seed the full
 * questionnaire via the package-private repositories.
 */
class ManualReviewFlowIntegrationTest extends AbstractIntegrationTest {

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
    @Autowired
    private UserRepository users;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // Shared DB (singleton container): reset questionnaire tables, then seed this flow's version.
    @BeforeEach
    void seedQuestionnaire() {
        answers.deleteAll();
        responses.deleteAll();
        options.deleteAll();
        questions.deleteAll();
        sections.deleteAll();
        versions.deleteAll();

        UUID versionId = UUID.randomUUID();
        versions.save(new QuestionnaireVersionEntity(versionId, "flow-v1", "Flow questionnaire",
                VersionStatus.ACTIVE));
        sections.save(new QuestionSectionEntity(UUID.randomUUID(), versionId, "main", "Main", 1));
        questions.save(new QuestionDefinitionEntity(UUID.randomUUID(), versionId, "main",
                "totalDebtAmount", QuestionType.MONEY, "Total debts", null, true, 1, null));
        questions.save(new QuestionDefinitionEntity(UUID.randomUUID(), versionId, "main",
                "hasRegularIncome", QuestionType.BOOLEAN, "Regular income?", null, true, 2, null));
        questions.save(new QuestionDefinitionEntity(UUID.randomUUID(), versionId, "main",
                "ownsMortgagedHome", QuestionType.BOOLEAN, "Mortgaged home?", null, true, 3, null));
    }

    @Test
    void mortgageCaseGoesThroughManualReviewToDownloadableDocument() throws Exception {
        String userToken = registerAndLoginUser("user-" + UUID.randomUUID() + "@example.test");
        UUID lawyerId = UUID.randomUUID();
        String lawyerToken = staff(lawyerId, UserRole.LAWYER);
        String adminToken = staff(UUID.randomUUID(), UserRole.ADMIN);

        // 1. Create case, answer questions (mortgage=true triggers manual review), submit.
        String applicationId = createApplication(userToken);
        answer(userToken, applicationId, "totalDebtAmount", "100000");
        answer(userToken, applicationId, "hasRegularIncome", "true");
        answer(userToken, applicationId, "ownsMortgagedHome", "true");
        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/submit")
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken))).andExpect(status().isNoContent());

        // 2. Evaluate → manual review required.
        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/evaluate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route").value("MANUAL_REVIEW"))
                .andExpect(jsonPath("$.manualReviewRequired").value(true));

        // 3. Admin finds the auto-created review and assigns it to the lawyer.
        String reviewId = findReviewId(adminToken, applicationId);
        mockMvc.perform(post("/api/v1/reviews/" + reviewId + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("assigneeId", lawyerId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        // 4. Lawyer approves.
        mockMvc.perform(post("/api/v1/reviews/" + reviewId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, bearer(lawyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "eligible after review"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // 5. Application is approved for document generation.
        mockMvc.perform(get("/api/v1/applications/" + applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(jsonPath("$.status").value("APPROVED_FOR_DOCUMENT_GENERATION"));

        // 6. Request a document and download it.
        String createResponse = mockMvc.perform(post("/api/v1/applications/" + applicationId + "/documents")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("READY_FOR_DOWNLOAD"))
                .andReturn().getResponse().getContentAsString();
        String documentId = objectMapper.readTree(createResponse).get("documentId").asText();

        mockMvc.perform(get("/api/v1/applications/" + applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(jsonPath("$.status").value("DOCUMENTS_READY"));

        mockMvc.perform(get("/api/v1/documents/" + documentId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")));

        // 7. The audit log (readable only by ADMIN/AUDITOR) captured the cross-module actions.
        mockMvc.perform(get("/api/v1/audit/events?applicationId=" + applicationId + "&size=100")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].action", hasItem("RULES_EVALUATED")))
                .andExpect(jsonPath("$.items[*].action", hasItem("REVIEW_DECISION_RECORDED")))
                .andExpect(jsonPath("$.items[*].action", hasItem("DOCUMENT_REQUESTED")));
    }

    private String findReviewId(String adminToken, String applicationId) throws Exception {
        String response = mockMvc.perform(get("/api/v1/reviews?size=100")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode item : objectMapper.readTree(response).get("items")) {
            if (item.get("applicationId").asText().equals(applicationId)) {
                return item.get("reviewId").asText();
            }
        }
        throw new AssertionError("No review found for application " + applicationId);
    }

    private void answer(String token, String applicationId, String code, String value) throws Exception {
        mockMvc.perform(put("/api/v1/applications/" + applicationId + "/answers/" + code)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", value))))
                .andExpect(status().isOk());
    }

    private String createApplication(String token) throws Exception {
        String response = mockMvc.perform(post("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("applicationId").asText();
    }

    private String registerAndLoginUser(String email) throws Exception {
        register(email, "Password123!");
        return login(email, "Password123!");
    }

    private String staff(UUID id, UserRole role) throws Exception {
        String email = role.name().toLowerCase() + "-" + id + "@example.test";
        users.save(User.register(id, email, passwordEncoder.encode("Password123!"), role));
        return login(email, "Password123!");
    }
}
