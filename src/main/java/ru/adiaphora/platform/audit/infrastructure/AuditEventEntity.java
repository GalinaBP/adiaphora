package ru.adiaphora.platform.audit.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.audit.domain.AuditAction;
import ru.adiaphora.platform.audit.domain.AuditEvent;
import ru.adiaphora.platform.audit.domain.AuditResult;

import java.time.Instant;
import java.util.UUID;

/** JPA view of an {@link AuditEvent}. Written once and never updated (append-only table). */
@Entity
@Table(name = "audit_events")
class AuditEventEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_id", columnDefinition = "BINARY(16)", updatable = false)
    private UUID actorId;

    @Column(name = "actor_role", length = 32, updatable = false)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 48, updatable = false)
    private AuditAction action;

    @Column(name = "object_type", length = 64, updatable = false)
    private String objectType;

    @Column(name = "object_id", columnDefinition = "BINARY(16)", updatable = false)
    private UUID objectId;

    @Column(name = "application_id", columnDefinition = "BINARY(16)", updatable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 16, updatable = false)
    private AuditResult result;

    @Column(name = "correlation_id", length = 64, updatable = false)
    private String correlationId;

    @Column(name = "metadata", length = 1000, updatable = false)
    private String metadata;

    protected AuditEventEntity() {
        // for JPA
    }

    static AuditEventEntity fromDomain(AuditEvent event) {
        AuditEventEntity entity = new AuditEventEntity();
        entity.id = event.id();
        entity.occurredAt = event.occurredAt();
        entity.actorId = event.actorId();
        entity.actorRole = event.actorRole();
        entity.action = event.action();
        entity.objectType = event.objectType();
        entity.objectId = event.objectId();
        entity.applicationId = event.applicationId();
        entity.result = event.result();
        entity.correlationId = event.correlationId();
        entity.metadata = event.metadata();
        return entity;
    }

    AuditEvent toDomain() {
        return new AuditEvent(id, occurredAt, actorId, actorRole, action, objectType, objectId,
                applicationId, result, correlationId, metadata);
    }
}
