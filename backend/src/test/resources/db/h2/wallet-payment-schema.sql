CREATE TABLE wallet_payment (
    transaction_id BIGINT PRIMARY KEY,
    settlement_status VARCHAR(16) NOT NULL,
    confirmed_at TIMESTAMP NULL,
    CONSTRAINT fk_test_payment_transaction FOREIGN KEY (transaction_id) REFERENCES wallet_transaction(id),
    CONSTRAINT chk_test_settlement CHECK (settlement_status IN ('FROZEN', 'CONFIRMED'))
);
UPDATE virtual_wallet SET balance_cent=5000, frozen_balance_cent=2000 WHERE id=1;
UPDATE virtual_wallet SET balance_cent=500, frozen_balance_cent=100 WHERE id=3;
