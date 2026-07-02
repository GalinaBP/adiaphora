package ru.adiaphora.platform.audit.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.audit.domain.AuditEvent;
import ru.adiaphora.platform.audit.domain.AuditEventRepository;
import ru.adiaphora.platform.common.web.CorrelationId;

import java.time.Instant;
import java.util.UUID;

/**
 * Persists audit entries in their own transaction ({@code REQUIRES_NEW}) so the trail is captured
 * independently of the business operation's outcome — including failed logins, whose originating
 * transaction rolls back. Supplies the record's identity, occurrence time, and correlation id.
 */
@Service
public class AuditRecorder {

    private final AuditEventRepository repository;

    public AuditRecorder(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEvent.Builder builder, Instant occurredAt) {
        AuditEvent event = builder.build(UUID.randomUUID(), occurredAt, CorrelationId.current());
        repository.append(event);
    }
}
