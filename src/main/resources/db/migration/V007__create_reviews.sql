CREATE TABLE reviews (
    id                    BINARY(16)    NOT NULL,
    application_id        BINARY(16)    NOT NULL,
    status                VARCHAR(32)   NOT NULL,
    assignee_id           BINARY(16)    NULL,
    route                 VARCHAR(48)   NOT NULL,
    ruleset_version       VARCHAR(64)   NOT NULL,
    last_decision_reason  VARCHAR(1000) NULL,
    created_at            DATETIME(6)   NOT NULL,
    updated_at            DATETIME(6)   NOT NULL,
    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT fk_reviews_application FOREIGN KEY (application_id) REFERENCES bankruptcy_applications (id),
    INDEX idx_reviews_application (application_id),
    INDEX idx_reviews_status (status),
    INDEX idx_reviews_assignee (assignee_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE review_route_overrides (
    id               BINARY(16)    NOT NULL,
    review_id        BINARY(16)    NOT NULL,
    previous_route   VARCHAR(48)   NOT NULL,
    new_route        VARCHAR(48)   NOT NULL,
    reason           VARCHAR(1000) NOT NULL,
    reviewer_id      BINARY(16)    NOT NULL,
    reviewed_at      DATETIME(6)   NOT NULL,
    ruleset_version  VARCHAR(64)   NOT NULL,
    CONSTRAINT pk_review_route_overrides PRIMARY KEY (id),
    CONSTRAINT fk_overrides_review FOREIGN KEY (review_id) REFERENCES reviews (id),
    INDEX idx_overrides_review (review_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
