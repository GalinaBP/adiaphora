CREATE TABLE bankruptcy_applications (
    id            BINARY(16)  NOT NULL,
    owner_id      BINARY(16)  NOT NULL,
    status        VARCHAR(48) NOT NULL,
    route         VARCHAR(48) NOT NULL,
    submitted_at  DATETIME(6) NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    CONSTRAINT pk_bankruptcy_applications PRIMARY KEY (id),
    CONSTRAINT fk_applications_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    INDEX idx_applications_owner (owner_id),
    INDEX idx_applications_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
