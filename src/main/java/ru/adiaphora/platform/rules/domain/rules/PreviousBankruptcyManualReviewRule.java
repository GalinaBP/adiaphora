package ru.adiaphora.platform.rules.domain.rules;

import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;
import ru.adiaphora.platform.rules.domain.BankruptcyRule;
import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleInputs;

/** Routes cases with a prior bankruptcy to manual review. Placeholder. */
public class PreviousBankruptcyManualReviewRule implements BankruptcyRule {

    @Override
    public String code() {
        return "MANUAL-REVIEW-PREVIOUS-BANKRUPTCY";
    }

    @Override
    public int order() {
        return 60;
    }

    @Override
    public RuleEvaluation evaluate(RuleContext context) {
        return context.bool(RuleInputs.PREVIOUS_BANKRUPTCY)
                .map(previous -> previous
                        ? new RuleEvaluation(code(), RuleOutcome.FAILED, RuleSeverity.MANUAL_REVIEW,
                        "previous bankruptcy declared",
                        "A previous bankruptcy requires manual legal review.", true)
                        : new RuleEvaluation(code(), RuleOutcome.PASSED, RuleSeverity.INFO,
                        "no previous bankruptcy", null, false))
                .orElseGet(() -> new RuleEvaluation(code(), RuleOutcome.NOT_APPLICABLE, RuleSeverity.INFO,
                        "previous-bankruptcy question unanswered", null, false));
    }
}
