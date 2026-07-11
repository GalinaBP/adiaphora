package ru.adiaphora.platform.rules.domain.rules;

import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;
import ru.adiaphora.platform.rules.domain.BankruptcyRule;
import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleInputs;

/**
 * Flags a missing statutory-grounds answer: the extrajudicial procedure requires at least one of the
 * grounds recognised by п. 1 ст. 223.2 127-ФЗ, so no automatic decision is possible without it.
 */
public class StatutoryGroundsMissingRule implements BankruptcyRule {

    @Override
    public String code() {
        return "APPLICATION-STATUTORY-GROUNDS-MISSING";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public RuleEvaluation evaluate(RuleContext context) {
        if (context.multi(RuleInputs.MFC_STATUTORY_GROUNDS).isEmpty()) {
            return new RuleEvaluation(code(), RuleOutcome.NEEDS_INFORMATION, RuleSeverity.WARNING,
                    "mfcStatutoryGrounds not provided",
                    "Отметьте, какие из категорий для внесудебного банкротства к вам относятся.", true);
        }
        return new RuleEvaluation(code(), RuleOutcome.PASSED, RuleSeverity.INFO,
                "mfcStatutoryGrounds present", null, false);
    }
}
