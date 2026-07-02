package ru.adiaphora.platform.rules.domain;

import org.junit.jupiter.api.Test;
import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.questionnaire.api.QuestionnaireSnapshot;
import ru.adiaphora.platform.rules.domain.rules.DebtAmountMissingRule;
import ru.adiaphora.platform.rules.domain.rules.MfcLowerBoundRule;
import ru.adiaphora.platform.rules.domain.rules.MfcUpperBoundRule;
import ru.adiaphora.platform.rules.domain.rules.MortgageManualReviewRule;
import ru.adiaphora.platform.rules.domain.rules.PaymentAbilityMissingRule;
import ru.adiaphora.platform.rules.domain.rules.PreviousBankruptcyManualReviewRule;
import ru.adiaphora.platform.rules.domain.rules.RecentPropertyTransactionManualReviewRule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineTest {

    private final RuleEngine engine = new RuleEngine(List.of(
            new DebtAmountMissingRule(),
            new PaymentAbilityMissingRule(),
            new MfcLowerBoundRule(),
            new MfcUpperBoundRule(),
            new MortgageManualReviewRule(),
            new PreviousBankruptcyManualReviewRule(),
            new RecentPropertyTransactionManualReviewRule()));

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
}
