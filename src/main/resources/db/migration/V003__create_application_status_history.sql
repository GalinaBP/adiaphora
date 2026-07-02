CREATE TABLE application_status_history (
    id              BINARY(16)   NOT NULL,
    application_id  BINARY(16)   NOT NULL,
    from_status     VARCHAR(48)  NULL,
    to_status       VARCHAR(48)  NOT NULL,
    reason          VARCHAR(500) NULL,
    actor_id        BINARY(16)   NULL,
    changed_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_application_status_history PRIMARY KEY (id),
    CONSTRAINT fk_status_history_application
        FOREIGN KEY (application_id) REFERENCES bankruptcy_applications (id),
    INDEX idx_status_history_application (application_id),
    INDEX idx_status_history_changed_at (application_id, changed_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
