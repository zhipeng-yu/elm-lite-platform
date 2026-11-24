CREATE TABLE points_order_spend (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    points BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_points_order_spend_order (order_id),
    KEY idx_points_order_spend_user (user_id, id),
    CONSTRAINT fk_points_order_spend_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_points_order_spend_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT chk_points_order_spend_points CHECK (points > 0),
    CONSTRAINT chk_points_order_spend_status CHECK (status IN ('SPENT', 'RETURNED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单积分抵扣记录';

CREATE TABLE points_order_allocation (
    spend_id BIGINT NOT NULL,
    grant_id BIGINT NOT NULL,
    points BIGINT NOT NULL,
    PRIMARY KEY (spend_id, grant_id),
    CONSTRAINT fk_points_order_alloc_spend FOREIGN KEY (spend_id) REFERENCES points_order_spend (id),
    CONSTRAINT fk_points_order_alloc_grant FOREIGN KEY (grant_id) REFERENCES points_grant (id),
    CONSTRAINT chk_points_order_alloc_points CHECK (points > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单消耗的积分批次';
