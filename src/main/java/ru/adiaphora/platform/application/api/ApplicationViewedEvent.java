package ru.adiaphora.platform.application.api;

import ru.adiaphora.platform.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a user or staff member reads a single application's details. Consumed by
 * {@code audit} so access to sensitive case data is traceable.
 */
public record ApplicationViewedEvent(UUID applicationId, UUID viewerId, Instant occurredAt)
        implements DomainEvent {
}
