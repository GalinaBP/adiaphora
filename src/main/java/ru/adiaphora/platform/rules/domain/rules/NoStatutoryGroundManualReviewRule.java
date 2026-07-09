package ru.adiaphora.platform.rules.domain.rules;

import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;
import ru.adiaphora.platform.rules.domain.BankruptcyRule;
import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleInputs;

/**
 * Routes cases where none of the statutory grounds for the extrajudicial (MFC) procedure applies to
 * manual review: without a ground the MFC route is unavailable by law, but whether one of the grounds
 * can still be established is a lawyer's call. Placeholder.
 */
public class NoStatutoryGroundManualReviewRule implements BankruptcyRule {

    @Override
    public String code() {
        return "MANUAL-REVIEW-NO-STATUTORY-GROUND";
    }

    @Override
    public int order() {
        return 80;
    }

    @Override
    public RuleEvaluation evaluate(RuleContext context) {
        return context.text(RuleInputs.MFC_STATUTORY_GROUND)
                .map(value -> RuleInputs.GROUND_NONE.equals(value)
                        ? new RuleEvaluation(code(), RuleOutcome.FAILED, RuleSeverity.MANUAL_REVIEW,
                        "no statutory ground selected",
                        "Ни одно из оснований для внесудебного банкротства не подтверждено — "
                                + "ситуацию должен оценить специалист.", true)
                        : new RuleEvaluation(code(), RuleOutcome.PASSED, RuleSeverity.INFO,
                        "statutory ground: " + value, null, false))
                .orElseGet(() -> new RuleEvaluation(code(), RuleOutcome.NOT_APPLICABLE, RuleSeverity.INFO,
                        "statutory-ground question unanswered", null, false));
    }
}
