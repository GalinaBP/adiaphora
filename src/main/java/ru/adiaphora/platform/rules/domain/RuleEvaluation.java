package ru.adiaphora.platform.rules.domain;

import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;

/**
 * The internal result of evaluating one rule. Unlike the public {@code RuleFinding}, it also carries
 * {@code internalReason}, which is for operators/logs only and is never exposed to end users.
 * {@code legalBasis} names the article/subparagraph of 127-ФЗ the rule applies (nullable; shown to
 * users on result screens and persisted for audit).
 */
public record RuleEvaluation(
        String ruleCode,
        RuleOutcome outcome,
        RuleSeverity severity,
        String internalReason,
        String userMessage,
        boolean blocksAutomaticDecision,
        String legalBasis
) {

    /** Convenience constructor for evaluations without a legal-basis citation. */
    public RuleEvaluation(String ruleCode, RuleOutcome outcome, RuleSeverity severity,
                          String internalReason, String userMessage, boolean blocksAutomaticDecision) {
        this(ruleCode, outcome, severity, internalReason, userMessage, blocksAutomaticDecision, null);
    }

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
