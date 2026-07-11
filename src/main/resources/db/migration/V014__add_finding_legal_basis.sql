-- Legal-basis citation (article/subparagraph of 127-ФЗ) behind a rule finding. Shown on result
-- screens and kept for audit; nullable because technical rules (missing answers) cite nothing.
ALTER TABLE rule_evaluation_findings
    ADD COLUMN legal_basis VARCHAR(150) NULL AFTER blocks_automatic_decision;
