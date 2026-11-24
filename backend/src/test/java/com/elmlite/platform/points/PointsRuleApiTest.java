package com.elmlite.platform.points;

import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.JwtTokenService.AccountType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:points_rule_api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql(scripts = {"/db/h2/admin-schema.sql", "/db/h2/points-rule-schema.sql"})
class PointsRuleApiTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenService tokens;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void administratorReadsAndUpdatesRules() throws Exception {
        mvc.perform(get("/api/v1/admin/points/rules")
                        .header("Authorization", bearer(1, AccountType.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)));

        mvc.perform(get("/api/v1/admin/points/rules/ORDER")
                        .header("Authorization", bearer(1, AccountType.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unitsPerAward").value(100))
                .andExpect(jsonPath("$.data.pointsPerAward").value(1))
                .andExpect(jsonPath("$.data.validDays").value(365));

        mvc.perform(put("/api/v1/admin/points/rules/ORDER")
                        .header("Authorization", bearer(1, AccountType.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "unitsPerAward", 100,
                                "pointsPerAward", 2,
                                "validDays", 30,
                                "enabled", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointsPerAward").value(2))
                .andExpect(jsonPath("$.data.validDays").value(30))
                .andExpect(jsonPath("$.data.enabled").value(false));

        mvc.perform(get("/api/v1/admin/points/rules/ORDER")
                        .header("Authorization", bearer(1, AccountType.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointsPerAward").value(2))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"unitsPerAward\":0,\"pointsPerAward\":1,\"validDays\":30,\"enabled\":true}",
            "{\"unitsPerAward\":1,\"pointsPerAward\":0,\"validDays\":30,\"enabled\":true}",
            "{\"unitsPerAward\":1,\"pointsPerAward\":1,\"validDays\":0,\"enabled\":true}",
            "{\"unitsPerAward\":1,\"pointsPerAward\":1,\"validDays\":36501,\"enabled\":true}",
            "{\"unitsPerAward\":10000000000,\"pointsPerAward\":1,\"validDays\":30,\"enabled\":true}",
            "{\"unitsPerAward\":1,\"pointsPerAward\":1,\"validDays\":30}"
    })
    void rejectsInvalidRules(String body) throws Exception {
        mvc.perform(put("/api/v1/admin/points/rules/ORDER")
                        .header("Authorization", bearer(1, AccountType.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checksRolesAndAdministratorStatus() throws Exception {
        mvc.perform(get("/api/v1/admin/points/rules"))
                .andExpect(status().isUnauthorized());

        for (AccountType type : new AccountType[]{
                AccountType.USER, AccountType.MERCHANT, AccountType.RIDER}) {
            mvc.perform(get("/api/v1/admin/points/rules")
                            .header("Authorization", bearer(1, type)))
                    .andExpect(status().isForbidden());
        }

        mvc.perform(get("/api/v1/admin/points/rules")
                        .header("Authorization", bearer(2, AccountType.ADMIN)))
                .andExpect(status().isForbidden());

        jdbc.update("UPDATE admin_account SET status=0 WHERE id=1");

        mvc.perform(get("/api/v1/admin/points/rules")
                        .header("Authorization", bearer(1, AccountType.ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownOrMissingRuleIsNotFound() throws Exception {
        mvc.perform(get("/api/v1/admin/points/rules/UNKNOWN")
                        .header("Authorization", bearer(1, AccountType.ADMIN)))
                .andExpect(status().isNotFound());

        jdbc.update("DELETE FROM points_rule WHERE source_type='ORDER'");

        mvc.perform(get("/api/v1/admin/points/rules/ORDER")
                        .header("Authorization", bearer(1, AccountType.ADMIN)))
                .andExpect(status().isNotFound());
    }

    private String bearer(long id, AccountType type) {
        return "Bearer " + tokens.issue(id, type);
    }
}
