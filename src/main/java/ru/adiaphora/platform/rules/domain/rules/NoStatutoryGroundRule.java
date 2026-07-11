package ru.adiaphora.platform.rules.domain.rules;

import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;
import ru.adiaphora.platform.rules.domain.BankruptcyRule;
import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleInputs;

import java.util.Set;

/**
 * Stage-3.1 gate: "none of these" selected alone means no statutory ground for the extrajudicial
 * procedure applies — the MFC route is unavailable and the judicial procedure is the alternative
 * (п. 1 ст. 223.2 127-ФЗ). When "none" is combined with real categories it is ignored per the
 * approved flow, and the category rules decide.
 */
public class NoStatutoryGroundRule implements BankruptcyRule {

    @Override
    public String code() {
        return "MFC-NO-STATUTORY-GROUND";
    }

    @Override
    public int order() {
        return 80;
    }

    @Override
    public RuleEvaluation evaluate(RuleContext context) {
        Set<String> grounds = context.multi(RuleInputs.MFC_STATUTORY_GROUNDS);
        if (grounds.isEmpty()) {
            return new RuleEvaluation(code(), RuleOutcome.NOT_APPLICABLE, RuleSeverity.INFO,
                    "statutory-grounds question unanswered", null, false);
        }
        boolean onlyNone = grounds.stream().allMatch(RuleInputs.GROUND_NONE::equals);
        if (onlyNone) {
            return new RuleEvaluation(code(), RuleOutcome.FAILED, RuleSeverity.INFO,
                    "no statutory ground selected",
                    "Ни одно из оснований для внесудебного банкротства не подходит — процедура через "
                            + "МФЦ недоступна; рассмотрите судебное банкротство.",
                    false, RuleInputs.BASIS_STATUTORY_GROUNDS);
        }
        return new RuleEvaluation(code(), RuleOutcome.PASSED, RuleSeverity.INFO,
                "at least one statutory ground selected", null, false);
    }
}
