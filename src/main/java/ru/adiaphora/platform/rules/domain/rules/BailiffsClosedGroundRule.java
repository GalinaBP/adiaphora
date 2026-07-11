package ru.adiaphora.platform.rules.domain.rules;

import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;
import ru.adiaphora.platform.rules.domain.BankruptcyRule;
import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleInputs;

/**
 * Block A of the category questionnaire: the bailiffs previously closed an enforcement case for lack
 * of assets (п. 4 ч. 1 ст. 46 Закона об исполнительном производстве) and no new case has been opened
 * since — the "ordinary debtor" ground, пп. 1 п. 1 ст. 223.2 127-ФЗ.
 */
public class BailiffsClosedGroundRule implements BankruptcyRule {

    @Override
    public String code() {
        return "MFC-GROUND-BAILIFFS-CLOSED";
    }

    @Override
    public int order() {
        return 90;
    }

    @Override
    public RuleEvaluation evaluate(RuleContext context) {
        if (!context.multi(RuleInputs.MFC_STATUTORY_GROUNDS).contains(RuleInputs.GROUND_ENFORCEMENT_ENDED)) {
            return new RuleEvaluation(code(), RuleOutcome.NOT_APPLICABLE, RuleSeverity.INFO,
                    "bailiffs-closed ground not selected", null, false);
        }
        return TriStateAnswer.read(context, RuleInputs.BAILIFFS_CASE_CLOSED_NO_NEW)
                .map(this::classify)
                .orElseGet(() -> new RuleEvaluation(code(), RuleOutcome.NEEDS_INFORMATION,
                        RuleSeverity.WARNING, "bailiffs-closed follow-up unanswered",
                        "Ответьте, оканчивал ли пристав исполнительное производство из-за отсутствия "
                                + "имущества и не открывались ли новые производства.", true));
    }

    private RuleEvaluation classify(TriStateAnswer answer) {
        return switch (answer) {
            case YES -> new RuleEvaluation(code(), RuleOutcome.PASSED, RuleSeverity.INFO,
                    "bailiffs closed the case, no new case since",
                    "Основание подтверждено: пристав окончил исполнительное производство из-за "
                            + "отсутствия имущества, новых производств нет.",
                    false, RuleInputs.BASIS_BAILIFFS_CLOSED);
            case NO -> new RuleEvaluation(code(), RuleOutcome.FAILED, RuleSeverity.INFO,
                    "bailiffs-closed conditions not met",
                    "Основание «пристав окончил производство» не подтверждено: производство не "
                            + "оканчивалось по п. 4 ч. 1 ст. 46 или после него открывались новые.",
                    false, RuleInputs.BASIS_BAILIFFS_CLOSED);
            case NOT_SURE -> new RuleEvaluation(code(), RuleOutcome.FAILED, RuleSeverity.MANUAL_REVIEW,
                    "bailiffs-closed follow-up answered not_sure",
                    "Проверьте статус исполнительных производств: fssp.gov.ru → «Банк данных "
                            + "исполнительных производств» — по ФИО и дате рождения. Закрытое дело "
                            + "будет со статусом «окончено» и причиной.",
                    true, RuleInputs.BASIS_BAILIFFS_CLOSED);
        };
    }
}
