CREATE TABLE questionnaire_versions (
    id          BINARY(16)   NOT NULL,
    code        VARCHAR(64)  NOT NULL,
    label       VARCHAR(200) NOT NULL,
    status      VARCHAR(16)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    CONSTRAINT pk_questionnaire_versions PRIMARY KEY (id),
    CONSTRAINT uq_questionnaire_versions_code UNIQUE (code),
    INDEX idx_questionnaire_versions_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE question_sections (
    id             BINARY(16)   NOT NULL,
    version_id     BINARY(16)   NOT NULL,
    code           VARCHAR(64)  NOT NULL,
    title          VARCHAR(200) NOT NULL,
    display_order  INT          NOT NULL,
    CONSTRAINT pk_question_sections PRIMARY KEY (id),
    CONSTRAINT fk_sections_version FOREIGN KEY (version_id) REFERENCES questionnaire_versions (id),
    CONSTRAINT uq_sections_version_code UNIQUE (version_id, code),
    INDEX idx_sections_version (version_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE question_definitions (
    id                        BINARY(16)    NOT NULL,
    version_id                BINARY(16)    NOT NULL,
    section_code              VARCHAR(64)   NOT NULL,
    code                      VARCHAR(64)   NOT NULL,
    type                      VARCHAR(32)   NOT NULL,
    label                     VARCHAR(500)  NOT NULL,
    help_text                 VARCHAR(1000) NULL,
    required                  BIT           NOT NULL,
    display_order             INT           NOT NULL,
    validation_configuration  VARCHAR(2000) NULL,
    CONSTRAINT pk_question_definitions PRIMARY KEY (id),
    CONSTRAINT fk_questions_version FOREIGN KEY (version_id) REFERENCES questionnaire_versions (id),
    CONSTRAINT uq_questions_version_code UNIQUE (version_id, code),
    INDEX idx_questions_version (version_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE question_options (
    id                      BINARY(16)   NOT NULL,
    question_definition_id  BINARY(16)   NOT NULL,
    value                   VARCHAR(128) NOT NULL,
    label                   VARCHAR(300) NOT NULL,
    display_order           INT          NOT NULL,
    CONSTRAINT pk_question_options PRIMARY KEY (id),
    CONSTRAINT fk_options_question FOREIGN KEY (question_definition_id) REFERENCES question_definitions (id),
    INDEX idx_options_question (question_definition_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
