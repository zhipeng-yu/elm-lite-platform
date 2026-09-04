package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.MerchantMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MerchantAuthService {

    private final MerchantMapper merchantMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public MerchantAuthService(
            MerchantMapper merchantMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService) {
        this.merchantMapper = merchantMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public LoginResult login(String account, String password) {
        Merchant merchant = merchantMapper.selectOne(
                Wrappers.<Merchant>lambdaQuery()
                        .eq(Merchant::getAccount, account));

        if (merchant == null
                || !passwordEncoder.matches(password, merchant.getPasswordHash())) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        if (!Integer.valueOf(1).equals(merchant.getStatus())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "账号已禁用");
        }

        String token = jwtTokenService.issue(
                merchant.getId(), JwtTokenService.AccountType.MERCHANT);
        return new LoginResult(token, merchant);
    }

    public record LoginResult(String accessToken, Merchant merchant) {
        @Override
        public String toString() {
            return "LoginResult[REDACTED]";
        }
    }
}
