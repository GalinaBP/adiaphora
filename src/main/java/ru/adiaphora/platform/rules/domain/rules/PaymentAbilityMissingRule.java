package ru.adiaphora.platform.rules.domain.rules;

import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;
import ru.adiaphora.platform.rules.domain.BankruptcyRule;
import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleInputs;

/** Flags missing payment-ability information (regular income). Placeholder. */
public class PaymentAbilityMissingRule implements BankruptcyRule {

    @Override
    public String code() {
        return "APPLICATION-PAYMENT-ABILITY-MISSING";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public RuleEvaluation evaluate(RuleContext context) {
        if (!context.isAnswered(RuleInputs.HAS_REGULAR_INCOME)) {
            return new RuleEvaluation(code(), RuleOutcome.NEEDS_INFORMATION, RuleSeverity.WARNING,
                    "hasRegularIncome not provided",
                    "Укажите, есть ли у вас регулярный доход.", true);
        }
        return new RuleEvaluation(code(), RuleOutcome.PASSED, RuleSeverity.INFO,
                "hasRegularIncome present", null, false);
    }
}
