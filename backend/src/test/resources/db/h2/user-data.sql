INSERT INTO users (
    id,
    username,
    phone,
    password_hash,
    nickname,
    gender,
    status
) VALUES (
    1,
    'demo_user',
    NULL,
    'test_password_hash',
    '演示用户',
    0,
    1
);

INSERT INTO users (
    id,
    username,
    phone,
    password_hash,
    nickname,
    gender,
    status
) VALUES (
    2,
    'phone_user',
    '19900000001',
    'test_password_hash',
    '手机号用户',
    0,
    1
);

ALTER TABLE users ALTER COLUMN id RESTART WITH 3;
