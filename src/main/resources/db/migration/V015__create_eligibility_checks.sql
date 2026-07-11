-- Anonymous eligibility-check sessions from the public home page (per the eligibility-flow ticket:
-- persisted for audit and future repeat-application logic). Append-only; no personal identifiers.
CREATE TABLE eligibility_checks
(
    id                 BINARY(16)    NOT NULL,
    created_at         TIMESTAMP(6)  NOT NULL,
    answers_json       VARCHAR(4000) NOT NULL,
    verdict            VARCHAR(32)   NOT NULL,
    route              VARCHAR(48)   NOT NULL,
    qualifying_grounds VARCHAR(200)  NULL,
    citations          VARCHAR(500)  NULL,
    ruleset_version    VARCHAR(64)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_eligibility_checks_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
