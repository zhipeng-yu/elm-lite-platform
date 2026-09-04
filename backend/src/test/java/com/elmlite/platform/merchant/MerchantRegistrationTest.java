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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    @Test
    void shouldRejectBlankPassword() throws Exception {
        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "new_merchant",
                                  "password": "   ",
                                  "merchantName": "校园美食店",
                                  "contactName": "测试联系人",
                                  "contactPhone": "19900000003"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.fieldErrors.password").exists());

        org.mockito.Mockito.verifyNoInteractions(merchantService);
    }
    @ParameterizedTest
    @ValueSource(strings = {"merchantName", "contactName", "contactPhone"})
    void shouldRejectBlankMerchantDetails(String field) throws Exception {
        var body = new ObjectMapper().createObjectNode();
        body.put("account", "new_merchant");
        body.put("password", "test_password_123");
        body.put("merchantName", "校园美食店");
        body.put("contactName", "测试联系人");
        body.put("contactPhone", "19900000003");
        body.put(field, "   ");

        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.fieldErrors." + field).exists());

        org.mockito.Mockito.verifyNoInteractions(merchantService);
    }
    @Test
    void shouldReturnConflictForDuplicateAccount() throws Exception {
        when(merchantService.register(
                "new_merchant", "test_password_123",
                "校园美食店", "测试联系人", "19900000003"))
                .thenThrow(new com.elmlite.platform.exception.BusinessException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "商家账号已存在"));

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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.msg").value("商家账号已存在"))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
