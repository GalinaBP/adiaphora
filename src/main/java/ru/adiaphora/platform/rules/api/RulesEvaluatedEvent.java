package ru.adiaphora.platform.rules.api;

import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Published when an application is evaluated by the rule engine. Consumed by {@code audit}. */
public record RulesEvaluatedEvent(
        UUID applicationId,
        String rulesetVersion,
        BankruptcyRoute route,
        boolean manualReviewRequired,
        Instant occurredAt
) implements DomainEvent {
}
