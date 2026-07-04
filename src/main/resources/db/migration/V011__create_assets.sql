-- Property in the debtor's estate (real estate, vehicles, bank accounts, securities, cash, ...).
-- Pledged assets may point at the secured creditor holding the collateral; that FK is nullable and
-- ON DELETE SET NULL so removing a creditor row never orphans an asset.
CREATE TABLE assets (
    id                   BINARY(16)    NOT NULL,
    application_id       BINARY(16)    NOT NULL,
    asset_type           VARCHAR(48)   NOT NULL,
    description          VARCHAR(500)  NOT NULL,
    currency             CHAR(3)       NOT NULL DEFAULT 'RUB',
    estimated_value      DECIMAL(19,2) NOT NULL,
    ownership_share      VARCHAR(16)   NULL,
    registration_number  VARCHAR(100)  NULL,
    acquired_on          DATE          NULL,
    pledged              BIT           NOT NULL DEFAULT 0,
    pledge_creditor_id   BINARY(16)    NULL,
    created_at           DATETIME(6)   NOT NULL,
    updated_at           DATETIME(6)   NOT NULL,
    CONSTRAINT pk_assets PRIMARY KEY (id),
    CONSTRAINT fk_assets_application
        FOREIGN KEY (application_id) REFERENCES bankruptcy_applications (id),
    CONSTRAINT fk_assets_pledge_creditor
        FOREIGN KEY (pledge_creditor_id) REFERENCES creditors (id) ON DELETE SET NULL,
    INDEX idx_assets_application (application_id),
    INDEX idx_assets_type (asset_type),
    INDEX idx_assets_pledge_creditor (pledge_creditor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
