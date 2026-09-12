CREATE TABLE rider (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '骑手编号',
    username VARCHAR(50) NOT NULL COMMENT '骑手登录账号',
    password_hash VARCHAR(100) NOT NULL COMMENT '加密后的密码',
    display_name VARCHAR(50) NOT NULL COMMENT '骑手姓名',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1正常',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '最后修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rider_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='骑手表';

ALTER TABLE orders
    ADD COLUMN rider_id BIGINT DEFAULT NULL COMMENT '领取订单的骑手编号' AFTER shop_id,
    ADD KEY idx_orders_rider_status (rider_id, order_status),
    ADD CONSTRAINT fk_orders_rider FOREIGN KEY (rider_id) REFERENCES rider (id);
