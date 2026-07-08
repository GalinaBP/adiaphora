-- AI-013: identify the exact input a historical evaluation was computed from.
-- SHA-256 (lowercase hex) over the questionnaire version and the answers sorted by question code.
-- NULL for evaluations persisted before this column existed.
ALTER TABLE rule_evaluations
    ADD COLUMN input_snapshot_hash VARCHAR(64) NULL AFTER questionnaire_version;
