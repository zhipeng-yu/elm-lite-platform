CREATE TABLE product_detail_image (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品详情图编号',
    product_id BIGINT NOT NULL COMMENT '所属商品编号',
    image_url VARCHAR(255) NOT NULL COMMENT '详情图片地址',
    sort_order INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '显示顺序',
    PRIMARY KEY (id),
    KEY idx_product_detail_image_product_id (product_id),
    CONSTRAINT fk_product_detail_image_product
        FOREIGN KEY (product_id) REFERENCES product (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品详情图片表';