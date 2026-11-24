CREATE TABLE points_mall_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    points_cost BIGINT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT chk_points_mall_item_cost CHECK (points_cost > 0),
    CONSTRAINT chk_points_mall_item_stock CHECK (stock >= 0),
    CONSTRAINT chk_points_mall_item_enabled CHECK (enabled IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分商城演示商品';

CREATE TABLE points_mall_redemption (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    points_per_item BIGINT NOT NULL,
    total_points BIGINT NOT NULL,
    request_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_points_mall_request (user_id,request_key),
    KEY idx_points_mall_redemption_user (user_id,id),
    CONSTRAINT fk_points_mall_redemption_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_points_mall_redemption_item FOREIGN KEY (item_id) REFERENCES points_mall_item(id),
    CONSTRAINT chk_points_mall_quantity CHECK (quantity > 0),
    CONSTRAINT chk_points_mall_unit CHECK (points_per_item > 0),
    CONSTRAINT chk_points_mall_total CHECK (total_points > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分商城兑换记录';

CREATE TABLE points_mall_allocation (
    redemption_id BIGINT NOT NULL,
    grant_id BIGINT NOT NULL,
    points BIGINT NOT NULL,
    PRIMARY KEY (redemption_id,grant_id),
    CONSTRAINT fk_points_mall_allocation_redemption FOREIGN KEY (redemption_id) REFERENCES points_mall_redemption(id),
    CONSTRAINT fk_points_mall_allocation_grant FOREIGN KEY (grant_id) REFERENCES points_grant(id),
    CONSTRAINT chk_points_mall_allocation_points CHECK (points > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城兑换的积分批次';

INSERT INTO points_mall_item(name,points_cost,stock,enabled) VALUES
    ('模拟饮品礼券',100,20,1), ('模拟餐食礼券',200,20,1), ('模拟文具礼券',500,20,1);
