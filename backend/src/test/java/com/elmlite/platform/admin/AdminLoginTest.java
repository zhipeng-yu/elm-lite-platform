package com.elmlite.platform.admin;

import com.elmlite.platform.entity.Admin;
import com.elmlite.platform.mapper.AdminMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        scripts = "/db/h2/admin-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class AdminLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void shouldLoginAndIssueAdminToken() throws Exception {
        Admin admin = new Admin();
        admin.setUsername("root_admin");
        admin.setPasswordHash(passwordEncoder.encode("admin_password_123"));
        admin.setStatus(1);
        adminMapper.insert(admin);

        String response = mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "root_admin", "password": "admin_password_123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.admin.id").value(admin.getId().intValue()))
                .andExpect(jsonPath("$.data.admin.username").value("root_admin"))
                .andExpect(jsonPath("$.data.admin.password").doesNotExist())
                .andExpect(jsonPath("$.data.admin.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String token = response.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
        Jwt jwt = jwtDecoder.decode(token);
        assertEquals("ADMIN", jwt.getClaimAsString("accountType"));
        assertEquals(Long.toString(admin.getId()), jwt.getSubject());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        Admin admin = new Admin();
        admin.setUsername("root_admin");
        admin.setPasswordHash(passwordEncoder.encode("admin_password_123"));
        admin.setStatus(1);
        adminMapper.insert(admin);

        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "root_admin", "password": "wrong_password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("账号或密码错误"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());
    }

    @Test
    void disabledAdminCannotLogin() throws Exception {
        Admin admin = new Admin();
        admin.setUsername("root_admin");
        admin.setPasswordHash(passwordEncoder.encode("admin_password_123"));
        admin.setStatus(0);
        adminMapper.insert(admin);

        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "root_admin", "password": "admin_password_123"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("账号已禁用"));
    }

    @Test
    void unknownAdminReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "no_such_admin", "password": "whatever_123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value(not("")));
    }
}
