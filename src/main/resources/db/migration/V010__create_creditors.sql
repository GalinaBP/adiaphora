-- Creditors the debtor owes money to (banks, microfinance orgs, the tax authority, individuals, ...).
-- One row per claim against a bankruptcy application. Amounts split so rule logic can reason about
-- principal vs. penalties; monetary columns are DECIMAL(19,2) per the money convention.
CREATE TABLE creditors (
    id                BINARY(16)    NOT NULL,
    application_id    BINARY(16)    NOT NULL,
    name              VARCHAR(300)  NOT NULL,
    creditor_type     VARCHAR(48)   NOT NULL,
    inn               VARCHAR(12)   NULL,
    claim_basis       VARCHAR(500)  NULL,
    currency          CHAR(3)       NOT NULL DEFAULT 'RUB',
    total_amount      DECIMAL(19,2) NOT NULL,
    principal_amount  DECIMAL(19,2) NULL,
    interest_amount   DECIMAL(19,2) NULL,
    penalty_amount    DECIMAL(19,2) NULL,
    overdue           BIT           NOT NULL DEFAULT 0,
    secured           BIT           NOT NULL DEFAULT 0,
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    CONSTRAINT pk_creditors PRIMARY KEY (id),
    CONSTRAINT fk_creditors_application
        FOREIGN KEY (application_id) REFERENCES bankruptcy_applications (id),
    INDEX idx_creditors_application (application_id),
    INDEX idx_creditors_type (creditor_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
