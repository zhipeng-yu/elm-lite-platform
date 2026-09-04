package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.User;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public LoginResult login(String username, String password) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "账号已禁用");
        }
        return new LoginResult(jwtTokenService.issue(user.getId(), JwtTokenService.AccountType.USER), user);
    }

    public record LoginResult(String accessToken, User user) {
    }
}
