package ru.adiaphora.platform.application.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.application.api.BankruptcyApplicationStatus;
import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.application.domain.BankruptcyApplication;
import ru.adiaphora.platform.common.persistence.BaseEntity;

import java.time.Instant;
import java.util.UUID;

/** JPA persistence view of the {@link BankruptcyApplication} aggregate. */
@Entity
@Table(name = "bankruptcy_applications")
class BankruptcyApplicationEntity extends BaseEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 48)
    private BankruptcyApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "route", nullable = false, length = 48)
    private BankruptcyRoute route;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    protected BankruptcyApplicationEntity() {
        // for JPA
    }

    static BankruptcyApplicationEntity fromDomain(BankruptcyApplication application) {
        BankruptcyApplicationEntity entity = new BankruptcyApplicationEntity();
        entity.applyFrom(application);
        return entity;
    }

    void applyFrom(BankruptcyApplication application) {
        this.id = application.id();
        this.ownerId = application.ownerId();
        this.status = application.status();
        this.route = application.route();
        this.submittedAt = application.submittedAt();
    }

    BankruptcyApplication toDomain() {
        return BankruptcyApplication.rehydrate(id, ownerId, status, route, submittedAt);
    }
}
