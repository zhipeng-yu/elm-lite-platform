INSERT INTO merchant (
    id,
    account,
    password_hash,
    merchant_name,
    contact_name,
    contact_phone,
    status
) VALUES (
    1,
    'shop_test_merchant',
    'test_password_hash',
    'Shop Test Merchant',
    'Test Contact',
    '19900000002',
    1
);

INSERT INTO shop (
    id,
    merchant_id,
    shop_name,
    description,
    address,
    start_price,
    delivery_price,
    business_status
) VALUES (
    1,
    1,
    'Campus Food Shop',
    'Shop mapper test data',
    'Test Address 1',
    15.00,
    3.00,
    1
);

ALTER TABLE merchant ALTER COLUMN id RESTART WITH 2;
ALTER TABLE shop ALTER COLUMN id RESTART WITH 2;
