package com.elmlite.platform.merchant;

import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.service.MerchantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MerchantRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MerchantService merchantService;

    @Test
    void shouldRegisterWithoutTokenAndExcludePasswordFromResponse()
            throws Exception {
        Merchant merchant = new Merchant();
        merchant.setId(10L);
        merchant.setAccount("new_merchant");
        merchant.setMerchantName("校园美食店");
        merchant.setPasswordHash("encoded_password");
        merchant.setStatus(1);

        when(merchantService.register(
                "new_merchant", "test_password_123",
                "校园美食店", "测试联系人", "19900000003"
        )).thenReturn(merchant);

        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "new_merchant",
                                  "password": "test_password_123",
                                  "merchantName": "校园美食店",
                                  "contactName": "测试联系人",
                                  "contactPhone": "19900000003"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.account").value("new_merchant"))
                .andExpect(jsonPath("$.data.merchantName").value("校园美食店"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        verify(merchantService).register(
                "new_merchant", "test_password_123",
                "校园美食店", "测试联系人", "19900000003"
        );
    }
    @Test
    void shouldRejectBlankAccount() throws Exception {
        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "   ",
                                  "password": "test_password_123",
                                  "merchantName": "校园美食店",
                                  "contactName": "测试联系人",
                                  "contactPhone": "19900000003"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.fieldErrors.account").exists());

        org.mockito.Mockito.verifyNoInteractions(merchantService);
    }
}
