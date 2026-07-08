package ru.adiaphora.platform.rules.domain;

import org.junit.jupiter.api.Test;
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

import java.math.BigDecimal;
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

    @Test
    void missingDebtRequiresInformation() {
        Map<String, String> answers = baseAnswers();
        answers.remove("totalDebtAmount");
        EngineResult result = evaluate(answers);
        assertThat(result.route()).isEqualTo(BankruptcyRoute.INSUFFICIENT_INFORMATION);
        assertThat(result.missingInformation()).contains("APPLICATION-DEBT-AMOUNT-MISSING");
        assertThat(result.manualReviewRequired()).isFalse();
    }

    @Test
    void mortgageForcesManualReview() {
        Map<String, String> answers = baseAnswers();
        answers.put("ownsMortgagedHome", "true");
        EngineResult result = evaluate(answers);
        assertThat(result.route()).isEqualTo(BankruptcyRoute.MANUAL_REVIEW);
        assertThat(result.manualReviewRequired()).isTrue();
    }

    @Test
    void debtAboveUpperBoundRoutesToCourt() {
        Map<String, String> answers = baseAnswers();
        answers.put("totalDebtAmount", "2000000");
        EngineResult result = evaluate(answers);
        assertThat(result.route()).isEqualTo(BankruptcyRoute.COURT_PRELIMINARY);
    }

    @Test
    void debtBelowLowerBoundIsNotRecommended() {
        Map<String, String> answers = baseAnswers();
        answers.put("totalDebtAmount", "1000");
        EngineResult result = evaluate(answers);
        assertThat(result.route()).isEqualTo(BankruptcyRoute.NOT_CURRENTLY_RECOMMENDED);
    }

    @Test
    void debtExactlyAtLowerBoundIsMfcPreliminary() {
        Map<String, String> answers = baseAnswers();
        answers.put("totalDebtAmount", RuleInputs.MFC_LOWER_BOUND.toPlainString());
        assertThat(evaluate(answers).route()).isEqualTo(BankruptcyRoute.MFC_PRELIMINARY);
    }

    @Test
    void debtJustBelowLowerBoundIsNotRecommended() {
        Map<String, String> answers = baseAnswers();
        answers.put("totalDebtAmount", RuleInputs.MFC_LOWER_BOUND.subtract(new BigDecimal("0.01")).toPlainString());
        assertThat(evaluate(answers).route()).isEqualTo(BankruptcyRoute.NOT_CURRENTLY_RECOMMENDED);
    }

    @Test
    void debtExactlyAtUpperBoundIsMfcPreliminary() {
        Map<String, String> answers = baseAnswers();
        answers.put("totalDebtAmount", RuleInputs.MFC_UPPER_BOUND.toPlainString());
        assertThat(evaluate(answers).route()).isEqualTo(BankruptcyRoute.MFC_PRELIMINARY);
    }

    @Test
    void debtJustAboveUpperBoundRoutesToCourt() {
        Map<String, String> answers = baseAnswers();
        answers.put("totalDebtAmount", RuleInputs.MFC_UPPER_BOUND.add(new BigDecimal("0.01")).toPlainString());
        assertThat(evaluate(answers).route()).isEqualTo(BankruptcyRoute.COURT_PRELIMINARY);
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
