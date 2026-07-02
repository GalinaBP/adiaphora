package ru.adiaphora.platform.application.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.application.api.BankruptcyApplicationStatus;
import ru.adiaphora.platform.application.domain.StatusChange;

import java.time.Instant;
import java.util.UUID;

/** Append-only status-history row. Immutable once written (no updates). */
@Entity
@Table(name = "application_status_history")
class ApplicationStatusHistoryEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "application_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 48)
    private BankruptcyApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 48)
    private BankruptcyApplicationStatus toStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "actor_id", columnDefinition = "BINARY(16)")
    private UUID actorId;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected ApplicationStatusHistoryEntity() {
        // for JPA
    }

    static ApplicationStatusHistoryEntity of(UUID applicationId, StatusChange change) {
        ApplicationStatusHistoryEntity entity = new ApplicationStatusHistoryEntity();
        entity.id = UUID.randomUUID();
        entity.applicationId = applicationId;
        entity.fromStatus = change.from();
        entity.toStatus = change.to();
        entity.reason = change.reason();
        entity.actorId = change.actorId();
        entity.changedAt = change.changedAt();
        return entity;
    }

    StatusChange toDomain() {
        return new StatusChange(fromStatus, toStatus, reason, actorId, changedAt);
    }
}
