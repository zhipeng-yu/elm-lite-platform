package com.elmlite.platform.merchant;

import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.mapper.MerchantMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        scripts = "/db/h2/merchant-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class MerchantLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    private Merchant merchant;

    @BeforeEach
    void prepareMerchant() {
        merchant = new Merchant();
        merchant.setAccount("login_merchant");
        merchant.setPasswordHash(passwordEncoder.encode("test_password_123"));
        merchant.setMerchantName("校园美食店");
        merchant.setContactName("测试联系人");
        merchant.setContactPhone("19900000003");
        merchant.setStatus(1);
        merchantMapper.insert(merchant);
    }

    @Test
    void shouldLoginAndIssueMerchantToken() throws Exception {
        var result = login("login_merchant", "test_password_123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.merchant.id")
                        .value(merchant.getId().intValue()))
                .andExpect(jsonPath("$.data.merchant.account")
                        .value("login_merchant"))
                .andExpect(jsonPath("$.data.merchant.merchantName")
                        .value("校园美食店"))
                .andExpect(jsonPath("$.data.merchant.password").doesNotExist())
                .andExpect(jsonPath("$.data.merchant.passwordHash").doesNotExist())
                .andReturn();

        String token = objectMapper.readTree(
                        result.getResponse().getContentAsByteArray())
                .path("data").path("accessToken").asText();

        Jwt jwt = jwtDecoder.decode(token);
        assertEquals(merchant.getId().toString(), jwt.getSubject());
        assertEquals("MERCHANT", jwt.getClaimAsString("accountType"));
        assertEquals("elm-lite-platform", jwt.getClaimAsString("iss"));
        assertEquals(3600L,
                Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).getSeconds());
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        expectInvalidCredentials("login_merchant", "wrong_password");
    }

    @Test
    void shouldRejectMissingAccountWithSameMessage() throws Exception {
        expectInvalidCredentials("missing_merchant", "test_password_123");
    }

    @Test
    void shouldRejectDisabledMerchant() throws Exception {
        merchant.setStatus(0);
        merchantMapper.updateById(merchant);

        login("login_merchant", "test_password_123")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("账号已禁用"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"account", "password"})
    void shouldRejectBlankCredentials(String field) throws Exception {
        String account = field.equals("account") ? "   " : "login_merchant";
        String password = field.equals("password") ? "   " : "test_password_123";

        login(account, password)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.fieldErrors." + field).exists());
    }

    private void expectInvalidCredentials(String account, String password)
            throws Exception {
        login(account, password)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("账号或密码错误"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private ResultActions login(String account, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/merchant/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "account", account,
                        "password", password
                ))));
    }
}
