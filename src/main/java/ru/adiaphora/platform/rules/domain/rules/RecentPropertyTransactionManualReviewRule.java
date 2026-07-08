package ru.adiaphora.platform.rules.domain.rules;

import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;
import ru.adiaphora.platform.rules.domain.BankruptcyRule;
import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleInputs;

import java.util.Set;

/** Routes cases with a recent property sale/gift to manual review (possible avoidance). Placeholder. */
public class RecentPropertyTransactionManualReviewRule implements BankruptcyRule {

    private static final Set<String> FLAGGED = Set.of("sold", "gifted");

    @Override
    public String code() {
        return "MANUAL-REVIEW-RECENT-PROPERTY-TRANSACTION";
    }

    @Override
    public int order() {
        return 70;
    }

    @Override
    public RuleEvaluation evaluate(RuleContext context) {
        return context.text(RuleInputs.RECENT_PROPERTY_TRANSACTION)
                .map(value -> FLAGGED.contains(value)
                        ? new RuleEvaluation(code(), RuleOutcome.FAILED, RuleSeverity.MANUAL_REVIEW,
                        "recent property transaction: " + value,
                        "Недавняя сделка с имуществом требует проверки юристом.", true)
                        : new RuleEvaluation(code(), RuleOutcome.PASSED, RuleSeverity.INFO,
                        "no flagged property transaction", null, false))
                .orElseGet(() -> new RuleEvaluation(code(), RuleOutcome.NOT_APPLICABLE, RuleSeverity.INFO,
                        "property-transaction question unanswered", null, false));
    }
}
