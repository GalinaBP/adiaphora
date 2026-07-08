package ru.adiaphora.platform.rules.domain;

/**
 * SPI for a single deterministic rule. Implementations must be pure functions of the {@link RuleContext}
 * (no I/O, no clock, no randomness) so evaluations are reproducible and auditable.
 *
 * <p><strong>Placeholder:</strong> the concrete rules ship with illustrative thresholds and messages
 * that MUST be reviewed and approved by a lawyer before any real use.
 */
public interface BankruptcyRule {

    String code();

    /**
     * Position in the evaluation sequence. The {@link RuleEngine} sorts by this (ties broken by
     * {@link #code()}), so evaluation and finding order never depend on bean registration order.
     */
    int order();

    RuleEvaluation evaluate(RuleContext context);
}
