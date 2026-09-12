package com.elmlite.platform.admin;

import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.entity.User;
import com.elmlite.platform.mapper.MerchantMapper;
import com.elmlite.platform.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        scripts = "/db/h2/admin-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class AdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void prepareAccounts() {
        User user = new User();
        user.setUsername("normal_user");
        user.setPasswordHash(passwordEncoder.encode("user_password_123"));
        user.setNickname("普通用户");
        user.setStatus(1);
        userMapper.insert(user);

        Merchant merchant = new Merchant();
        merchant.setAccount("normal_merchant");
        merchant.setPasswordHash(passwordEncoder.encode("merchant_password_123"));
        merchant.setMerchantName("普通商家");
        merchant.setContactName("联系人");
        merchant.setContactPhone("19900000004");
        merchant.setStatus(1);
        merchantMapper.insert(merchant);
    }

    private String userToken() throws Exception {
        String body = """
                {"username": "normal_user", "password": "user_password_123"}
                """;
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
    }

    private String merchantToken() throws Exception {
        String body = """
                {"account": "normal_merchant", "password": "merchant_password_123"}
                """;
        return mockMvc.perform(post("/api/v1/merchant/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void anonymousAdminRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void userTokenCannotAccessAdminPaths() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void merchantTokenCannotAccessAdminPaths() throws Exception {
        mockMvc.perform(get("/api/v1/admin/merchants")
                        .header("Authorization", "Bearer " + merchantToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void adminLoginPathIsPublic() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "root_admin", "password": "wrong_password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}
