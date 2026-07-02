package ru.adiaphora.platform.application.api;

import ru.adiaphora.platform.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Published when a case is submitted for evaluation. Consumed by {@code audit}. */
public record ApplicationSubmittedEvent(UUID applicationId, UUID ownerId, Instant occurredAt)
        implements DomainEvent {
}
