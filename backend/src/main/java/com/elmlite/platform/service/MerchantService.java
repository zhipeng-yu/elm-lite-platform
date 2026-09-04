package com.elmlite.platform.service;

import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.MerchantMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantService {

    private final MerchantMapper merchantMapper;
    private final PasswordEncoder passwordEncoder;

    public MerchantService(MerchantMapper merchantMapper,
                           PasswordEncoder passwordEncoder) {
        this.merchantMapper = merchantMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Merchant register(String account,
                             String password,
                             String merchantName,
                             String contactName,
                             String contactPhone) {
        Merchant merchant = new Merchant();
        merchant.setAccount(account);
        merchant.setPasswordHash(passwordEncoder.encode(password));
        merchant.setMerchantName(merchantName);
        merchant.setContactName(contactName);
        merchant.setContactPhone(contactPhone);
        merchant.setStatus(1);

        try {
            merchantMapper.insert(merchant);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "商家账号已存在"

            );
        }

        return merchant;
    }
}
