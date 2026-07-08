package ru.adiaphora.platform.rules;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.adiaphora.platform.support.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVP-1: the anonymous eligibility estimate must produce the same outcomes as the authenticated
 * evaluation at the AI-012 approved boundary amounts, work without any token, and persist nothing —
 * no application and no evaluation row may appear for an anonymous estimate.
 */
class EligibilityEstimateIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @ParameterizedTest(name = "debt {0} -> {1}")
    @CsvSource({
            "24999,   AMOUNT_OUT_OF_RANGE, NOT_CURRENTLY_RECOMMENDED",
            "25000,   MFC_ELIGIBLE,        MFC_PRELIMINARY",
            "1000000, MFC_ELIGIBLE,        MFC_PRELIMINARY",
            "1000001, AMOUNT_OUT_OF_RANGE, COURT_PRELIMINARY"
    })
    void approvedBoundaryAmountsMatchTheAuthenticatedEngine(String debt, String verdict, String route)
            throws Exception {
        mockMvc.perform(post("/api/v1/eligibility/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completeAnswers(debt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value(verdict))
                .andExpect(jsonPath("$.route").value(route))
                .andExpect(jsonPath("$.rulesetVersion").isNotEmpty());
    }

    @Test
    void mortgageTriggersManualReviewVerdict() throws Exception {
        mockMvc.perform(post("/api/v1/eligibility/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalDebtAmount\":500000,\"hasRegularIncome\":true,"
                                + "\"ownsMortgagedHome\":true,\"previousBankruptcy\":false,"
                                + "\"recentPropertyTransaction\":\"none\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("MANUAL_REVIEW"))
                .andExpect(jsonPath("$.messages").isNotEmpty());
    }

    @Test
    void missingAnswersYieldNeedsInformation() throws Exception {
        mockMvc.perform(post("/api/v1/eligibility/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NEEDS_INFORMATION"))
                .andExpect(jsonPath("$.missingInformation").isNotEmpty());
    }

    @Test
    void invalidValuesAreRejectedWithValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/eligibility/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalDebtAmount\":-1,\"recentPropertyTransaction\":\"burned\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void anonymousEstimatePersistsNothing() throws Exception {
        Integer applicationsBefore = count("bankruptcy_applications");
        Integer evaluationsBefore = count("rule_evaluations");

        mockMvc.perform(post("/api/v1/eligibility/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completeAnswers("100000")))
                .andExpect(status().isOk());

        assertThat(count("bankruptcy_applications")).isEqualTo(applicationsBefore);
        assertThat(count("rule_evaluations")).isEqualTo(evaluationsBefore);
    }

    private Integer count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private String completeAnswers(String debt) {
        return "{\"totalDebtAmount\":" + debt + ",\"hasRegularIncome\":true,"
                + "\"ownsMortgagedHome\":false,\"previousBankruptcy\":false,"
                + "\"recentPropertyTransaction\":\"none\"}";
    }
}
