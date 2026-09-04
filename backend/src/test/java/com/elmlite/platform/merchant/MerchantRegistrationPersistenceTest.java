package com.elmlite.platform.merchant;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.mapper.MerchantMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        scripts = {
                "/db/h2/merchant-schema.sql",
                "/db/h2/merchant-data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class MerchantRegistrationPersistenceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @ParameterizedTest
    @ValueSource(ints = {8, 72})
    void shouldPersistMerchantAtPasswordLengthBoundaries(int length)
            throws Exception {
        String account = "boundary_merchant";
        String password = "a".repeat(length);

        var result = mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration(account, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.account").value(account))
                .andExpect(jsonPath("$.data.merchantName").value("校园美食店"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andReturn();

        Merchant saved = merchantMapper.selectOne(
                Wrappers.<Merchant>lambdaQuery()
                        .eq(Merchant::getAccount, account));

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("校园美食店", saved.getMerchantName());
        assertEquals("测试联系人", saved.getContactName());
        assertEquals("19900000003", saved.getContactPhone());
        assertEquals(Integer.valueOf(1), saved.getStatus());

        assertNotEquals(password, saved.getPasswordHash());
        assertTrue(saved.getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches(password, saved.getPasswordHash()));
        assertFalse(passwordEncoder.matches("wrong_password", saved.getPasswordHash()));

        long responseId = objectMapper.readTree(
                result.getResponse().getContentAsByteArray())
                .path("data").path("id").asLong();
        assertEquals(saved.getId().longValue(), responseId);
    }

    @Test
    void shouldRejectDuplicateAccountWithoutAddingRow() throws Exception {
        Merchant before = merchantMapper.selectOne(
                Wrappers.<Merchant>lambdaQuery()
                        .eq(Merchant::getAccount, "demo_merchant"));
        assertNotNull(before);
        Long countBefore = merchantMapper.selectCount(null);

        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration("demo_merchant", "12345678")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.msg").value("商家账号已存在"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        assertEquals(countBefore, merchantMapper.selectCount(null));

        Merchant after = merchantMapper.selectById(before.getId());
        assertNotNull(after);
        assertEquals(before.getPasswordHash(), after.getPasswordHash());
        assertEquals(before.getMerchantName(), after.getMerchantName());
    }

    private String registration(String account, String password)
            throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "account", account,
                "password", password,
                "merchantName", "校园美食店",
                "contactName", "测试联系人",
                "contactPhone", "19900000003"
        ));
    }
}