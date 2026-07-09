package ru.adiaphora.platform.rules.domain.rules;

import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;
import ru.adiaphora.platform.rules.domain.BankruptcyRule;
import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleInputs;

/**
 * Flags a missing statutory-ground answer: the extrajudicial procedure requires at least one of the
 * grounds recognised by law, so no automatic decision is possible without it. Placeholder.
 */
public class StatutoryGroundMissingRule implements BankruptcyRule {

    @Override
    public String code() {
        return "APPLICATION-STATUTORY-GROUND-MISSING";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public RuleEvaluation evaluate(RuleContext context) {
        if (!context.isAnswered(RuleInputs.MFC_STATUTORY_GROUND)) {
            return new RuleEvaluation(code(), RuleOutcome.NEEDS_INFORMATION, RuleSeverity.WARNING,
                    "mfcStatutoryGround not provided",
                    "Укажите, относитесь ли вы к одной из категорий для внесудебного банкротства.", true);
        }
        return new RuleEvaluation(code(), RuleOutcome.PASSED, RuleSeverity.INFO,
                "mfcStatutoryGround present", null, false);
    }
}
