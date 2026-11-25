CREATE TABLE wallet_payment (
    transaction_id BIGINT NOT NULL,
    settlement_status VARCHAR(16) NOT NULL COMMENT 'FROZEN 或 CONFIRMED',
    confirmed_at DATETIME DEFAULT NULL,
    PRIMARY KEY (transaction_id),
    CONSTRAINT fk_wallet_payment_transaction FOREIGN KEY (transaction_id) REFERENCES wallet_transaction(id),
    CONSTRAINT chk_wallet_payment_status CHECK (settlement_status IN ('FROZEN', 'CONFIRMED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包付款结算状态';
