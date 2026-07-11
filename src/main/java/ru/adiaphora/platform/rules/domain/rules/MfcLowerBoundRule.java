package ru.adiaphora.platform.rules.domain.rules;

import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;
import ru.adiaphora.platform.rules.domain.BankruptcyRule;
import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleInputs;

import java.math.BigDecimal;

/** Checks the debt is at or above the MFC (extrajudicial) lower bound. Placeholder threshold. */
public class MfcLowerBoundRule implements BankruptcyRule {

    @Override
    public String code() {
        return "MFC-AMOUNT-LOWER-BOUND";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public RuleEvaluation evaluate(RuleContext context) {
        BigDecimal debt = context.money(RuleInputs.TOTAL_DEBT_AMOUNT).orElse(null);
        if (debt == null) {
            return notApplicable();
        }
        if (debt.compareTo(RuleInputs.MFC_LOWER_BOUND) < 0) {
            return new RuleEvaluation(code(), RuleOutcome.FAILED, RuleSeverity.INFO,
                    "debt " + debt + " below MFC lower bound " + RuleInputs.MFC_LOWER_BOUND,
                    "Сумма долга ниже минимального порога внесудебного банкротства (25 000 ₽); "
                            + "рассмотрите судебную процедуру.",
                    false, RuleInputs.BASIS_DEBT_BOUNDS);
        }
        return new RuleEvaluation(code(), RuleOutcome.PASSED, RuleSeverity.INFO,
                "debt at or above MFC lower bound", null, false);
    }

    private RuleEvaluation notApplicable() {
        return new RuleEvaluation(code(), RuleOutcome.NOT_APPLICABLE, RuleSeverity.INFO,
                "debt amount unknown", null, false);
    }
}
