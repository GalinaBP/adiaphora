package ru.adiaphora.platform.rules.domain.rules;

import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;
import ru.adiaphora.platform.rules.domain.BankruptcyRule;
import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleInputs;

import java.math.BigDecimal;

/** Checks the debt is at or below the MFC (extrajudicial) upper bound. Placeholder threshold. */
public class MfcUpperBoundRule implements BankruptcyRule {

    @Override
    public String code() {
        return "MFC-AMOUNT-UPPER-BOUND";
    }

    @Override
    public RuleEvaluation evaluate(RuleContext context) {
        BigDecimal debt = context.money(RuleInputs.TOTAL_DEBT_AMOUNT).orElse(null);
        if (debt == null) {
            return new RuleEvaluation(code(), RuleOutcome.NOT_APPLICABLE, RuleSeverity.INFO,
                    "debt amount unknown", null, false);
        }
        if (debt.compareTo(RuleInputs.MFC_UPPER_BOUND) > 0) {
            return new RuleEvaluation(code(), RuleOutcome.FAILED, RuleSeverity.INFO,
                    "debt " + debt + " above MFC upper bound " + RuleInputs.MFC_UPPER_BOUND,
                    "Your debt exceeds the out-of-court (MFC) maximum; the court route is likely.", false);
        }
        return new RuleEvaluation(code(), RuleOutcome.PASSED, RuleSeverity.INFO,
                "debt at or below MFC upper bound", null, false);
    }
}
