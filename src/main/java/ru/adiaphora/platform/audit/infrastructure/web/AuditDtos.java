package ru.adiaphora.platform.audit.infrastructure.web;

import ru.adiaphora.platform.audit.domain.AuditEvent;

import java.time.Instant;
import java.util.UUID;

/** Response payloads for the audit endpoints. */
public final class AuditDtos {

    private AuditDtos() {
    }

    public record AuditEventResponse(
            UUID id,
            Instant occurredAt,
            UUID actorId,
            String actorRole,
            String action,
            String objectType,
            UUID objectId,
            UUID applicationId,
            String result,
            String correlationId,
            String metadata) {

        public static AuditEventResponse from(AuditEvent event) {
            return new AuditEventResponse(
                    event.id(),
                    event.occurredAt(),
                    event.actorId(),
                    event.actorRole(),
                    event.action().name(),
                    event.objectType(),
                    event.objectId(),
                    event.applicationId(),
                    event.result().name(),
                    event.correlationId(),
                    event.metadata());
        }
    }
}
