package com.elmlite.platform.admin;

import com.elmlite.platform.entity.Admin;
import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.entity.User;
import com.elmlite.platform.mapper.AdminMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        scripts = "/db/h2/admin-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class AdminDisableTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long userId;
    private Long merchantId;

    @BeforeEach
    void prepareData() {
        Admin admin = new Admin();
        admin.setUsername("root_admin");
        admin.setPasswordHash(passwordEncoder.encode("admin_password_123"));
        admin.setStatus(1);
        adminMapper.insert(admin);

        User user = new User();
        user.setUsername("target_user");
        user.setNickname("目标用户");
        user.setPasswordHash(passwordEncoder.encode("user_password_123"));
        user.setStatus(1);
        userMapper.insert(user);
        userId = user.getId();

        Merchant merchant = new Merchant();
        merchant.setAccount("target_merchant");
        merchant.setMerchantName("目标商家");
        merchant.setContactName("联系人");
        merchant.setContactPhone("19900000005");
        merchant.setPasswordHash(passwordEncoder.encode("merchant_password_123"));
        merchant.setStatus(1);
        merchantMapper.insert(merchant);
        merchantId = merchant.getId();
    }

    private String adminToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "root_admin", "password": "admin_password_123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return response.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
    }

    private String userToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "target_user", "password": "user_password_123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return response.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void disabledUserTokenIsRejectedAndReenableRestoresAccess() throws Exception {
        String token = adminToken();
        String oldUserToken = userToken();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + oldUserToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/users/" + userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": 0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value(0));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + oldUserToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(patch("/api/v1/admin/users/" + userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": 1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + oldUserToken))
                .andExpect(status().isOk());
    }

    @Test
    void disabledMerchantCannotLoginUntilReenabled() throws Exception {
        String token = adminToken();

        mockMvc.perform(patch("/api/v1/admin/merchants/" + merchantId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": 0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(0));

        mockMvc.perform(post("/api/v1/merchant/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account": "target_merchant", "password": "merchant_password_123"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("账号已禁用"));

        mockMvc.perform(patch("/api/v1/admin/merchants/" + merchantId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": 1}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/merchant/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account": "target_merchant", "password": "merchant_password_123"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidStatusMissingStatusAndUnknownIds() throws Exception {
        String token = adminToken();

        mockMvc.perform(patch("/api/v1/admin/users/" + userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": 2}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(patch("/api/v1/admin/users/" + userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(patch("/api/v1/admin/users/999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": 0}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(patch("/api/v1/admin/merchants/999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": 0}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
