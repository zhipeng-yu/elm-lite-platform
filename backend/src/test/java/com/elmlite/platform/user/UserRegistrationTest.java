package com.elmlite.platform.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.User;
import com.elmlite.platform.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        scripts = {"/db/h2/user-schema.sql", "/db/h2/user-data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class UserRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void registersUserWithoutExposingPassword() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration("new_user", "12345678", "新用户")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.username").value("new_user"))
                .andExpect(jsonPath("$.data.displayName").value("新用户"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        User saved = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, "new_user"));
        assertNotNull(saved);
        assertNotEquals("12345678", saved.getPasswordHash());
        assertTrue(saved.getPasswordHash().startsWith("$2"));
        assertEquals(1, saved.getStatus());
    }

    @Test
    void rejectsDuplicateUsername() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration("demo_user", "12345678", "重复用户")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.msg").value("用户名已存在"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @ParameterizedTest
    @MethodSource("invalidRegistrations")
    void rejectsInvalidRegistration(String username, String password, String displayName, String field)
            throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration(username, password, displayName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("参数校验失败"))
                .andExpect(jsonPath("$.data.fieldErrors." + field).isString());
    }

    private String registration(String username, String password, String displayName) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password,
                "displayName", displayName));
    }

    private static Stream<Arguments> invalidRegistrations() {
        return Stream.of(
                Arguments.of("", "12345678", "用户", "username"),
                Arguments.of("u".repeat(51), "12345678", "用户", "username"),
                Arguments.of("user", "1234567", "用户", "password"),
                Arguments.of("user", "12345678", "", "displayName"),
                Arguments.of("user", "12345678", "名".repeat(51), "displayName"));
    }
}
