CREATE TABLE wallet_recharge_rule (
    id BIGINT NOT NULL,
    threshold_cent BIGINT NOT NULL COMMENT '达到此金额可获奖励，单位分',
    reward_cent BIGINT NOT NULL COMMENT '每次充值赠送金额，单位分',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用奖励',
    updated_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_recharge_threshold CHECK (threshold_cent > 0),
    CONSTRAINT chk_recharge_reward CHECK (reward_cent >= 0),
    CONSTRAINT chk_recharge_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包充值奖励规则';

INSERT INTO wallet_recharge_rule (id, threshold_cent, reward_cent, enabled)
VALUES (1, 100000, 10000, 1);

CREATE TABLE wallet_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transaction_no VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    transaction_type VARCHAR(16) NOT NULL,
    request_wallet_id BIGINT NOT NULL,
    request_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_wallet_id BIGINT DEFAULT NULL,
    destination_wallet_id BIGINT DEFAULT NULL,
    amount_cent BIGINT NOT NULL,
    reward_cent BIGINT NOT NULL DEFAULT 0,
    fee_cent BIGINT NOT NULL DEFAULT 0,
    source_balance_after_cent BIGINT DEFAULT NULL,
    destination_balance_after_cent BIGINT DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_transaction_no (transaction_no),
    UNIQUE KEY uk_wallet_transaction_request (request_wallet_id, request_key),
    KEY idx_wallet_transaction_source (source_wallet_id, id),
    KEY idx_wallet_transaction_destination (destination_wallet_id, id),
    CONSTRAINT fk_wallet_tx_request FOREIGN KEY (request_wallet_id) REFERENCES virtual_wallet(id),
    CONSTRAINT fk_wallet_tx_source FOREIGN KEY (source_wallet_id) REFERENCES virtual_wallet(id),
    CONSTRAINT fk_wallet_tx_destination FOREIGN KEY (destination_wallet_id) REFERENCES virtual_wallet(id),
    CONSTRAINT chk_wallet_tx_participant CHECK (source_wallet_id IS NOT NULL OR destination_wallet_id IS NOT NULL),
    CONSTRAINT chk_wallet_tx_amount CHECK (amount_cent > 0),
    CONSTRAINT chk_wallet_tx_reward CHECK (reward_cent >= 0),
    CONSTRAINT chk_wallet_tx_fee CHECK (fee_cent >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包交易流水';
