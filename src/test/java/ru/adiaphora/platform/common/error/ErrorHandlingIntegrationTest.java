package ru.adiaphora.platform.common.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import ru.adiaphora.platform.support.AbstractIntegrationTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI-016 negative API tests: every failure mode returns the ApiError envelope with a stable code and
 * a correlation id, client mistakes are 4xx (never 500), and no response ever contains a stack trace
 * or framework/DB detail.
 */
class ErrorHandlingIntegrationTest extends AbstractIntegrationTest {

    @Test
    void malformedJsonBodyIsA400WithEnvelope() throws Exception {
        String token = authenticatedUser();
        String applicationId = createApplication(token);

        MvcResult result = mockMvc.perform(
                        put("/api/v1/applications/" + applicationId + "/answers/totalDebtAmount")
                                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{ this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(header().exists("X-Correlation-Id"))
                .andReturn();
        assertSafeBody(result);
    }

    @Test
    void invalidUuidInPathIsA400NotA500() throws Exception {
        String token = authenticatedUser();

        MvcResult result = mockMvc.perform(get("/api/v1/applications/not-a-uuid")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid value for 'applicationId'"))
                .andReturn();
        assertSafeBody(result);
    }

    @Test
    void unknownPathIsA404WithEnvelope() throws Exception {
        String token = authenticatedUser();

        MvcResult result = mockMvc.perform(get("/api/v1/no-such-endpoint")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andReturn();
        assertSafeBody(result);
    }

    @Test
    void unsupportedMethodIsA405WithEnvelope() throws Exception {
        String token = authenticatedUser();
        String applicationId = createApplication(token);

        MvcResult result = mockMvc.perform(delete("/api/v1/applications/" + applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andReturn();
        assertSafeBody(result);
    }

    @Test
    void beanValidationFailuresListFieldDetails() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors.length()")
                        .value(org.hamcrest.Matchers.greaterThan(0)))
                .andReturn();
        assertSafeBody(result);
    }

    @Test
    void suppliedCorrelationIdIsEchoedInErrorResponses() throws Exception {
        String token = authenticatedUser();
        String correlationId = "test-corr-" + UUID.randomUUID();

        mockMvc.perform(get("/api/v1/applications/not-a-uuid")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("X-Correlation-Id", correlationId))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Correlation-Id", correlationId))
                .andExpect(jsonPath("$.correlationId").value(correlationId));
    }

    @Test
    void businessErrorsUseTheSameEnvelope() throws Exception {
        String token = authenticatedUser();

        MvcResult result = mockMvc.perform(get("/api/v1/applications/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andReturn();
        assertSafeBody(result);
    }

    /** No stack traces, exception class names, or framework/SQL detail in any error response. */
    private void assertSafeBody(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .doesNotContain("java.")
                .doesNotContain("Exception")
                .doesNotContain("at ru.adiaphora")
                .doesNotContainIgnoringCase("sql")
                .doesNotContainIgnoringCase("hibernate");
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
