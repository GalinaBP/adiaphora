CREATE TABLE generated_documents (
    id              BINARY(16)   NOT NULL,
    application_id  BINARY(16)   NOT NULL,
    template_code   VARCHAR(100) NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    filename        VARCHAR(300) NULL,
    content_type    VARCHAR(150) NULL,
    storage_key     VARCHAR(200) NULL,
    size_bytes      BIGINT       NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_generated_documents PRIMARY KEY (id),
    CONSTRAINT fk_documents_application FOREIGN KEY (application_id) REFERENCES bankruptcy_applications (id),
    INDEX idx_documents_application (application_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
