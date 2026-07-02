package ru.adiaphora.platform.audit.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import ru.adiaphora.platform.audit.domain.AuditEvent;
import ru.adiaphora.platform.audit.domain.AuditEventRepository;

import java.util.Optional;
import java.util.UUID;

/** Adapts Spring Data JPA to the append-and-read {@link AuditEventRepository} port. */
@Component
class AuditEventRepositoryAdapter implements AuditEventRepository {

    private final AuditEventJpaRepository jpa;

    AuditEventRepositoryAdapter(AuditEventJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AuditEvent append(AuditEvent event) {
        return jpa.save(AuditEventEntity.fromDomain(event)).toDomain();
    }

    @Override
    public Page<AuditEvent> findAll(Pageable pageable) {
        return jpa.findAll(pageable).map(AuditEventEntity::toDomain);
    }

    @Override
    public Page<AuditEvent> findByApplicationId(UUID applicationId, Pageable pageable) {
        return jpa.findByApplicationId(applicationId, pageable).map(AuditEventEntity::toDomain);
    }

    @Override
    public Optional<AuditEvent> findById(UUID id) {
        return jpa.findById(id).map(AuditEventEntity::toDomain);
    }
}
