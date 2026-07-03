package ru.adiaphora.platform.review.api;

import ru.adiaphora.platform.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Published when a review is assigned to an operator/lawyer. Consumed by {@code audit}. */
public record ReviewAssignedEvent(UUID reviewId, UUID applicationId, UUID assigneeId, Instant occurredAt)
        implements DomainEvent {
}
