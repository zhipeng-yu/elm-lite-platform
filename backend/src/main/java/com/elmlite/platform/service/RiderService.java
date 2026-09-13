package com.elmlite.platform.service;

import com.elmlite.platform.entity.Rider;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.RiderMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiderService {
    private final RiderMapper riders;
    private final PasswordEncoder passwords;

    public RiderService(RiderMapper riders, PasswordEncoder passwords) {
        this.riders = riders;
        this.passwords = passwords;
    }

    @Transactional
    public Rider register(String username, String password, String displayName) {
        Rider rider = new Rider();
        rider.setUsername(username);
        rider.setPasswordHash(passwords.encode(password));
        rider.setDisplayName(displayName);
        rider.setStatus(1);
        try {
            riders.insert(rider);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "骑手账号已存在");
        }
        return rider;
    }
}
