package ru.adiaphora.platform.questionnaire.api;

import ru.adiaphora.platform.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when an answer is saved. Consumed by {@code audit}. Carries only the question code — never
 * the answer value, which may be sensitive.
 */
public record AnswerUpdatedEvent(UUID applicationId, String questionCode, UUID actorId, Instant occurredAt)
        implements DomainEvent {
}
