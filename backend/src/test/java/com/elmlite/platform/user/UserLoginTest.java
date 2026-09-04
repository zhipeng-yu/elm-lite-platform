package com.elmlite.platform.user;

import com.elmlite.platform.entity.User;
import com.elmlite.platform.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        scripts = {"/db/h2/user-schema.sql", "/db/h2/user-data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class UserLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    @BeforeEach
    void useBcryptPassword() {
        User user = userMapper.selectById(1L);
        user.setPasswordHash(passwordEncoder.encode("12345678"));
        userMapper.updateById(user);
    }

    @Test
    void logsInWithUserIdentityToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("demo_user", "12345678")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.user.id").value(1))
                .andExpect(jsonPath("$.data.user.username").value("demo_user"))
                .andExpect(jsonPath("$.data.user.displayName").value("演示用户"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        JsonNode body = objectMapper.readTree(response);
        String token = body.at("/data/accessToken").asText();
        JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(token.split("\\.")[1]));

        assertEquals(Set.of("iss", "sub", "iat", "exp", "accountType"),
                objectMapper.convertValue(claims, java.util.Map.class).keySet());
        assertEquals("elm-lite-platform", claims.get("iss").asText());
        assertEquals("1", claims.get("sub").asText());
        assertEquals("USER", claims.get("accountType").asText());
        assertEquals(3600, claims.get("exp").asLong() - claims.get("iat").asLong());
    }

    @ParameterizedTest
    @MethodSource("wrongCredentials")
    void rejectsWrongCredentials(String username, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login(username, password)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("账号或密码错误"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void rejectsDisabledUser() throws Exception {
        User user = userMapper.selectById(1L);
        user.setStatus(0);
        userMapper.updateById(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("demo_user", "12345678")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("账号已禁用"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void rejectsMissingPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("demo_user", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("参数校验失败"))
                .andExpect(jsonPath("$.data.fieldErrors.password").isString());
    }

    private String login(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginBody(username, password));
    }

    private static Stream<Arguments> wrongCredentials() {
        return Stream.of(
                Arguments.of("missing_user", "12345678"),
                Arguments.of("demo_user", "wrong_password"));
    }

    private record LoginBody(String username, String password) {
    }
}
