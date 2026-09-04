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
import static org.mockito.Mockito.verify;

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
    @Test
    void shouldRegisterMerchantWithEncodedPassword() {
        when(passwordEncoder.encode("test_password_123"))
                .thenReturn("encoded_password");

        when(merchantMapper.insert(any(Merchant.class)))
                .thenAnswer(invocation -> {
                    Merchant saved = invocation.getArgument(0);

                    assertEquals("new_merchant", saved.getAccount());
                    assertEquals("encoded_password", saved.getPasswordHash());
                    assertEquals("校园美食店", saved.getMerchantName());
                    assertEquals("测试联系人", saved.getContactName());
                    assertEquals("19900000003", saved.getContactPhone());
                    assertEquals(Integer.valueOf(1), saved.getStatus());

                    saved.setId(10L);
                    return 1;
                });

        Merchant result = merchantService.register(
                "new_merchant",
                "test_password_123",
                "校园美食店",
                "测试联系人",
                "19900000003"
        );

        assertEquals(Long.valueOf(10L), result.getId());
        verify(passwordEncoder).encode("test_password_123");
        verify(merchantMapper).insert(any(Merchant.class));
    }
}
