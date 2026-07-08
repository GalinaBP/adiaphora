package ru.adiaphora.platform.rules.domain.rules;

import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;
import ru.adiaphora.platform.rules.domain.BankruptcyRule;
import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleInputs;

/** Flags a missing total-debt amount, which blocks any automatic decision. Placeholder. */
public class DebtAmountMissingRule implements BankruptcyRule {

    @Override
    public String code() {
        return "APPLICATION-DEBT-AMOUNT-MISSING";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public RuleEvaluation evaluate(RuleContext context) {
        if (context.money(RuleInputs.TOTAL_DEBT_AMOUNT).isEmpty()) {
            return new RuleEvaluation(code(), RuleOutcome.NEEDS_INFORMATION, RuleSeverity.WARNING,
                    "totalDebtAmount not provided", "Укажите общую сумму долга.", true);
        }
        return new RuleEvaluation(code(), RuleOutcome.PASSED, RuleSeverity.INFO,
                "totalDebtAmount present", null, false);
    }
}
