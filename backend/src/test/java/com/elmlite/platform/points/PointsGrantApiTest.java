package com.elmlite.platform.points;

import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.JwtTokenService.AccountType;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:points_grant_api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql(scripts = {"/db/h2/points-grant-reset.sql", "/db/h2/admin-schema.sql", "/db/h2/points-grant-schema.sql"})
class PointsGrantApiTest {
    @Autowired MockMvc mvc;
    @Autowired JwtTokenService tokens;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void newUsersHaveZeroBalanceAndNoHistory() throws Exception {
        mvc.perform(get("/api/v1/points/me").header("Authorization", auth(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.availablePoints").value(0));
        mvc.perform(get("/api/v1/points/me/grants").header("Authorization", auth(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void grantUsesRuleAndIsIdempotentEvenAfterRuleChanges() throws Exception {
        var first = grant(1, AccountType.ADMIN, 1, "ORDER", "order_100", 12345);
        assertEquals(200, first.getResponse().getStatus());
        var original = data(first);
        assertEquals(123, original.path("awardedPoints").asLong());
        assertEquals(123, original.path("remainingPoints").asLong());
        assertEquals(today().plusDays(365).toString(), original.path("expiresOn").asText());
        jdbc.update("UPDATE points_rule SET enabled=0, points_per_award=8 WHERE source_type='ORDER'");
        var retry = grant(1, AccountType.ADMIN, 1, "ORDER", "order_100", 12345);
        assertEquals(200, retry.getResponse().getStatus());
        assertEquals(original.path("id").asLong(), data(retry).path("id").asLong());
        assertEquals(123, data(retry).path("awardedPoints").asLong());
        assertEquals(409, grant(1, AccountType.ADMIN, 1, "ORDER", "order_100", 12346).getResponse().getStatus());
        assertEquals(1, count());
        mvc.perform(get("/api/v1/points/me").header("Authorization", auth(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.availablePoints").value(123));
        mvc.perform(get("/api/v1/points/me/grants").header("Authorization", auth(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].eventKey").value("order_100"))
                .andExpect(jsonPath("$.data[0].expired").value(false));
    }

    @Test
    void sourcesAndUsersAreIsolatedAndZeroPointEventsCanBeRetried() throws Exception {
        assertEquals(200, grant(1, AccountType.ADMIN, 1, "REVIEW", "event", 2).getResponse().getStatus());
        assertEquals(200, grant(1, AccountType.ADMIN, 1, "ACTIVITY", "event", 1).getResponse().getStatus());
        var zero = grant(1, AccountType.ADMIN, 1, "ORDER", "under_threshold", 99);
        assertEquals(200, zero.getResponse().getStatus());
        assertEquals(0, data(zero).path("awardedPoints").asLong());
        assertEquals(200, grant(1, AccountType.ADMIN, 1, "ORDER", "under_threshold", 99).getResponse().getStatus());
        assertEquals(200, grant(1, AccountType.ADMIN, 2, "REVIEW", "event", 1).getResponse().getStatus());
        assertEquals(4, count());
        mvc.perform(get("/api/v1/points/me").header("Authorization", auth(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.availablePoints").value(20));
        mvc.perform(get("/api/v1/points/me/grants").header("Authorization", auth(2, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].awardedPoints").value(5));
    }

    @Test
    void pointsExpireAtTheStartOfTheirExpirationDate() throws Exception {
        assertEquals(200, grant(1, AccountType.ADMIN, 1, "REVIEW", "expired", 2).getResponse().getStatus());
        assertEquals(200, grant(1, AccountType.ADMIN, 1, "ACTIVITY", "valid", 1).getResponse().getStatus());
        jdbc.update("UPDATE points_grant SET earned_on=?, expires_on=? WHERE event_key='expired'",
                today().minusDays(1), today());
        mvc.perform(get("/api/v1/points/me").header("Authorization", auth(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.availablePoints").value(10));
        mvc.perform(get("/api/v1/points/me/grants").header("Authorization", auth(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[1].eventKey").value("expired"))
                .andExpect(jsonPath("$.data[1].expired").value(true));
    }

    @Test
    void rejectsMissingOrDisabledAccountsAndRules() throws Exception {
        assertEquals(403, grant(2, AccountType.ADMIN, 1, "ORDER", "admin_disabled", 100).getResponse().getStatus());
        assertEquals(403, grant(99, AccountType.ADMIN, 1, "ORDER", "admin_missing", 100).getResponse().getStatus());
        assertEquals(404, grant(1, AccountType.ADMIN, 99, "ORDER", "user_missing", 100).getResponse().getStatus());
        assertEquals(403, grant(1, AccountType.ADMIN, 3, "ORDER", "user_disabled", 100).getResponse().getStatus());
        jdbc.update("UPDATE points_rule SET enabled=0 WHERE source_type='ORDER'");
        assertEquals(409, grant(1, AccountType.ADMIN, 1, "ORDER", "disabled_rule", 100).getResponse().getStatus());
        jdbc.update("DELETE FROM points_rule WHERE source_type='REVIEW'");
        assertEquals(404, grant(1, AccountType.ADMIN, 1, "REVIEW", "missing_rule", 1).getResponse().getStatus());
        mvc.perform(get("/api/v1/points/me").header("Authorization", auth(3, AccountType.USER)))
                .andExpect(status().isForbidden());
        assertEquals(0, count());
    }

    @Test
    void onlyActiveUsersAndAdministratorsCanUseTheirOwnEndpoints() throws Exception {
        mvc.perform(get("/api/v1/points/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/points/grants").contentType(MediaType.APPLICATION_JSON)
                .content(body(1, "ORDER", "no_auth", 100))).andExpect(status().isUnauthorized());
        assertEquals(403, grant(1, AccountType.USER, 1, "ORDER", "user", 100).getResponse().getStatus());
        mvc.perform(get("/api/v1/points/me").header("Authorization", auth(1, AccountType.MERCHANT)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/points/me/grants").header("Authorization", auth(1, AccountType.RIDER)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/points/me").header("Authorization", auth(1, AccountType.ADMIN)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"userId\":0,\"sourceType\":\"ORDER\",\"eventKey\":\"a\",\"basisUnits\":100}",
        "{\"userId\":1,\"sourceType\":\"OTHER\",\"eventKey\":\"a\",\"basisUnits\":100}",
        "{\"userId\":1,\"sourceType\":\"ORDER\",\"eventKey\":\"bad key\",\"basisUnits\":100}",
        "{\"userId\":1,\"sourceType\":\"ORDER\",\"eventKey\":\"a\",\"basisUnits\":-1}",
        "{\"userId\":1,\"sourceType\":\"ORDER\",\"eventKey\":\"a\",\"basisUnits\":1.5}",
        "{\"userId\":1,\"sourceType\":\"ORDER\",\"eventKey\":\"a\",\"basisUnits\":10000000000}",
        "{\"userId\":1,\"sourceType\":\"ORDER\",\"basisUnits\":100}"
    })
    void rejectsInvalidInputWithoutCreatingGrants(String input) throws Exception {
        mvc.perform(post("/api/v1/admin/points/grants").header("Authorization", auth(1, AccountType.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isBadRequest());
        assertEquals(0, count());
    }

    @Test
    void detectsCalculationAndBalanceOverflow() throws Exception {
        jdbc.update("UPDATE points_rule SET units_per_award=1, points_per_award=9999999999 WHERE source_type='ORDER'");
        assertEquals(409, grant(1, AccountType.ADMIN, 1, "ORDER", "too_large", 9999999999L).getResponse().getStatus());
        assertEquals(0, count());
        assertEquals(200, grant(1, AccountType.ADMIN, 1, "ORDER", "large_1", 600000000L).getResponse().getStatus());
        assertEquals(200, grant(1, AccountType.ADMIN, 1, "ORDER", "large_2", 600000000L).getResponse().getStatus());
        mvc.perform(get("/api/v1/points/me").header("Authorization", auth(1, AccountType.USER)))
                .andExpect(status().isConflict());
    }

    @Test
    void simultaneousRetriesCreateOnlyOneGrant() throws Exception {
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        Callable<MvcResult> action = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
            return grant(1, AccountType.ADMIN, 1, "ORDER", "parallel", 500);
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(action);
            var two = executor.submit(action);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            List<MvcResult> result = List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS));
            assertEquals(200, result.get(0).getResponse().getStatus());
            assertEquals(200, result.get(1).getResponse().getStatus());
            assertEquals(data(result.get(0)).path("id").asLong(), data(result.get(1)).path("id").asLong());
            assertEquals(1, count());
        }
    }

    private MvcResult grant(long admin, AccountType type, long user, String source, String key, long basis) throws Exception {
        return mvc.perform(post("/api/v1/admin/points/grants").header("Authorization", auth(admin, type))
                .contentType(MediaType.APPLICATION_JSON).content(body(user, source, key, basis))).andReturn();
    }
    private String body(long user, String source, String key, long basis) throws Exception {
        return json.writeValueAsString(Map.of("userId", user, "sourceType", source, "eventKey", key, "basisUnits", basis));
    }
    private String auth(long id, AccountType type) { return "Bearer " + tokens.issue(id, type); }
    private JsonNode data(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString()).path("data");
    }
    private int count() { return jdbc.queryForObject("SELECT COUNT(*) FROM points_grant", Integer.class); }
    private LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Shanghai")); }
}
