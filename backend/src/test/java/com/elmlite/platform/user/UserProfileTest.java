package com.elmlite.platform.user;

import com.elmlite.platform.entity.User;
import com.elmlite.platform.mapper.UserMapper;
import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.JwtTokenService.AccountType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        scripts = {"/db/h2/user-schema.sql", "/db/h2/user-data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class UserProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private UserMapper userMapper;

    @Test
    void getsCurrentUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearer(userToken(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("demo_user"))
                .andExpect(jsonPath("$.data.displayName").value("演示用户"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void updatesOnlyCurrentUsersDisplayName() throws Exception {
        String content = objectMapper.writeValueAsString(Map.of(
                "displayName", "新昵称",
                "username", "cannot_change"));

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", bearer(userToken(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("demo_user"))
                .andExpect(jsonPath("$.data.displayName").value("新昵称"));

        User saved = userMapper.selectById(1L);
        assertEquals("demo_user", saved.getUsername());
        assertEquals("新昵称", saved.getNickname());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "超过五十个字的昵称超过五十个字的昵称超过五十个字的昵称超过五十个字的昵称超过五十个字的昵称超过五十个字的昵称"})
    void rejectsInvalidDisplayName(String displayName) throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", bearer(userToken(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("displayName", displayName))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.fieldErrors.displayName").isString());
    }

    @Test
    void rejectsMissingToken() throws Exception {
        expectUnauthorized(null);
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        expectUnauthorized("Bearer invalid-token");
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        expectUnauthorized(bearer(expiredUserToken()));
    }

    @Test
    void rejectsMerchantToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", bearer(jwtTokenService.issue(1L, AccountType.MERCHANT))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("无权操作"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void rejectsTokenForMissingUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearer(userToken(999L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("用户不存在"));
    }

    @Test
    void rejectsDisabledCurrentUser() throws Exception {
        User user = userMapper.selectById(1L);
        user.setStatus(0);
        userMapper.updateById(user);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearer(userToken(1L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("账号已禁用"));
    }

    private void expectUnauthorized(String authorization) throws Exception {
        var request = get("/api/v1/users/me");
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("未登录或Token无效"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private String userToken(long userId) {
        return jwtTokenService.issue(userId, AccountType.USER);
    }

    private String expiredUserToken() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("elm-lite-platform")
                .subject("1")
                .issuedAt(now.minusSeconds(7200))
                .expiresAt(now.minusSeconds(3600))
                .claim("accountType", AccountType.USER.name())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
