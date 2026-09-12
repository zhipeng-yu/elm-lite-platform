ALTER TABLE orders
    ADD COLUMN user_coupon_id BIGINT DEFAULT NULL
        COMMENT '本订单使用的用户优惠券编号'
        AFTER address_id,
    ADD COLUMN discount_amount DECIMAL(10,2) UNSIGNED NOT NULL DEFAULT 0.00
        COMMENT '实际优惠金额快照'
        AFTER product_amount,
    ADD KEY idx_orders_user_coupon_id (user_coupon_id),
    ADD CONSTRAINT fk_orders_user_coupon
        FOREIGN KEY (user_coupon_id) REFERENCES user_coupon (id)
        ON DELETE SET NULL;