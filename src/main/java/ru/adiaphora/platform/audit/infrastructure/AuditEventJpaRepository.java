package ru.adiaphora.platform.audit.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AuditEventJpaRepository extends JpaRepository<AuditEventEntity, UUID> {

    Page<AuditEventEntity> findByApplicationId(UUID applicationId, Pageable pageable);
}
