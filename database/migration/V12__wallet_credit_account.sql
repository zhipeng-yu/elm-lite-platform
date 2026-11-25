CREATE TABLE wallet_credit_account (
    wallet_id BIGINT NOT NULL,
    credit_limit_cent BIGINT NOT NULL COMMENT '透支额度，单位分',
    repayment_days INT NOT NULL COMMENT '还款期限，单位天',
    daily_rate_basis_points INT NOT NULL COMMENT '逾期日利率，单位万分之一',
    principal_cent BIGINT NOT NULL DEFAULT 0 COMMENT '未偿还本金，单位分',
    accrued_interest_cent BIGINT NOT NULL DEFAULT 0 COMMENT '已结算未偿还利息，单位分',
    due_date DATE DEFAULT NULL,
    interest_calculated_through DATE DEFAULT NULL,
    PRIMARY KEY (wallet_id),
    CONSTRAINT fk_credit_wallet FOREIGN KEY (wallet_id) REFERENCES virtual_wallet(id),
    CONSTRAINT chk_credit_limit CHECK (credit_limit_cent >= 0),
    CONSTRAINT chk_credit_days CHECK (repayment_days BETWEEN 1 AND 3650),
    CONSTRAINT chk_credit_rate CHECK (daily_rate_basis_points BETWEEN 0 AND 10000),
    CONSTRAINT chk_credit_principal CHECK (principal_cent >= 0),
    CONSTRAINT chk_credit_interest CHECK (accrued_interest_cent >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包透支账户';
