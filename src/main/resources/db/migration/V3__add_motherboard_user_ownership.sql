ALTER TABLE cloud_account
    ADD COLUMN motherboard_user_id BIGINT NOT NULL DEFAULT 0 AFTER id,
    DROP INDEX uk_cloud_account_name,
    ADD UNIQUE KEY uk_cloud_account_user_name (motherboard_user_id, name),
    ADD KEY idx_cloud_account_user (motherboard_user_id, id);

ALTER TABLE billing_item_rule
    ADD COLUMN motherboard_user_id BIGINT NOT NULL DEFAULT 0 AFTER id,
    ADD KEY idx_billing_item_rule_user (motherboard_user_id, id);

ALTER TABLE billing_audit_run
    ADD COLUMN motherboard_user_id BIGINT NOT NULL DEFAULT 0 AFTER id,
    ADD KEY idx_billing_audit_run_user (motherboard_user_id, id);
