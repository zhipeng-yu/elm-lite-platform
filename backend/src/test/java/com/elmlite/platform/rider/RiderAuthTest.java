package com.elmlite.platform.rider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:rider_auth;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@Sql("/db/h2/rider-auth-schema.sql")
class RiderAuthTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwords;
    @Autowired private JwtDecoder jwtDecoder;

    @BeforeEach
    void activeRider() {
        jdbc.update("INSERT INTO rider(username,password_hash,display_name,status) VALUES(?,?,?,1)",
                "rider_one", passwords.encode("password123"), "骑手一");
    }

    @Test
    void registersRiderWithoutExposingPassword() throws Exception {
        register("new_rider", "password123", "新骑手")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.username").value("new_rider"))
                .andExpect(jsonPath("$.data.displayName").value("新骑手"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM rider WHERE username='new_rider'", String.class);
        assertNotEquals("password123", hash);
        assertTrue(passwords.matches("password123", hash));
    }

    @Test
    void rejectsDuplicateUsername() throws Exception {
        register("rider_one", "password123", "重复骑手")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @ParameterizedTest
    @ValueSource(strings = {"username", "password", "displayName"})
    void rejectsBlankRegistrationFields(String field) throws Exception {
        mvc.perform(post("/api/v1/riders").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "username", field.equals("username") ? " " : "valid_rider",
                                "password", field.equals("password") ? " " : "password123",
                                "displayName", field.equals("displayName") ? " " : "骑手"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.fieldErrors." + field).exists());
    }

    @Test
    void logsInAndIssuesRiderToken() throws Exception {
        var response = login("rider_one", "password123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.rider.username").value("rider_one"))
                .andReturn();

        String token = json.readTree(response.getResponse().getContentAsByteArray())
                .path("data").path("accessToken").asText();
        var jwt = jwtDecoder.decode(token);
        org.junit.jupiter.api.Assertions.assertEquals("RIDER", jwt.getClaimAsString("accountType"));
    }

    @Test
    void usesSameMessageForMissingAccountAndWrongPassword() throws Exception {
        expectInvalidLogin("missing", "password123");
        expectInvalidLogin("rider_one", "wrong_password");
    }

    @Test
    void rejectsDisabledRider() throws Exception {
        jdbc.update("UPDATE rider SET status=0 WHERE username='rider_one'");
        login("rider_one", "password123")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("账号已禁用"));
    }

    private ResultActions register(String username, String password, String displayName) throws Exception {
        return mvc.perform(post("/api/v1/riders").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "username", username, "password", password, "displayName", displayName))));
    }

    private ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/v1/rider/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", password))));
    }

    private void expectInvalidLogin(String username, String password) throws Exception {
        login(username, password)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("账号或密码错误"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
