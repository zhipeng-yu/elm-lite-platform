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
    'demo_merchant',
    'test_password_hash',
    'Demo Merchant',
    'Test Contact',
    '19900000002',
    1
);

ALTER TABLE merchant ALTER COLUMN id RESTART WITH 2;
