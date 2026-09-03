-- elm-lite-platform V2 用户登录字段调整
-- 适用于 MySQL 5.5
-- 为现有用户生成独立登录用户名，并允许手机号为空

SET NAMES utf8mb4;

ALTER TABLE users
    ADD COLUMN username VARCHAR(50) DEFAULT NULL
    COMMENT '登录用户名'
    AFTER id;

UPDATE users
SET username = CONCAT('user_', id)
WHERE username IS NULL OR username = '';

ALTER TABLE users
    ADD UNIQUE KEY uk_users_username (username);

ALTER TABLE users
    MODIFY COLUMN username VARCHAR(50) NOT NULL
    COMMENT '登录用户名';

ALTER TABLE users
    MODIFY COLUMN phone VARCHAR(20) DEFAULT NULL
    COMMENT '手机号';
