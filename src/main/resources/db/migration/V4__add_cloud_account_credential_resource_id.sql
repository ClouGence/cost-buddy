ALTER TABLE cloud_account
    ADD COLUMN credential_resource_id VARCHAR(128) NULL AFTER motherboard_user_id;

UPDATE cloud_account
SET credential_resource_id = CONCAT('aliyun-credential:legacy:', id)
WHERE credential_resource_id IS NULL;

ALTER TABLE cloud_account
    MODIFY COLUMN credential_resource_id VARCHAR(128) NOT NULL,
    ADD UNIQUE KEY uk_cloud_account_credential_resource (credential_resource_id);
