package com.elmlite.platform.service;

import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.MerchantMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MerchantService merchantService;

    @Test
    void shouldRejectDuplicateAccount() {
        when(passwordEncoder.encode("test_password_123"))
                .thenReturn("encoded_password");

        when(merchantMapper.insert(any(Merchant.class)))
                .thenThrow(new DuplicateKeyException("duplicate account"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> merchantService.register(
                        "demo_merchant",
                        "test_password_123",
                        "校园美食店",
                        "测试联系人",
                        "19900000003"
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("商家账号已存在", exception.getMessage());
    }
}