package ru.adiaphora.platform.rules.domain.rules;

import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;
import ru.adiaphora.platform.rules.domain.BankruptcyRule;
import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleInputs;

/**
 * Block C of the category questionnaire: the enforcement document was issued 7+ years ago, was
 * presented for collection, and the debt is still not fully paid — пп. 4 п. 1 ст. 223.2 127-ФЗ.
 * Deliberately has no property-ownership check: the law imposes no asset condition for this ground,
 * unlike the pensioner/child-benefit/SVO block.
 */
public class OldDebtGroundRule implements BankruptcyRule {

    @Override
    public String code() {
        return "MFC-GROUND-OLD-DEBT";
    }

    @Override
    public int order() {
        return 110;
    }

    @Override
    public RuleEvaluation evaluate(RuleContext context) {
        if (!context.multi(RuleInputs.MFC_STATUTORY_GROUNDS).contains(RuleInputs.GROUND_LONG_ENFORCEMENT)) {
            return new RuleEvaluation(code(), RuleOutcome.NOT_APPLICABLE, RuleSeverity.INFO,
                    "old-debt ground not selected", null, false);
        }
        return TriStateAnswer.read(context, RuleInputs.WRIT_ISSUED_OVER_SEVEN_YEARS)
                .map(this::classify)
                .orElseGet(() -> new RuleEvaluation(code(), RuleOutcome.NEEDS_INFORMATION,
                        RuleSeverity.WARNING, "old-debt follow-up unanswered",
                        "Ответьте, выдан ли исполнительный документ не менее семи лет назад и "
                                + "предъявлялся ли он к взысканию.", true));
    }

    private RuleEvaluation classify(TriStateAnswer answer) {
        return switch (answer) {
            case YES -> new RuleEvaluation(code(), RuleOutcome.PASSED, RuleSeverity.INFO,
                    "writ issued 7+ years ago, presented, debt unpaid",
                    "Основание подтверждено: исполнительный документ выдан не менее семи лет назад, "
                            + "предъявлялся к взысканию, долг не погашен.",
                    false, RuleInputs.BASIS_OLD_DEBT);
            case NO -> new RuleEvaluation(code(), RuleOutcome.FAILED, RuleSeverity.INFO,
                    "old-debt conditions not met",
                    "Основание «долг взыскивают не менее семи лет» не подтверждено.",
                    false, RuleInputs.BASIS_OLD_DEBT);
            case NOT_SURE -> new RuleEvaluation(code(), RuleOutcome.FAILED, RuleSeverity.MANUAL_REVIEW,
                    "old-debt follow-up answered not_sure",
                    "Посмотрите дату в постановлении пристава о возбуждении производства (не дату "
                            + "решения суда) или выписку из банка, если деньги списывали напрямую.",
                    true, RuleInputs.BASIS_OLD_DEBT);
        };
    }
}
