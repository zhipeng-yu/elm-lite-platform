INSERT INTO users (id) VALUES (1);

INSERT INTO delivery_address (
    id,
    user_id,
    receiver_name,
    receiver_phone,
    address_detail,
    address_label,
    is_default
) VALUES (
    1,
    1,
    'Test User',
    '19900000001',
    'Test Campus Dormitory 1',
    'School',
    1
);

ALTER TABLE delivery_address ALTER COLUMN id RESTART WITH 2;
