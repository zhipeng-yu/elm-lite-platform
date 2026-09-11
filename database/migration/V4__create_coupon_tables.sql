CREATE TABLE coupon (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '优惠券编号',
    shop_id BIGINT NOT NULL COMMENT '所属店铺编号',
    name VARCHAR(255) NOT NULL COMMENT '优惠券名称',
    threshold_amount DECIMAL(10,2) UNSIGNED NOT NULL
        COMMENT '使用门槛金额',
    discount_amount DECIMAL(10,2) UNSIGNED NOT NULL
        COMMENT '优惠金额',
    starts_at DATETIME NOT NULL COMMENT '生效时间',
    expires_at DATETIME NOT NULL COMMENT '到期时间',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '最后修改时间',
    PRIMARY KEY (id),
    KEY idx_coupon_shop_id (shop_id),
    KEY idx_coupon_shop_enabled_time (
        shop_id,
        enabled,
        starts_at,
        expires_at
    ),
    CONSTRAINT fk_coupon_shop
        FOREIGN KEY (shop_id) REFERENCES shop (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

CREATE TABLE user_coupon (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户优惠券编号',
    user_id BIGINT NOT NULL COMMENT '用户编号',
    coupon_id BIGINT NOT NULL COMMENT '优惠券编号',
    status TINYINT NOT NULL DEFAULT 0
        COMMENT '状态：0未使用，1已使用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    updated_at DATETIME DEFAULT NULL COMMENT '最后修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_coupon_user_coupon (user_id, coupon_id),
    KEY idx_user_coupon_user_status (user_id, status),
    KEY idx_user_coupon_coupon_id (coupon_id),
    CONSTRAINT fk_user_coupon_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_coupon_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupon (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';