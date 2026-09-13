package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.Rider;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.RiderMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RiderAuthService {
    private final RiderMapper riders;
    private final PasswordEncoder passwords;
    private final JwtTokenService tokens;

    public RiderAuthService(RiderMapper riders, PasswordEncoder passwords, JwtTokenService tokens) {
        this.riders = riders;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    public LoginResult login(String username, String password) {
        Rider rider = riders.selectOne(Wrappers.<Rider>lambdaQuery().eq(Rider::getUsername, username));
        if (rider == null || !passwords.matches(password, rider.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        if (!Integer.valueOf(1).equals(rider.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "账号已禁用");
        }
        return new LoginResult(tokens.issue(rider.getId(), JwtTokenService.AccountType.RIDER), rider);
    }

    public record LoginResult(String accessToken, Rider rider) {
        @Override
        public String toString() { return "LoginResult[REDACTED]"; }
    }
}
