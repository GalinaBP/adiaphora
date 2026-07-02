CREATE TABLE questionnaire_responses (
    id             BINARY(16)  NOT NULL,
    application_id BINARY(16)  NOT NULL,
    version_id     BINARY(16)  NOT NULL,
    version_code   VARCHAR(64) NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT pk_questionnaire_responses PRIMARY KEY (id),
    CONSTRAINT uq_responses_application UNIQUE (application_id),
    CONSTRAINT fk_responses_application FOREIGN KEY (application_id) REFERENCES bankruptcy_applications (id),
    CONSTRAINT fk_responses_version FOREIGN KEY (version_id) REFERENCES questionnaire_versions (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE question_answers (
    id             BINARY(16)    NOT NULL,
    response_id    BINARY(16)    NOT NULL,
    question_code  VARCHAR(64)   NOT NULL,
    value          VARCHAR(4000) NULL,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    CONSTRAINT pk_question_answers PRIMARY KEY (id),
    CONSTRAINT fk_answers_response FOREIGN KEY (response_id) REFERENCES questionnaire_responses (id),
    CONSTRAINT uq_answers_response_question UNIQUE (response_id, question_code),
    INDEX idx_answers_response (response_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
