package ru.adiaphora.platform.application.api;

import ru.adiaphora.platform.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Published on every validated status transition of a case. Consumed by {@code audit}. */
public record ApplicationStatusChangedEvent(
        UUID applicationId,
        BankruptcyApplicationStatus fromStatus,
        BankruptcyApplicationStatus toStatus,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {
}
