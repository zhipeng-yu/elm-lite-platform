-- elm-lite-platform V1 初始化演示数据
-- 所有账号和联系方式均为虚构数据
-- password_hash 为合成 BCrypt 哈希，不包含真实密码

SET NAMES utf8mb4;

START TRANSACTION;

INSERT IGNORE INTO users (
    id, phone, password_hash, nickname, gender, status
) VALUES (
    1,
    '19900000001',
    '$2b$12$TBLttgwuUA/9F6Buygfk/.J1c//g6iAkkgC08U4NAlebwvT3J2tYO',
    '演示用户',
    0,
    1
);

INSERT IGNORE INTO merchant (
    id, account, password_hash, merchant_name,
    contact_name, contact_phone, status
) VALUES (
    1,
    'demo_merchant',
    '$2b$12$TBLttgwuUA/9F6Buygfk/.J1c//g6iAkkgC08U4NAlebwvT3J2tYO',
    '演示餐饮商家',
    '测试联系人',
    '19900000002',
    1
);

INSERT IGNORE INTO shop (
    id, merchant_id, shop_name, description, address,
    start_price, delivery_price, business_status
) VALUES (
    1,
    1,
    '校园美食店',
    '用于本地开发和测试的演示店铺',
    '测试地址1号',
    15.00,
    3.00,
    1
);

INSERT IGNORE INTO product_category (
    id, shop_id, category_name, sort_order, status
) VALUES
    (1, 1, '主食', 1, 1),
    (2, 1, '饮品', 2, 1);

INSERT IGNORE INTO product (
    id, shop_id, category_id, product_name,
    description, price, stock, status
) VALUES
    (1, 1, 1, '牛肉盖饭', '演示主食商品', 18.00, 100, 1),
    (2, 1, 2, '柠檬水', '演示饮品商品', 6.50, 100, 1);

INSERT IGNORE INTO cart_item (
    id, user_id, product_id, quantity
) VALUES (
    1, 1, 2, 1
);

INSERT IGNORE INTO delivery_address (
    id, user_id, receiver_name, receiver_phone,
    address_detail, address_label, is_default
) VALUES (
    1,
    1,
    '测试用户',
    '19900000001',
    '测试校区1号宿舍楼',
    '学校',
    1
);

INSERT IGNORE INTO orders (
    id, order_no, user_id, shop_id, address_id,
    receiver_name, receiver_phone, delivery_address,
    product_amount, delivery_fee, total_amount, order_status, remark
) VALUES (
    1,
    'TEST202609020001',
    1,
    1,
    1,
    '测试用户',
    '19900000001',
    '测试校区1号宿舍楼',
    36.00,
    3.00,
    39.00,
    0,
    '初始化演示订单'
);

INSERT IGNORE INTO order_item (
    id, order_id, product_id, product_name,
    unit_price, quantity, subtotal
) VALUES (
    1,
    1,
    1,
    '牛肉盖饭',
    18.00,
    2,
    36.00
);

COMMIT;