package ru.adiaphora.platform.rules.api;

/** How serious a triggered rule is, which drives whether manual review is required. */
public enum RuleSeverity {
    INFO,
    WARNING,
    MANUAL_REVIEW,
    BLOCKING
}
