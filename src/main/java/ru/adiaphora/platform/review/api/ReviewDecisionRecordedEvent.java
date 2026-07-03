package ru.adiaphora.platform.review.api;

import ru.adiaphora.platform.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a reviewer records a decision (approve/reject). Consumed by {@code audit}. Carries
 * the decision label only, never sensitive free-text.
 */
public record ReviewDecisionRecordedEvent(
        UUID reviewId,
        UUID applicationId,
        String decision,
        UUID reviewerId,
        Instant occurredAt
) implements DomainEvent {
}
