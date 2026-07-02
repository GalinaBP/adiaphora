package ru.adiaphora.platform.audit.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain port for the audit log. Intentionally append-and-read only: there is no update or delete, so
 * the trail is immutable through the application.
 */
public interface AuditEventRepository {

    AuditEvent append(AuditEvent event);

    Page<AuditEvent> findAll(Pageable pageable);

    Page<AuditEvent> findByApplicationId(UUID applicationId, Pageable pageable);

    Optional<AuditEvent> findById(UUID id);
}
