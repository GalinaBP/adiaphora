package ru.adiaphora.platform.rules.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.questionnaire.api.QuestionnaireSnapshot;
import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;
import ru.adiaphora.platform.rules.domain.rules.BailiffsClosedGroundRule;
import ru.adiaphora.platform.rules.domain.rules.DebtAmountMissingRule;
import ru.adiaphora.platform.rules.domain.rules.MfcLowerBoundRule;
import ru.adiaphora.platform.rules.domain.rules.MfcUpperBoundRule;
import ru.adiaphora.platform.rules.domain.rules.MortgageManualReviewRule;
import ru.adiaphora.platform.rules.domain.rules.NoStatutoryGroundRule;
import ru.adiaphora.platform.rules.domain.rules.OldDebtGroundRule;
import ru.adiaphora.platform.rules.domain.rules.PriorBankruptcyFiveYearRule;
import ru.adiaphora.platform.rules.domain.rules.RecentPropertyTransactionManualReviewRule;
import ru.adiaphora.platform.rules.domain.rules.StatutoryGroundsMissingRule;
import ru.adiaphora.platform.rules.domain.rules.VulnerableCategoryGroundRule;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RuleEngineTest {

    /** Fixed "today" for the 5-year repeat-filing boundary rows: 2026-07-01. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-01T12:00:00Z"), ZoneOffset.UTC);

    private static List<BankruptcyRule> allRules() {
        return List.of(
                new DebtAmountMissingRule(),
                new StatutoryGroundsMissingRule(),
                new MfcLowerBoundRule(),
                new MfcUpperBoundRule(),
                new PriorBankruptcyFiveYearRule(FIXED_CLOCK),
                new NoStatutoryGroundRule(),
                new BailiffsClosedGroundRule(),
                new VulnerableCategoryGroundRule(),
                new OldDebtGroundRule(),
                new MortgageManualReviewRule(),
                new RecentPropertyTransactionManualReviewRule());
    }

    private final RuleEngine engine = new RuleEngine(allRules());

    private EngineResult evaluate(Map<String, String> answers) {
        return engine.evaluate(new RuleContext(
                new QuestionnaireSnapshot(UUID.randomUUID(), "v2", answers)));
    }

    /** A clean, fully-answered case on the "bailiffs closed the case" ground. */
    private Map<String, String> baseAnswers() {
        Map<String, String> answers = new HashMap<>();
        answers.put("totalDebtAmount", "100000");
        answers.put("previousBankruptcy", "false");
        answers.put("mfcStatutoryGrounds", "enforcement_ended");
        answers.put("bailiffsCaseClosedNoNew", "yes");
        answers.put("ownsMortgagedHome", "false");
        answers.put("recentPropertyTransaction", "none");
        return answers;
    }

    @Test
    void cleanCaseWithinBoundsIsMfcPreliminary() {
        EngineResult result = evaluate(baseAnswers());
        assertThat(result.route()).isEqualTo(BankruptcyRoute.MFC_PRELIMINARY);
        assertThat(result.manualReviewRequired()).isFalse();
        assertThat(result.missingInformation()).isEmpty();
    }

    /**
     * The AI-012 approved boundary cases (stage 1 of the eligibility flow, п. 1 ст. 223.2 127-ФЗ):
     * bounds are inclusive, so 25 000 and 1 000 000 stay on the MFC route while 24 999 and
     * 1 000 001 fall outside. Kopeck-precision rows guard the exact edge. The amount check is a hard
     * gate: it decides the route even when later-stage answers are missing.
     */
    @ParameterizedTest(name = "debt {0} -> {1}")
    @CsvSource({
            "1000,       NOT_CURRENTLY_RECOMMENDED",
            "24999,      NOT_CURRENTLY_RECOMMENDED",
            "24999.99,   NOT_CURRENTLY_RECOMMENDED",
            "25000,      MFC_PRELIMINARY",
            "25000.01,   MFC_PRELIMINARY",
            "100000,     MFC_PRELIMINARY",
            "999999.99,  MFC_PRELIMINARY",
            "1000000,    MFC_PRELIMINARY",
            "1000000.01, COURT_PRELIMINARY",
            "1000001,    COURT_PRELIMINARY",
            "2000000,    COURT_PRELIMINARY"
    })
    void approvedAmountBoundaryCases(String debtAmount, BankruptcyRoute expectedRoute) {
        Map<String, String> answers = baseAnswers();
        answers.put("totalDebtAmount", debtAmount);
        EngineResult result = evaluate(answers);
        assertThat(result.route()).isEqualTo(expectedRoute);
        assertThat(result.manualReviewRequired()).isFalse();
        assertThat(result.missingInformation()).isEmpty();
    }

    /** The amount gate is hard: an out-of-bounds debt decides the route with everything else unanswered. */
    @Test
    void amountGateDecidesEvenWithLaterStagesUnanswered() {
        EngineResult result = evaluate(Map.of("totalDebtAmount", "5000"));
        assertThat(result.route()).isEqualTo(BankruptcyRoute.NOT_CURRENTLY_RECOMMENDED);
        assertThat(result.manualReviewRequired()).isFalse();
    }

    /**
     * Stage 2 of the eligibility flow: repeat extrajudicial filing is barred for 5 years from the
     * end of the previous procedure (п. 8 ст. 223.2 127-ФЗ). With a fixed "today" of 2026-07-01 the
     * bound is inclusive: exactly 5 years ago (2021-07-01) passes, one day later fails.
     */
    @ParameterizedTest(name = "previous bankruptcy ended {0} -> {1}")
    @CsvSource({
            "2010-01-01, MFC_PRELIMINARY",
            "2021-06-30, MFC_PRELIMINARY",
            "2021-07-01, MFC_PRELIMINARY",
            "2021-07-02, COURT_PRELIMINARY",
            "2026-06-30, COURT_PRELIMINARY"
    })
    void fiveYearRepeatFilingBarBoundaryCases(String endedOn, BankruptcyRoute expectedRoute) {
        Map<String, String> answers = baseAnswers();
        answers.put("previousBankruptcy", "true");
        answers.put("previousBankruptcyEndedOn", endedOn);
        EngineResult result = evaluate(answers);
        assertThat(result.route()).isEqualTo(expectedRoute);
        assertThat(result.manualReviewRequired()).isFalse();
    }

    @Test
    void priorBankruptcyWithoutEndDateNeedsInformation() {
        Map<String, String> answers = baseAnswers();
        answers.put("previousBankruptcy", "true");
        EngineResult result = evaluate(answers);
        assertThat(result.route()).isEqualTo(BankruptcyRoute.INSUFFICIENT_INFORMATION);
        assertThat(result.missingInformation()).contains("MFC-PRIOR-BANKRUPTCY-5Y");
    }

    @ParameterizedTest(name = "missing {0} -> {1}")
    @CsvSource({
            "totalDebtAmount,     APPLICATION-DEBT-AMOUNT-MISSING",
            "previousBankruptcy,  MFC-PRIOR-BANKRUPTCY-5Y",
            "mfcStatutoryGrounds, APPLICATION-STATUTORY-GROUNDS-MISSING",
            "bailiffsCaseClosedNoNew, MFC-GROUND-BAILIFFS-CLOSED"
    })
    void missingAnswerRequiresInformation(String questionCode, String expectedRuleCode) {
        Map<String, String> answers = baseAnswers();
        answers.remove(questionCode);
        EngineResult result = evaluate(answers);
        assertThat(result.route()).isEqualTo(BankruptcyRoute.INSUFFICIENT_INFORMATION);
        assertThat(result.missingInformation()).contains(expectedRuleCode);
        assertThat(result.manualReviewRequired()).isFalse();
    }

    /**
     * Stage 3 of the eligibility flow (п. 1 ст. 223.2 127-ФЗ): the statutory-ground decision table.
     * Eligibility is OR across selected categories and AND across the conditions inside one
     * category; "none" selected alone routes to court, combined with real categories it is ignored;
     * a failed unified-child-benefit confirmation drops only that category; the old-debt ground has
     * no property condition; any "not sure" answer routes to manual review (check-your-documents).
     * Empty cells mean the question was not asked/answered for that scenario.
     */
    @ParameterizedTest(name = "[{index}] grounds={0} bailiffs={1} benefit={2} writ1y={3} property={4} writ7y={5} -> {6}")
    @CsvSource({
            // grounds,                            bailiffs, benefit,  writ1y,   property, writ7y,   expected route
            "enforcement_ended,                    yes,      ,         ,         ,         ,         MFC_PRELIMINARY",
            "enforcement_ended,                    no,       ,         ,         ,         ,         COURT_PRELIMINARY",
            "enforcement_ended,                    not_sure, ,         ,         ,         ,         MANUAL_REVIEW",
            "pensioner,                            ,         ,         yes,      no,       ,         MFC_PRELIMINARY",
            "pensioner,                            ,         ,         no,       no,       ,         COURT_PRELIMINARY",
            "pensioner,                            ,         ,         yes,      yes,      ,         COURT_PRELIMINARY",
            "pensioner,                            ,         ,         not_sure, no,       ,         MANUAL_REVIEW",
            "pensioner,                            ,         ,         yes,      not_sure, ,         MANUAL_REVIEW",
            "svo_participant,                      ,         ,         yes,      no,       ,         MFC_PRELIMINARY",
            "child_benefit,                        ,         yes,      yes,      no,       ,         MFC_PRELIMINARY",
            "child_benefit,                        ,         no,       ,         ,         ,         COURT_PRELIMINARY",
            "child_benefit,                        ,         not_sure, ,         ,         ,         MANUAL_REVIEW",
            "'pensioner,child_benefit',            ,         no,       yes,      no,       ,         MFC_PRELIMINARY",
            "'pensioner,svo_participant',          ,         ,         yes,      no,       ,         MFC_PRELIMINARY",
            "long_enforcement,                     ,         ,         ,         ,         yes,      MFC_PRELIMINARY",
            "long_enforcement,                     ,         ,         ,         ,         no,       COURT_PRELIMINARY",
            "long_enforcement,                     ,         ,         ,         ,         not_sure, MANUAL_REVIEW",
            "none,                                 ,         ,         ,         ,         ,         COURT_PRELIMINARY",
            "'none,enforcement_ended',             yes,      ,         ,         ,         ,         MFC_PRELIMINARY",
            "'enforcement_ended,long_enforcement', no,       ,         ,         ,         yes,      MFC_PRELIMINARY",
            "'enforcement_ended,long_enforcement', no,       ,         ,         ,         no,       COURT_PRELIMINARY"
    })
    void statutoryGroundDecisionTable(String grounds, String bailiffs, String benefit, String writ1y,
                                      String property, String writ7y, BankruptcyRoute expectedRoute) {
        Map<String, String> answers = new HashMap<>();
        answers.put("totalDebtAmount", "100000");
        answers.put("previousBankruptcy", "false");
        answers.put("mfcStatutoryGrounds", grounds);
        putIfPresent(answers, "bailiffsCaseClosedNoNew", bailiffs);
        putIfPresent(answers, "childBenefitConfirmed", benefit);
        putIfPresent(answers, "writUnpaidOverOneYear", writ1y);
        putIfPresent(answers, "ownsSellableProperty", property);
        putIfPresent(answers, "writIssuedOverSevenYears", writ7y);

        EngineResult result = evaluate(answers);
        assertThat(result.route()).isEqualTo(expectedRoute);
        assertThat(result.missingInformation()).isEmpty();
        assertThat(result.manualReviewRequired())
                .isEqualTo(expectedRoute == BankruptcyRoute.MANUAL_REVIEW);
    }

    /** The old-debt ground never asks the property question (пп. 4 п. 1 ст. 223.2 has no asset condition). */
    @Test
    void oldDebtGroundPassesWithoutPropertyAnswer() {
        Map<String, String> answers = new HashMap<>();
        answers.put("totalDebtAmount", "100000");
        answers.put("previousBankruptcy", "false");
        answers.put("mfcStatutoryGrounds", "long_enforcement");
        answers.put("writIssuedOverSevenYears", "yes");
        EngineResult result = evaluate(answers);
        assertThat(result.route()).isEqualTo(BankruptcyRoute.MFC_PRELIMINARY);
        assertThat(result.missingInformation()).isEmpty();
    }

    /** A qualifying ground carries the legal-basis citation for the result screen. */
    @Test
    void passedGroundCarriesLegalBasis() {
        EngineResult result = evaluate(baseAnswers());
        assertThat(result.evaluations())
                .anyMatch(e -> e.ruleCode().equals("MFC-GROUND-BAILIFFS-CLOSED")
                        && e.outcome() == RuleOutcome.PASSED
                        && "пп. 1 п. 1 ст. 223.2 Закона № 127-ФЗ".equals(e.legalBasis()));
    }

    @Test
    void amountFailureCarriesLegalBasis() {
        Map<String, String> answers = baseAnswers();
        answers.put("totalDebtAmount", "1000001");
        EngineResult result = evaluate(answers);
        assertThat(result.triggered())
                .anyMatch(e -> e.ruleCode().equals("MFC-AMOUNT-UPPER-BOUND")
                        && "п. 1 ст. 223.2 Закона № 127-ФЗ".equals(e.legalBasis()));
    }

    @ParameterizedTest(name = "{0}={1} -> {2}")
    @CsvSource({
            "ownsMortgagedHome,         true,   MANUAL-REVIEW-MORTGAGE",
            "recentPropertyTransaction, sold,   MANUAL-REVIEW-RECENT-PROPERTY-TRANSACTION",
            "recentPropertyTransaction, gifted, MANUAL-REVIEW-RECENT-PROPERTY-TRANSACTION"
    })
    void flaggedAnswerForcesManualReview(String questionCode, String answer, String expectedRuleCode) {
        Map<String, String> answers = baseAnswers();
        answers.put(questionCode, answer);
        EngineResult result = evaluate(answers);
        assertThat(result.route()).isEqualTo(BankruptcyRoute.MANUAL_REVIEW);
        assertThat(result.manualReviewRequired()).isTrue();
        assertThat(result.triggered())
                .anyMatch(e -> e.ruleCode().equals(expectedRuleCode));
    }

    @Test
    void sameInputProducesSameResult() {
        Map<String, String> answers = baseAnswers();
        answers.put("ownsMortgagedHome", "true");
        assertThat(evaluate(answers)).isEqualTo(evaluate(answers));
    }

    @Test
    void evaluationOrderDoesNotDependOnHowRulesWereRegistered() {
        List<BankruptcyRule> reversed = new ArrayList<>(allRules());
        Collections.reverse(reversed);
        RuleEngine reversedEngine = new RuleEngine(reversed);

        Map<String, String> answers = baseAnswers();
        answers.remove("totalDebtAmount");
        answers.remove("mfcStatutoryGrounds");

        EngineResult expected = engine.evaluate(new RuleContext(
                new QuestionnaireSnapshot(UUID.randomUUID(), "v2", answers)));
        EngineResult actual = reversedEngine.evaluate(new RuleContext(
                new QuestionnaireSnapshot(UUID.randomUUID(), "v2", answers)));

        assertThat(actual).isEqualTo(expected);
        assertThat(actual.missingInformation()).containsExactly(
                "APPLICATION-DEBT-AMOUNT-MISSING", "APPLICATION-STATUTORY-GROUNDS-MISSING");
    }

    @Test
    void blockingSeverityForcesManualReview() {
        List<BankruptcyRule> rules = new ArrayList<>(allRules());
        rules.add(new BankruptcyRule() {
            @Override
            public String code() {
                return "TEST-BLOCKING";
            }

            @Override
            public int order() {
                return 999;
            }

            @Override
            public RuleEvaluation evaluate(RuleContext context) {
                return new RuleEvaluation(code(), RuleOutcome.FAILED, RuleSeverity.BLOCKING,
                        "always blocks", "Blocked for testing.", true);
            }
        });

        EngineResult result = new RuleEngine(rules).evaluate(new RuleContext(
                new QuestionnaireSnapshot(UUID.randomUUID(), "v2", baseAnswers())));

        assertThat(result.route()).isEqualTo(BankruptcyRoute.MANUAL_REVIEW);
        assertThat(result.manualReviewRequired()).isTrue();
        assertThat(result.triggered())
                .anyMatch(e -> e.ruleCode().equals("TEST-BLOCKING") && e.blocksAutomaticDecision());
    }

    @Test
    void duplicateRuleCodesAreRejected() {
        List<BankruptcyRule> rules = new ArrayList<>(allRules());
        rules.add(new MfcLowerBoundRule());
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RuleEngine(rules))
                .withMessageContaining("MFC-AMOUNT-LOWER-BOUND");
    }

    private static void putIfPresent(Map<String, String> answers, String code, String value) {
        if (value != null && !value.isBlank()) {
            answers.put(code, value);
        }
    }
}
