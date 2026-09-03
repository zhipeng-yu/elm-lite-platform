-- elm-lite-platform V1 数据库结构
-- 适用于 MySQL 5.5
-- 数据库需要提前创建，本迁移文件不负责创建或删除数据库

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 用户表
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户编号',
    phone VARCHAR(20) NOT NULL COMMENT '登录手机号',
    password_hash VARCHAR(100) NOT NULL COMMENT '加密后的密码',
    nickname VARCHAR(50) NOT NULL COMMENT '用户昵称',
    gender TINYINT NOT NULL DEFAULT 0 COMMENT '性别：0未知，1男，2女',
    avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1正常',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '最后修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 商家表
CREATE TABLE merchant (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商家编号',
    account VARCHAR(50) NOT NULL COMMENT '商家登录账号',
    password_hash VARCHAR(100) NOT NULL COMMENT '加密后的密码',
    merchant_name VARCHAR(100) NOT NULL COMMENT '商家名称',
    contact_name VARCHAR(50) NOT NULL COMMENT '联系人姓名',
    contact_phone VARCHAR(20) NOT NULL COMMENT '联系人电话',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1正常',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '最后修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_merchant_account (account)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家表';

-- 3. 店铺表
CREATE TABLE shop (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '店铺编号',
    merchant_id BIGINT NOT NULL COMMENT '所属商家编号',
    shop_name VARCHAR(100) NOT NULL COMMENT '店铺名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '店铺简介',
    address VARCHAR(255) NOT NULL COMMENT '店铺地址',
    image_url VARCHAR(255) DEFAULT NULL COMMENT '店铺图片地址',
    start_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '起送价格',
    delivery_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '配送价格',
    business_status TINYINT NOT NULL DEFAULT 0
        COMMENT '营业状态：0休息，1营业，2临时闭店',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '最后修改时间',
    PRIMARY KEY (id),
    KEY idx_shop_merchant_id (merchant_id),
    CONSTRAINT fk_shop_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺表';


-- 4. 商品分类表
CREATE TABLE product_category (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类编号',
    shop_id BIGINT NOT NULL COMMENT '所属店铺编号',
    category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    sort_order INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '显示顺序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '最后修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_shop_name (shop_id, category_name),
    KEY idx_category_shop_id (shop_id),
    CONSTRAINT fk_category_shop
        FOREIGN KEY (shop_id) REFERENCES shop (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 5. 商品表
CREATE TABLE product (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品编号',
    shop_id BIGINT NOT NULL COMMENT '所属店铺编号',
    category_id BIGINT NOT NULL COMMENT '所属分类编号',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '商品介绍',
    image_url VARCHAR(255) DEFAULT NULL COMMENT '商品图片地址',
    price DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '商品价格',
    stock INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '库存数量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0下架，1上架',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '最后修改时间',
    PRIMARY KEY (id),
    KEY idx_product_shop_id (shop_id),
    KEY idx_product_category_id (category_id),
    KEY idx_product_shop_status (shop_id, status),
    CONSTRAINT fk_product_shop
        FOREIGN KEY (shop_id) REFERENCES shop (id),
    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id) REFERENCES product_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';


-- 6. 购物车明细表
CREATE TABLE cart_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车明细编号',
    user_id BIGINT NOT NULL COMMENT '所属用户编号',
    product_id BIGINT NOT NULL COMMENT '商品编号',
    quantity INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '商品数量',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '最后修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_user_product (user_id, product_id),
    KEY idx_cart_user_id (user_id),
    KEY idx_cart_product_id (product_id),
    CONSTRAINT fk_cart_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_cart_product
        FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车明细表';

-- 7. 收货地址表
CREATE TABLE delivery_address (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '收货地址编号',
    user_id BIGINT NOT NULL COMMENT '所属用户编号',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收货人联系电话',
    address_detail VARCHAR(255) NOT NULL COMMENT '详细收货地址',
    address_label VARCHAR(20) DEFAULT NULL COMMENT '地址标签，如家、学校、公司',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址：0否，1是',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '最后修改时间',
    PRIMARY KEY (id),
    KEY idx_address_user_id (user_id),
    KEY idx_address_user_default (user_id, is_default),
    CONSTRAINT fk_address_user
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';


-- 8. 订单表
CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单编号',
    order_no VARCHAR(32) NOT NULL COMMENT '对外展示的订单号',
    user_id BIGINT NOT NULL COMMENT '下单用户编号',
    shop_id BIGINT NOT NULL COMMENT '所属店铺编号',
    address_id BIGINT DEFAULT NULL COMMENT '下单时选择的收货地址编号',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收货人姓名快照',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收货人电话快照',
    delivery_address VARCHAR(255) NOT NULL COMMENT '收货地址快照',
    product_amount DECIMAL(10,2) UNSIGNED NOT NULL DEFAULT 0.00
        COMMENT '商品总金额',
    delivery_fee DECIMAL(10,2) UNSIGNED NOT NULL DEFAULT 0.00
        COMMENT '配送费',
    total_amount DECIMAL(10,2) UNSIGNED NOT NULL DEFAULT 0.00
        COMMENT '订单总金额',
    order_status TINYINT NOT NULL DEFAULT 0
        COMMENT '订单状态：0待处理，1已确认，2制作中，3配送中，4已完成，5已取消',
    remark VARCHAR(255) DEFAULT NULL COMMENT '订单备注',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '最后修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_order_no (order_no),
    KEY idx_orders_user_status (user_id, order_status),
    KEY idx_orders_shop_status (shop_id, order_status),
    KEY idx_orders_address_id (address_id),
    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_orders_shop
        FOREIGN KEY (shop_id) REFERENCES shop (id),
    CONSTRAINT fk_orders_address
        FOREIGN KEY (address_id) REFERENCES delivery_address (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 9. 订单明细表
CREATE TABLE order_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单明细编号',
    order_id BIGINT NOT NULL COMMENT '所属订单编号',
    product_id BIGINT NOT NULL COMMENT '商品编号',
    product_name VARCHAR(100) NOT NULL COMMENT '下单时的商品名称快照',
    unit_price DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '下单时的商品单价',
    quantity INT UNSIGNED NOT NULL COMMENT '购买数量',
    subtotal DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '商品小计',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_item_product (order_id, product_id),
    KEY idx_order_item_order_id (order_id),
    KEY idx_order_item_product_id (product_id),
    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_item_product
        FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

SET FOREIGN_KEY_CHECKS = 1;