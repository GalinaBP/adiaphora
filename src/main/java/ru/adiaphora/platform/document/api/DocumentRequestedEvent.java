package ru.adiaphora.platform.document.api;

import ru.adiaphora.platform.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Published when a document generation is requested. Consumed by {@code audit}. */
public record DocumentRequestedEvent(UUID documentId, UUID applicationId, UUID actorId, Instant occurredAt)
        implements DomainEvent {
}
