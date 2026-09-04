INSERT INTO users (id) VALUES (1);
INSERT INTO shop (id) VALUES (1);
INSERT INTO delivery_address (id) VALUES (1);

INSERT INTO orders (
    id,
    order_no,
    user_id,
    shop_id,
    address_id,
    receiver_name,
    receiver_phone,
    delivery_address,
    product_amount,
    delivery_fee,
    total_amount,
    order_status,
    remark
) VALUES (
    1,
    'TEST202609020001',
    1,
    1,
    1,
    'Test User',
    '19900000001',
    'Test Campus Dormitory 1',
    36.00,
    3.00,
    39.00,
    0,
    'Initial test order'
);

ALTER TABLE orders ALTER COLUMN id RESTART WITH 2;
