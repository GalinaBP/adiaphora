package ru.adiaphora.platform.application.api;

import ru.adiaphora.platform.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Published when a bankruptcy case is created. Consumed by {@code audit}. */
public record ApplicationCreatedEvent(UUID applicationId, UUID ownerId, Instant occurredAt)
        implements DomainEvent {
}
