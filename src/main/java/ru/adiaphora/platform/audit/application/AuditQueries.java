package ru.adiaphora.platform.audit.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.audit.domain.AuditEvent;
import ru.adiaphora.platform.audit.domain.AuditEventRepository;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;
import ru.adiaphora.platform.common.web.PageResponse;

import java.util.UUID;

/** Read access to the audit log, for ADMIN/AUDITOR roles (URL-secured). */
@Service
public class AuditQueries {

    private final AuditEventRepository repository;

    public AuditQueries(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEvent> list(UUID applicationId, Pageable pageable) {
        Page<AuditEvent> page = applicationId != null
                ? repository.findByApplicationId(applicationId, pageable)
                : repository.findAll(pageable);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public AuditEvent getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("AuditEvent", id));
    }
}
