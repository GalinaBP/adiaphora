-- Notable transactions the debtor made in the look-back period (sales, gifts, pledges, transfers).
-- The financial manager may challenge these, so occurred_on and counterparty_relation are indexed
-- to support "deals in the last N years with interested parties" queries. Named
-- financial_transactions to avoid confusion with the SQL TRANSACTION keyword.
CREATE TABLE financial_transactions (
    id                     BINARY(16)    NOT NULL,
    application_id         BINARY(16)    NOT NULL,
    transaction_type       VARCHAR(48)   NOT NULL,
    counterparty           VARCHAR(300)  NULL,
    counterparty_relation  VARCHAR(48)   NULL,
    subject                VARCHAR(500)  NULL,
    currency               CHAR(3)       NOT NULL DEFAULT 'RUB',
    amount                 DECIMAL(19,2) NOT NULL,
    occurred_on            DATE          NOT NULL,
    registered             BIT           NOT NULL DEFAULT 0,
    created_at             DATETIME(6)   NOT NULL,
    updated_at             DATETIME(6)   NOT NULL,
    CONSTRAINT pk_financial_transactions PRIMARY KEY (id),
    CONSTRAINT fk_transactions_application
        FOREIGN KEY (application_id) REFERENCES bankruptcy_applications (id),
    INDEX idx_transactions_application (application_id),
    INDEX idx_transactions_occurred_on (application_id, occurred_on),
    INDEX idx_transactions_relation (counterparty_relation)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
