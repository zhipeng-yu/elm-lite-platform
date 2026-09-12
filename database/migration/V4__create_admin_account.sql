CREATE TABLE admin_account (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '管理员编号',
    username VARCHAR(50) NOT NULL COMMENT '管理员登录名',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码摘要，不保存明文',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0 停用、1 启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_account_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员账号表，无公开注册入口，由受控初始化创建';
