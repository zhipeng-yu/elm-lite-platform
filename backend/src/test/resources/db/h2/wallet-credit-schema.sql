CREATE TABLE wallet_credit_account (
    wallet_id BIGINT PRIMARY KEY,
    credit_limit_cent BIGINT NOT NULL,
    repayment_days INT NOT NULL,
    daily_rate_basis_points INT NOT NULL,
    principal_cent BIGINT NOT NULL DEFAULT 0,
    accrued_interest_cent BIGINT NOT NULL DEFAULT 0,
    due_date DATE DEFAULT NULL,
    interest_calculated_through DATE DEFAULT NULL,
    CONSTRAINT fk_test_credit_wallet FOREIGN KEY (wallet_id) REFERENCES virtual_wallet(id),
    CONSTRAINT chk_test_credit_limit CHECK (credit_limit_cent >= 0),
    CONSTRAINT chk_test_credit_days CHECK (repayment_days BETWEEN 1 AND 3650),
    CONSTRAINT chk_test_credit_rate CHECK (daily_rate_basis_points BETWEEN 0 AND 10000),
    CONSTRAINT chk_test_credit_principal CHECK (principal_cent >= 0),
    CONSTRAINT chk_test_credit_interest CHECK (accrued_interest_cent >= 0)
);
