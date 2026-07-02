CREATE TABLE rule_evaluations (
    id                     BINARY(16)    NOT NULL,
    application_id         BINARY(16)    NOT NULL,
    ruleset_version        VARCHAR(64)   NOT NULL,
    questionnaire_version  VARCHAR(64)   NULL,
    started_at             DATETIME(6)   NOT NULL,
    completed_at           DATETIME(6)   NOT NULL,
    route                  VARCHAR(48)   NOT NULL,
    manual_review_required BIT           NOT NULL,
    missing_information    VARCHAR(1000) NULL,
    CONSTRAINT pk_rule_evaluations PRIMARY KEY (id),
    CONSTRAINT fk_evaluations_application
        FOREIGN KEY (application_id) REFERENCES bankruptcy_applications (id),
    INDEX idx_evaluations_application (application_id, completed_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE rule_evaluation_findings (
    id                         BINARY(16)    NOT NULL,
    evaluation_id              BINARY(16)    NOT NULL,
    rule_code                  VARCHAR(100)  NOT NULL,
    outcome                    VARCHAR(32)   NOT NULL,
    severity                   VARCHAR(32)   NOT NULL,
    internal_reason            VARCHAR(1000) NULL,
    user_message               VARCHAR(1000) NULL,
    blocks_automatic_decision  BIT           NOT NULL,
    CONSTRAINT pk_rule_evaluation_findings PRIMARY KEY (id),
    CONSTRAINT fk_findings_evaluation FOREIGN KEY (evaluation_id) REFERENCES rule_evaluations (id),
    INDEX idx_findings_evaluation (evaluation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
