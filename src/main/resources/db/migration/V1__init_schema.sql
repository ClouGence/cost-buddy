CREATE TABLE ai_engine (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    model VARCHAR(128) NOT NULL,
    api_key TEXT NOT NULL,
    api_addr VARCHAR(512) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_engine_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cloud_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    provider VARCHAR(32) NOT NULL DEFAULT 'ALIYUN',
    access_key_id VARCHAR(256) NULL,
    access_key_secret TEXT NULL,
    bill_owner_id BIGINT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_cloud_account_name (name),
    KEY idx_cloud_account_provider (provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE billing_item_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(32) NOT NULL DEFAULT 'ALIYUN',
    match_scope VARCHAR(32) NOT NULL,
    product_code VARCHAR(128) NULL,
    product_name VARCHAR(256) NULL,
    product_detail VARCHAR(512) NULL,
    commodity_code VARCHAR(128) NULL,
    billing_item_code VARCHAR(128) NULL,
    billing_item VARCHAR(256) NULL,
    decision VARCHAR(32) NOT NULL,
    note TEXT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_billing_item_rule_provider (provider),
    KEY idx_billing_item_rule_product_code (product_code),
    KEY idx_billing_item_rule_billing_item_code (billing_item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE billing_audit_run (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cloud_account_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    bill_date DATE NOT NULL,
    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL,
    item_count INT NOT NULL DEFAULT 0,
    unknown_item_count INT NOT NULL DEFAULT 0,
    total_pretax_amount DECIMAL(18, 6) NOT NULL DEFAULT 0,
    unknown_pretax_amount DECIMAL(18, 6) NOT NULL DEFAULT 0,
    message TEXT NULL,
    started_at DATETIME(3) NULL,
    finished_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_billing_audit_run_cloud_account (cloud_account_id),
    KEY idx_billing_audit_run_bill_date (bill_date),
    KEY idx_billing_audit_run_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE billing_audit_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL DEFAULT 'ALIYUN',
    product_code VARCHAR(128) NULL,
    product_name VARCHAR(256) NULL,
    product_detail VARCHAR(512) NULL,
    commodity_code VARCHAR(128) NULL,
    billing_item_code VARCHAR(128) NULL,
    billing_item VARCHAR(256) NULL,
    billing_type VARCHAR(128) NULL,
    subscription_type VARCHAR(64) NULL,
    currency VARCHAR(16) NULL,
    stable_day_pretax_amount DECIMAL(18, 6) NOT NULL DEFAULT 0,
    period_pretax_amount DECIMAL(18, 6) NOT NULL DEFAULT 0,
    instance_count INT NOT NULL DEFAULT 0,
    region_count INT NOT NULL DEFAULT 0,
    sample_instance_id VARCHAR(256) NULL,
    sample_region VARCHAR(128) NULL,
    sample_usage VARCHAR(128) NULL,
    sample_usage_unit VARCHAR(64) NULL,
    decision VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_billing_audit_item_run (run_id),
    KEY idx_billing_audit_item_product_code (product_code),
    KEY idx_billing_audit_item_billing_item_code (billing_item_code),
    KEY idx_billing_audit_item_decision (decision)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE billing_item_explanation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    audit_item_id BIGINT NOT NULL,
    ai_engine_id BIGINT NOT NULL,
    prompt_context MEDIUMTEXT NOT NULL,
    explanation MEDIUMTEXT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_billing_item_explanation_audit_item (audit_item_id),
    KEY idx_billing_item_explanation_ai_engine (ai_engine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
