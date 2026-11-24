package com.elmlite.platform.points;

import com.elmlite.platform.mapper.PointsMallItemMapper;
import com.elmlite.platform.mapper.PointsOrderSpendMapper;
import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.JwtTokenService.AccountType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:points_mall_api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql({"/db/h2/points-mall-reset.sql", "/db/h2/points-grant-reset.sql", "/db/h2/admin-schema.sql",
      "/db/h2/points-grant-schema.sql", "/db/h2/points-mall-schema.sql"})
class PointsMallApiTest {
    @Autowired MockMvc mvc;
    @Autowired JwtTokenService tokens;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean PointsMallItemMapper items;
    @MockitoSpyBean PointsOrderSpendMapper batches;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO points_mall_item(id,name,points_cost,stock,enabled) VALUES(1,'Tea',20,3,1),(2,'Hidden',15,3,0),(3,'One left',20,1,1)");
        jdbc.execute("ALTER TABLE points_mall_item ALTER COLUMN id RESTART WITH 10");
        grant(1, 1, 10, today().minusDays(1), today().plusDays(1));
        grant(2, 1, 40, today().minusDays(1), today().plusDays(3));
        grant(3, 1, 100, today().minusDays(3), today().minusDays(1));
        grant(4, 2, 50, today().minusDays(1), today().plusDays(2));
    }

    @Test
    void usersSeeEnabledItemsAndAdministratorsCanCreateAndUpdateThem() throws Exception {
        mvc.perform(get("/api/v1/points/mall/items").header("Authorization", auth(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(get("/api/v1/admin/points/mall/items").header("Authorization", auth(1, AccountType.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(3));
        var created = adminPost(1, "{\"name\":\"  Pencil  \",\"pointsCost\":25,\"stock\":4,\"enabled\":true}");
        assertEquals(200, created.getResponse().getStatus());
        long id = data(created).path("id").asLong();
        assertEquals("Pencil", data(created).path("name").asText());
        assertEquals(200, adminPut(1, id, "{\"name\":\"Pen\",\"pointsCost\":30,\"stock\":2,\"enabled\":false}").getResponse().getStatus());
        assertEquals(404, adminPut(1, 999, "{\"name\":\"X\",\"pointsCost\":1,\"stock\":1,\"enabled\":true}").getResponse().getStatus());
        assertEquals(403, adminPost(2, "{\"name\":\"X\",\"pointsCost\":1,\"stock\":1,\"enabled\":true}").getResponse().getStatus());
        assertEquals(403, adminPost(99, "{\"name\":\"X\",\"pointsCost\":1,\"stock\":1,\"enabled\":true}").getResponse().getStatus());
        doReturn(0).when(items).updateById(any(com.elmlite.platform.entity.PointsMallItem.class));
        assertEquals(409, adminPut(1, 1, "{\"name\":\"X\",\"pointsCost\":1,\"stock\":1,\"enabled\":true}").getResponse().getStatus());
    }

    @Test
    void redemptionUsesEarliestPointsAndRetryKeepsTheOriginalSnapshot() throws Exception {
        var first = redeem(1, 1, 2, "first");
        assertEquals(200, first.getResponse().getStatus());
        JsonNode original = data(first);
        assertEquals(40, original.path("totalPoints").asLong());
        assertEquals("Tea", original.path("itemName").asText());
        assertEquals(0, remaining(1)); assertEquals(10, remaining(2)); assertEquals(100, remaining(3));
        assertEquals(1, stock(1)); assertEquals(2, count("points_mall_allocation"));
        assertEquals(200, adminPut(1, 1, "{\"name\":\"Changed\",\"pointsCost\":99,\"stock\":1,\"enabled\":false}").getResponse().getStatus());
        var retry = redeem(1, 1, 2, "first");
        assertEquals(200, retry.getResponse().getStatus());
        assertEquals(original.path("id").asLong(), data(retry).path("id").asLong());
        assertEquals("Tea", data(retry).path("itemName").asText());
        assertEquals(original.path("createdAt").asText(), data(retry).path("createdAt").asText());
        assertEquals(409, redeem(1, 1, 1, "first").getResponse().getStatus());
        assertEquals(409, redeem(1, 3, 2, "first").getResponse().getStatus());
        assertEquals(1, count("points_mall_redemption")); assertEquals(1, stock(1));
        mvc.perform(get("/api/v1/points/mall/redemptions").header("Authorization", auth(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].totalPoints").value(40));
        mvc.perform(get("/api/v1/points/mall/redemptions").header("Authorization", auth(2, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(get("/api/v1/points/me").header("Authorization", auth(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.availablePoints").value(10));
    }

    @Test
    void insufficientPointsDoNotChangeStockOrPoints() throws Exception {
        assertEquals(409, redeem(1, 1, 3, "poor").getResponse().getStatus());
        assertEquals(3, stock(1)); assertEquals(10, remaining(1)); assertEquals(40, remaining(2));
        assertEquals(0, count("points_mall_redemption")); assertEquals(0, count("points_mall_allocation"));
    }

    @Test
    void checksAccountsAvailabilityAndRoles() throws Exception {
        mvc.perform(get("/api/v1/points/mall/items")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/points/mall/items").header("Authorization", auth(1, AccountType.MERCHANT))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/points/mall/items").header("Authorization", auth(3, AccountType.USER))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/points/mall/redemptions").header("Authorization", auth(3, AccountType.USER))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/points/mall/items").header("Authorization", auth(1, AccountType.USER))).andExpect(status().isForbidden());
        assertEquals(403, redeem(3, 1, 1, "disabled_user").getResponse().getStatus());
        assertEquals(404, redeem(99, 1, 1, "missing_user").getResponse().getStatus());
        assertEquals(404, redeem(1, 999, 1, "missing_item").getResponse().getStatus());
        assertEquals(409, redeem(1, 2, 1, "disabled_item").getResponse().getStatus());
        assertEquals(409, redeem(1, 1, 4, "low_stock").getResponse().getStatus());
        assertEquals(0, count("points_mall_redemption"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"itemId\":0,\"quantity\":1,\"requestKey\":\"a\"}",
        "{\"itemId\":1,\"quantity\":0,\"requestKey\":\"a\"}",
        "{\"itemId\":1,\"quantity\":101,\"requestKey\":\"a\"}",
        "{\"itemId\":1,\"quantity\":1.5,\"requestKey\":\"a\"}",
        "{\"itemId\":1,\"quantity\":1,\"requestKey\":\"bad key\"}",
        "{\"itemId\":1,\"quantity\":1}"
    })
    void rejectsInvalidRedemptionRequests(String input) throws Exception {
        mvc.perform(post("/api/v1/points/mall/redemptions").header("Authorization", auth(1, AccountType.USER))
                .contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isBadRequest());
        assertEquals(0, count("points_mall_redemption"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"name\":\" \",\"pointsCost\":1,\"stock\":1,\"enabled\":true}",
        "{\"name\":\"A\",\"pointsCost\":0,\"stock\":1,\"enabled\":true}",
        "{\"name\":\"A\",\"pointsCost\":1,\"stock\":-1,\"enabled\":true}",
        "{\"name\":\"A\",\"pointsCost\":1,\"stock\":1}"
    })
    void rejectsInvalidItemConfiguration(String input) throws Exception {
        assertEquals(400, adminPost(1, input).getResponse().getStatus());
        assertEquals(3, count("points_mall_item"));
    }

    @Test
    void detectsPointOverflowAndDatabaseFailuresWithoutPartialChanges() throws Exception {
        jdbc.update("UPDATE points_mall_item SET points_cost=9223372036854775807 WHERE id=1");
        assertEquals(409, redeem(1, 1, 2, "overflow").getResponse().getStatus());
        jdbc.update("UPDATE points_mall_item SET points_cost=20 WHERE id=1");
        jdbc.execute("ALTER TABLE points_mall_redemption ADD CONSTRAINT fail_redemption CHECK (request_key <> 'FAIL_INSERT')");
        assertEquals(500, redeem(1, 1, 1, "FAIL_INSERT").getResponse().getStatus());
        jdbc.execute("ALTER TABLE points_mall_redemption DROP CONSTRAINT fail_redemption");
        jdbc.execute("ALTER TABLE points_mall_allocation ADD CONSTRAINT fail_allocation CHECK (points <> 10)");
        assertEquals(500, redeem(1, 1, 1, "FAIL_ALLOC").getResponse().getStatus());
        jdbc.execute("ALTER TABLE points_mall_allocation DROP CONSTRAINT fail_allocation");
        doReturn(0).when(items).deductStock(anyLong(), anyInt());
        assertEquals(409, redeem(1, 1, 1, "stock_changed").getResponse().getStatus());
        org.mockito.Mockito.reset(items);
        doReturn(0).when(batches).deduct(anyLong(), anyLong(), anyLong());
        assertEquals(409, redeem(1, 1, 1, "points_changed").getResponse().getStatus());
        assertEquals(3, stock(1)); assertEquals(10, remaining(1)); assertEquals(40, remaining(2));
        assertEquals(0, count("points_mall_redemption")); assertEquals(0, count("points_mall_allocation"));
    }

    @Test
    void concurrentUsersCannotRedeemTheLastItemTwice() throws Exception {
        var results = concurrently(() -> redeem(1, 3, 1, "user1"), () -> redeem(2, 3, 1, "user2"));
        assertEquals(List.of(200,409), results.stream().map(r -> r.getResponse().getStatus()).sorted().toList());
        assertEquals(0, stock(3)); assertEquals(1, count("points_mall_redemption"));
        assertEquals(80L, jdbc.queryForObject("SELECT SUM(remaining_points) FROM points_grant WHERE id IN(1,2,4)", Long.class));
    }

    @Test
    void concurrentRetriesForOneRequestOnlyRedeemOnce() throws Exception {
        var results = concurrently(() -> redeem(1, 1, 1, "same"), () -> redeem(1, 1, 1, "same"));
        assertEquals(200, results.get(0).getResponse().getStatus());
        assertEquals(200, results.get(1).getResponse().getStatus());
        assertEquals(data(results.get(0)).path("id").asLong(), data(results.get(1)).path("id").asLong());
        assertEquals(2, stock(1)); assertEquals(1, count("points_mall_redemption"));
    }

    private List<MvcResult> concurrently(Callable<MvcResult> one, Callable<MvcResult> two) throws Exception {
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        java.util.function.Function<Callable<MvcResult>,Callable<MvcResult>> wrap = action -> () -> {
            ready.countDown(); if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
            return action.call();
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(wrap.apply(one)); var b = pool.submit(wrap.apply(two));
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        }
    }
    private void grant(long id, long user, long points, LocalDate earned, LocalDate expires) {
        jdbc.update("INSERT INTO points_grant(id,user_id,source_type,event_key,basis_units,awarded_points,remaining_points,earned_on,expires_on) VALUES(?,?,'ORDER',?,?,?,?,?,?)",
                id,user,"mall_" + id,points,points,points,earned,expires);
    }
    private MvcResult redeem(long user,long item,int quantity,String key) throws Exception {
        return mvc.perform(post("/api/v1/points/mall/redemptions").header("Authorization", auth(user,AccountType.USER))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("itemId",item,"quantity",quantity,"requestKey",key)))).andReturn();
    }
    private MvcResult adminPost(long admin,String body) throws Exception {
        return mvc.perform(post("/api/v1/admin/points/mall/items").header("Authorization", auth(admin,AccountType.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }
    private MvcResult adminPut(long admin,long id,String body) throws Exception {
        return mvc.perform(put("/api/v1/admin/points/mall/items/{id}",id).header("Authorization", auth(admin,AccountType.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }
    private String auth(long id,AccountType role) { return "Bearer " + tokens.issue(id,role); }
    private JsonNode data(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()).path("data"); }
    private long remaining(long id) { return jdbc.queryForObject("SELECT remaining_points FROM points_grant WHERE id=?",Long.class,id); }
    private int stock(long id) { return jdbc.queryForObject("SELECT stock FROM points_mall_item WHERE id=?",Integer.class,id); }
    private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table,Integer.class); }
    private LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Shanghai")); }
}
