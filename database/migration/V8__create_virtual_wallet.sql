CREATE TABLE virtual_wallet (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '钱包编号',
    owner_type VARCHAR(16) NOT NULL COMMENT '账户类型 USER 或 MERCHANT',
    owner_id BIGINT NOT NULL COMMENT '对应账户编号',
    balance_cent BIGINT NOT NULL DEFAULT 0 COMMENT '总余额，单位分',
    frozen_balance_cent BIGINT NOT NULL DEFAULT 0 COMMENT '冻结余额，单位分',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_virtual_wallet_owner (owner_type, owner_id),
    CONSTRAINT chk_wallet_owner_type CHECK (owner_type IN ('USER', 'MERCHANT')),
    CONSTRAINT chk_wallet_owner_id CHECK (owner_id > 0),
    CONSTRAINT chk_wallet_frozen_nonnegative CHECK (frozen_balance_cent >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='虚拟钱包';
