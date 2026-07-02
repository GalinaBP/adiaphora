package ru.adiaphora.platform.rules.domain;

import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;

/**
 * The internal result of evaluating one rule. Unlike the public {@code RuleFinding}, it also carries
 * {@code internalReason}, which is for operators/logs only and is never exposed to end users.
 */
public record RuleEvaluation(
        String ruleCode,
        RuleOutcome outcome,
        RuleSeverity severity,
        String internalReason,
        String userMessage,
        boolean blocksAutomaticDecision
) {

    /** True when the rule "fired" — i.e. produced anything other than a clean pass / not-applicable. */
    public boolean isTriggered() {
        return outcome != RuleOutcome.PASSED && outcome != RuleOutcome.NOT_APPLICABLE;
    }

    public boolean requiresManualReview() {
        return isTriggered()
                && (severity == RuleSeverity.MANUAL_REVIEW || severity == RuleSeverity.BLOCKING);
    }

    public boolean needsInformation() {
        return outcome == RuleOutcome.NEEDS_INFORMATION;
    }
}
