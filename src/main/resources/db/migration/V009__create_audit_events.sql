-- Audit log. Intentionally has no foreign keys: audit records are decoupled from and outlive the
-- entities they reference, and the table is append-only (no updates or deletes from the application).
CREATE TABLE audit_events (
    id              BINARY(16)    NOT NULL,
    occurred_at     DATETIME(6)   NOT NULL,
    actor_id        BINARY(16)    NULL,
    actor_role      VARCHAR(32)   NULL,
    action          VARCHAR(48)   NOT NULL,
    object_type     VARCHAR(64)   NULL,
    object_id       BINARY(16)    NULL,
    application_id  BINARY(16)    NULL,
    result          VARCHAR(16)   NOT NULL,
    correlation_id  VARCHAR(64)   NULL,
    metadata        VARCHAR(1000) NULL,
    CONSTRAINT pk_audit_events PRIMARY KEY (id),
    INDEX idx_audit_events_application (application_id),
    INDEX idx_audit_events_occurred_at (occurred_at),
    INDEX idx_audit_events_action (action)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
