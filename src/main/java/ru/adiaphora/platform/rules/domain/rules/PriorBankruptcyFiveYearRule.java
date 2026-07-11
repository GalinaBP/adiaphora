package ru.adiaphora.platform.rules.domain.rules;

import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;
import ru.adiaphora.platform.rules.domain.BankruptcyRule;
import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleInputs;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Stage-2 hard gate: repeat extrajudicial filing is barred for 5 years from the date the previous
 * bankruptcy procedure (out-of-court or judicial) was completed or terminated
 * (п. 8 ст. 223.2 127-ФЗ). The bound is inclusive: exactly 5 years ago passes.
 */
public class PriorBankruptcyFiveYearRule implements BankruptcyRule {

    private final Clock clock;

    public PriorBankruptcyFiveYearRule(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String code() {
        return "MFC-PRIOR-BANKRUPTCY-5Y";
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public RuleEvaluation evaluate(RuleContext context) {
        return context.bool(RuleInputs.PREVIOUS_BANKRUPTCY)
                .map(previous -> previous ? evaluateEndedDate(context)
                        : new RuleEvaluation(code(), RuleOutcome.PASSED, RuleSeverity.INFO,
                        "no previous bankruptcy", null, false))
                .orElseGet(() -> new RuleEvaluation(code(), RuleOutcome.NEEDS_INFORMATION,
                        RuleSeverity.WARNING, "previous-bankruptcy question unanswered",
                        "Укажите, признавались ли вы банкротом ранее.", true));
    }

    private RuleEvaluation evaluateEndedDate(RuleContext context) {
        LocalDate endedOn = context.date(RuleInputs.PREVIOUS_BANKRUPTCY_ENDED_ON).orElse(null);
        if (endedOn == null) {
            return new RuleEvaluation(code(), RuleOutcome.NEEDS_INFORMATION, RuleSeverity.WARNING,
                    "previous-bankruptcy end date unanswered",
                    "Укажите, когда завершилась или была прекращена предыдущая процедура банкротства.",
                    true);
        }
        LocalDate today = LocalDate.now(clock);
        if (endedOn.plus(RuleInputs.REPEAT_FILING_BAR).isAfter(today)) {
            return new RuleEvaluation(code(), RuleOutcome.FAILED, RuleSeverity.INFO,
                    "previous bankruptcy ended " + endedOn + " — inside the 5-year bar",
                    "С завершения предыдущей процедуры банкротства прошло менее 5 лет — повторное "
                            + "внесудебное банкротство пока недоступно; рассмотрите судебную процедуру.",
                    false, RuleInputs.BASIS_REPEAT_FILING);
        }
        return new RuleEvaluation(code(), RuleOutcome.PASSED, RuleSeverity.INFO,
                "previous bankruptcy ended " + endedOn + " — outside the 5-year bar", null, false);
    }
}
