package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.Admin;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.AdminMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AdminAuthService(
            AdminMapper adminMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService) {
        this.adminMapper = adminMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public LoginResult login(String username, String password) {
        Admin admin = adminMapper.selectOne(
                Wrappers.<Admin>lambdaQuery()
                        .eq(Admin::getUsername, username));

        if (admin == null
                || !passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        if (!Integer.valueOf(1).equals(admin.getStatus())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "账号已禁用");
        }

        String token = jwtTokenService.issue(
                admin.getId(), JwtTokenService.AccountType.ADMIN);
        return new LoginResult(token, admin);
    }

    public record LoginResult(String accessToken, Admin admin) {
        @Override
        public String toString() {
            return "LoginResult[REDACTED]";
        }
    }
}
