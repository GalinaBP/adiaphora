package ru.adiaphora.platform.rules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.adiaphora.platform.support.AbstractIntegrationTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The anonymous eligibility check implements the staged flow (127-ФЗ, ст. 223.2): amount gate,
 * 5-year repeat-filing bar, multi-select statutory grounds with follow-up blocks. It must work
 * without any token, match the authenticated engine at the AI-012 approved boundary amounts, return
 * legal-basis citations, and persist one session row per check — but never an application or an
 * evaluation row.
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
    void eligibleCheckListsQualifyingGroundsWithCitations() throws Exception {
        mockMvc.perform(post("/api/v1/eligibility/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completeAnswers("500000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("MFC_ELIGIBLE"))
                .andExpect(jsonPath("$.qualifyingGrounds[0].code").value("MFC-GROUND-BAILIFFS-CLOSED"))
                .andExpect(jsonPath("$.qualifyingGrounds[0].legalBasis")
                        .value("пп. 1 п. 1 ст. 223.2 Закона № 127-ФЗ"))
                .andExpect(jsonPath("$.citations").isNotEmpty());
    }

    @Test
    void priorBankruptcyInsideFiveYearBarRoutesToJudicial() throws Exception {
        String endedOn = LocalDate.now().minusYears(1).toString();
        mockMvc.perform(post("/api/v1/eligibility/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalDebtAmount\":500000,\"previousBankruptcy\":true,"
                                + "\"previousBankruptcyEndedOn\":\"" + endedOn + "\","
                                + "\"mfcStatutoryGrounds\":[\"enforcement_ended\"],"
                                + "\"bailiffsCaseClosedNoNew\":\"yes\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("JUDICIAL_ROUTE"))
                .andExpect(jsonPath("$.route").value("COURT_PRELIMINARY"))
                .andExpect(jsonPath("$.citations").isNotEmpty());
    }

    @Test
    void priorBankruptcyOlderThanFiveYearsStaysEligible() throws Exception {
        String endedOn = LocalDate.now().minusYears(6).toString();
        mockMvc.perform(post("/api/v1/eligibility/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalDebtAmount\":500000,\"previousBankruptcy\":true,"
                                + "\"previousBankruptcyEndedOn\":\"" + endedOn + "\","
                                + "\"mfcStatutoryGrounds\":[\"enforcement_ended\"],"
                                + "\"bailiffsCaseClosedNoNew\":\"yes\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("MFC_ELIGIBLE"));
    }

    @Test
    void noneAloneRoutesToJudicial() throws Exception {
        mockMvc.perform(post("/api/v1/eligibility/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalDebtAmount\":500000,\"previousBankruptcy\":false,"
                                + "\"mfcStatutoryGrounds\":[\"none\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("JUDICIAL_ROUTE"))
                .andExpect(jsonPath("$.messages").isNotEmpty());
    }

    @Test
    void notSureAnswerRoutesToManualReview() throws Exception {
        mockMvc.perform(post("/api/v1/eligibility/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalDebtAmount\":500000,\"previousBankruptcy\":false,"
                                + "\"mfcStatutoryGrounds\":[\"enforcement_ended\"],"
                                + "\"bailiffsCaseClosedNoNew\":\"not_sure\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("MANUAL_REVIEW"))
                .andExpect(jsonPath("$.messages").isNotEmpty());
    }

    @Test
    void orAcrossCategoriesOneFailingOnePassingIsEligible() throws Exception {
        mockMvc.perform(post("/api/v1/eligibility/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalDebtAmount\":500000,\"previousBankruptcy\":false,"
                                + "\"mfcStatutoryGrounds\":[\"enforcement_ended\",\"long_enforcement\"],"
                                + "\"bailiffsCaseClosedNoNew\":\"no\","
                                + "\"writIssuedOverSevenYears\":\"yes\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("MFC_ELIGIBLE"))
                .andExpect(jsonPath("$.qualifyingGrounds[0].code").value("MFC-GROUND-OLD-DEBT"));
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
                        .content("{\"totalDebtAmount\":-1,"
                                + "\"mfcStatutoryGrounds\":[\"lottery\"],"
                                + "\"bailiffsCaseClosedNoNew\":\"maybe\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void checkSessionIsPersistedButNoApplicationOrEvaluationAppears() throws Exception {
        Integer checksBefore = count("eligibility_checks");
        Integer applicationsBefore = count("bankruptcy_applications");
        Integer evaluationsBefore = count("rule_evaluations");

        mockMvc.perform(post("/api/v1/eligibility/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completeAnswers("100000")))
                .andExpect(status().isOk());

        assertThat(count("eligibility_checks")).isEqualTo(checksBefore + 1);
        assertThat(count("bankruptcy_applications")).isEqualTo(applicationsBefore);
        assertThat(count("rule_evaluations")).isEqualTo(evaluationsBefore);

        String verdict = jdbc.queryForObject(
                "SELECT verdict FROM eligibility_checks ORDER BY created_at DESC LIMIT 1", String.class);
        assertThat(verdict).isEqualTo("MFC_ELIGIBLE");
    }

    private Integer count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private String completeAnswers(String debt) {
        return "{\"totalDebtAmount\":" + debt + ",\"previousBankruptcy\":false,"
                + "\"mfcStatutoryGrounds\":[\"enforcement_ended\"],"
                + "\"bailiffsCaseClosedNoNew\":\"yes\"}";
    }
}
