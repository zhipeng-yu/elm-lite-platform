CREATE TABLE wallet_withdrawal_rule (
    id BIGINT NOT NULL,
    fee_basis_points INT NOT NULL COMMENT '提现手续费率，单位为万分之一',
    updated_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_withdrawal_rate CHECK (
        fee_basis_points >= 0 AND fee_basis_points < 10000
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包提现手续费规则';

INSERT INTO wallet_withdrawal_rule (id, fee_basis_points)
VALUES (1, 1000);
