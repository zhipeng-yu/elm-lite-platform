DROP TABLE IF EXISTS wallet_withdrawal_rule;

CREATE TABLE wallet_withdrawal_rule (
    id BIGINT PRIMARY KEY,
    fee_basis_points INT NOT NULL,
    updated_at TIMESTAMP DEFAULT NULL,
    CONSTRAINT chk_test_withdrawal_rate CHECK (
        fee_basis_points >= 0 AND fee_basis_points < 10000
    )
);

INSERT INTO wallet_withdrawal_rule (id, fee_basis_points)
VALUES (1, 1000);
