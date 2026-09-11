package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.Admin;
import com.elmlite.platform.mapper.AdminMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 管理员账号受控初始化：凭据仅从 ADMIN_USERNAME / ADMIN_PASSWORD 环境变量读取，
 * 不写入仓库、配置文件或日志。未提供凭据或表不存在时跳过初始化。
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminInitializer(
            AdminMapper adminMapper,
            PasswordEncoder passwordEncoder,
            @Value("${admin.username:}") String adminUsername,
            @Value("${admin.password:}") String adminPassword) {
        this.adminMapper = adminMapper;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminUsername == null || adminUsername.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            log.warn("未提供管理员初始化凭据（ADMIN_USERNAME/ADMIN_PASSWORD 环境变量），跳过管理员账号初始化");
            return;
        }
        try {
            Admin existing = adminMapper.selectOne(
                    Wrappers.<Admin>lambdaQuery().eq(Admin::getUsername, adminUsername));
            if (existing != null) {
                log.info("管理员账号已存在，跳过初始化");
                return;
            }
            Admin admin = new Admin();
            admin.setUsername(adminUsername);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setStatus(1);
            adminMapper.insert(admin);
            log.info("管理员账号初始化完成");
        } catch (DataAccessException exception) {
            log.warn("管理员账号表不可用，跳过管理员账号初始化");
        }
    }
}
