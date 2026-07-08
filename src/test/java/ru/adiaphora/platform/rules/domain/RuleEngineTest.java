package ru.adiaphora.platform.rules.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.questionnaire.api.QuestionnaireSnapshot;
import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;
import ru.adiaphora.platform.rules.domain.rules.DebtAmountMissingRule;
import ru.adiaphora.platform.rules.domain.rules.MfcLowerBoundRule;
import ru.adiaphora.platform.rules.domain.rules.MfcUpperBoundRule;
import ru.adiaphora.platform.rules.domain.rules.MortgageManualReviewRule;
import ru.adiaphora.platform.rules.domain.rules.PaymentAbilityMissingRule;
import ru.adiaphora.platform.rules.domain.rules.PreviousBankruptcyManualReviewRule;
import ru.adiaphora.platform.rules.domain.rules.RecentPropertyTransactionManualReviewRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RuleEngineTest {

    private static List<BankruptcyRule> allRules() {
        return List.of(
                new DebtAmountMissingRule(),
                new PaymentAbilityMissingRule(),
                new MfcLowerBoundRule(),
                new MfcUpperBoundRule(),
                new MortgageManualReviewRule(),
                new PreviousBankruptcyManualReviewRule(),
                new RecentPropertyTransactionManualReviewRule());
    }

    private final RuleEngine engine = new RuleEngine(allRules());

    private EngineResult evaluate(Map<String, String> answers) {
        return engine.evaluate(new RuleContext(
                new QuestionnaireSnapshot(UUID.randomUUID(), "v1", answers)));
    }

    private Map<String, String> baseAnswers() {
        Map<String, String> answers = new HashMap<>();
        answers.put("totalDebtAmount", "100000");
        answers.put("hasRegularIncome", "true");
        answers.put("ownsMortgagedHome", "false");
        answers.put("previousBankruptcy", "false");
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
     * The AI-012 approved boundary cases: bounds are inclusive, so 25 000 and 1 000 000 stay on the
     * MFC route while 24 999 and 1 000 001 fall outside. Kopeck-precision rows guard the exact edge.
     * Expected results pending lawyer approval (placeholder thresholds).
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

    @ParameterizedTest(name = "missing {0} -> {1}")
    @CsvSource({
            "totalDebtAmount,  APPLICATION-DEBT-AMOUNT-MISSING",
            "hasRegularIncome, APPLICATION-PAYMENT-ABILITY-MISSING"
    })
    void missingAnswerRequiresInformation(String questionCode, String expectedRuleCode) {
        Map<String, String> answers = baseAnswers();
        answers.remove(questionCode);
        EngineResult result = evaluate(answers);
        assertThat(result.route()).isEqualTo(BankruptcyRoute.INSUFFICIENT_INFORMATION);
        assertThat(result.missingInformation()).contains(expectedRuleCode);
        assertThat(result.manualReviewRequired()).isFalse();
    }

    @ParameterizedTest(name = "{0}={1} -> {2}")
    @CsvSource({
            "ownsMortgagedHome,         true,   MANUAL-REVIEW-MORTGAGE",
            "previousBankruptcy,        true,   MANUAL-REVIEW-PREVIOUS-BANKRUPTCY",
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
        answers.remove("hasRegularIncome");

        EngineResult expected = engine.evaluate(new RuleContext(
                new QuestionnaireSnapshot(UUID.randomUUID(), "v1", answers)));
        EngineResult actual = reversedEngine.evaluate(new RuleContext(
                new QuestionnaireSnapshot(UUID.randomUUID(), "v1", answers)));

        assertThat(actual).isEqualTo(expected);
        assertThat(actual.missingInformation()).containsExactly(
                "APPLICATION-DEBT-AMOUNT-MISSING", "APPLICATION-PAYMENT-ABILITY-MISSING");
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
                new QuestionnaireSnapshot(UUID.randomUUID(), "v1", baseAnswers())));

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
}
