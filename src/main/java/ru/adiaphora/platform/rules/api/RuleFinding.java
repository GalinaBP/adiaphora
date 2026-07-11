package ru.adiaphora.platform.rules.api;

/**
 * A public, client-safe finding for a triggered rule. Deliberately excludes the rule's internal
 * reasoning; only the user-facing message and the legal-basis citation (nullable) are exposed.
 */
public record RuleFinding(
        String ruleCode,
        RuleOutcome outcome,
        RuleSeverity severity,
        String userMessage,
        boolean blocksAutomaticDecision,
        String legalBasis
) {
}
