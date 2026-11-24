package com.elmlite.platform.points;

import com.elmlite.platform.mapper.PointsOrderSpendMapper;
import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.JwtTokenService.AccountType;
import com.elmlite.platform.service.PointsOrderService;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:points_order_api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql({"/db/h2/order-api-schema.sql", "/db/h2/order-api-data.sql", "/db/h2/order-coupon-schema.sql",
      "/db/h2/order-coupon-data.sql", "/db/h2/points-order-schema.sql"})
class PointsOrderApiTest {
    @Autowired MockMvc mvc;
    @Autowired JwtTokenService tokens;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PointsOrderService service;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean PointsOrderSpendMapper spendMapper;

    @BeforeEach
    void seedPoints() {
        insertGrant(1, 1, 40, today().minusDays(1), today().plusDays(2));
        insertGrant(2, 1, 70, today().minusDays(1), today().plusDays(5));
        insertGrant(3, 1, 999, today().minusDays(3), today().minusDays(1));
        insertGrant(4, 2, 20, today().minusDays(1), today().plusDays(3));
    }

    @Test
    void checkoutUsesEarliestUnexpiredBatchesAndCancellationReturnsThemOnce() throws Exception {
        MvcResult created = create(1, "{\"addressId\":1,\"cartItemIds\":[1],\"pointsToUse\":60}");
        assertEquals(201, created.getResponse().getStatus());
        long orderId = data(created).path("id").asLong();
        assertEquals(60, data(created).path("discountAmountCent").asLong());
        assertEquals(3840, data(created).path("totalAmountCent").asLong());
        assertEquals(0, remaining(1));
        assertEquals(50, remaining(2));
        assertEquals(999, remaining(3));
        assertEquals(2, count("points_order_allocation"));
        mvc.perform(get("/api/v1/points/me").header("Authorization", user(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.availablePoints").value(50));
        mvc.perform(get("/api/v1/points/me/spends").header("Authorization", user(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].orderId").value(orderId))
                .andExpect(jsonPath("$.data[0].points").value(60)).andExpect(jsonPath("$.data[0].status").value("SPENT"));
        mvc.perform(get("/api/v1/points/me/spends").header("Authorization", user(2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        assertEquals(404, create(1, "{\"addressId\":1,\"cartItemIds\":[1],\"pointsToUse\":60}").getResponse().getStatus());
        assertEquals(1, count("points_order_spend"));
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/orders/{id}/cancel", orderId).header("Authorization", user(1)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(5));
        }
        assertEquals(40, remaining(1));
        assertEquals(70, remaining(2));
        assertEquals("RETURNED", jdbc.queryForObject("SELECT status FROM points_order_spend", String.class));
        new TransactionTemplate(transactions).executeWithoutResult(tx -> service.returnForOrder(1, orderId));
        assertEquals(40, remaining(1));
        mvc.perform(get("/api/v1/points/me/spends").header("Authorization", user(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].status").value("RETURNED"));
    }

    @Test
    void couponAndPointsShareTheOrderAndCancelAtomically() throws Exception {
        var created = create(1, "{\"addressId\":1,\"cartItemIds\":[1],\"userCouponId\":1,\"pointsToUse\":100}");
        assertEquals(201, created.getResponse().getStatus());
        long id = data(created).path("id").asLong();
        assertEquals(600, data(created).path("discountAmountCent").asLong());
        assertEquals(3300, data(created).path("totalAmountCent").asLong());
        assertEquals(1, number("SELECT status FROM user_coupon WHERE id=1"));
        mvc.perform(post("/api/v1/orders/{id}/cancel", id).header("Authorization", user(1))).andExpect(status().isOk());
        assertEquals(0, number("SELECT status FROM user_coupon WHERE id=1"));
        assertEquals(40, remaining(1));
        assertEquals(70, remaining(2));
        assertEquals(10, number("SELECT stock FROM product WHERE id=1"));
    }

    @Test
    void insufficientPointsRollBackOrderStockCartAndCoupon() throws Exception {
        var result = create(1, "{\"addressId\":1,\"cartItemIds\":[1],\"userCouponId\":1,\"pointsToUse\":111}");
        assertEquals(409, result.getResponse().getStatus());
        assertEquals(0, count("orders"));
        assertEquals(0, count("points_order_spend"));
        assertEquals(0, count("points_order_allocation"));
        assertEquals(40, remaining(1));
        assertEquals(70, remaining(2));
        assertEquals(10, number("SELECT stock FROM product WHERE id=1"));
        assertEquals(1, number("SELECT COUNT(*) FROM cart_item WHERE id=1"));
        assertEquals(0, number("SELECT status FROM user_coupon WHERE id=1"));
    }

    @Test
    void discountCannotExceedProductsAfterCoupon() throws Exception {
        assertEquals(409, create(1, "{\"addressId\":1,\"cartItemIds\":[1],\"userCouponId\":1,\"pointsToUse\":3101}")
                .getResponse().getStatus());
        assertEquals(0, count("orders"));
        assertEquals(10, number("SELECT stock FROM product WHERE id=1"));
        assertEquals(0, number("SELECT status FROM user_coupon WHERE id=1"));
    }

    @Test
    void pointsReturnedAfterExpirationStayExpired() throws Exception {
        var created = create(1, "{\"addressId\":1,\"cartItemIds\":[1],\"pointsToUse\":60}");
        assertEquals(201, created.getResponse().getStatus());
        long id = data(created).path("id").asLong();
        jdbc.update("UPDATE points_grant SET earned_on=?, expires_on=? WHERE id=1", today().minusDays(1), today());
        mvc.perform(post("/api/v1/orders/{id}/cancel", id).header("Authorization", user(1))).andExpect(status().isOk());
        assertEquals(40, remaining(1));
        mvc.perform(get("/api/v1/points/me").header("Authorization", user(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.availablePoints").value(70));
    }

    @Test
    void failuresWhileWritingAllocationsOrDeductingPointsRollBackEverything() throws Exception {
        jdbc.execute("ALTER TABLE points_order_allocation ADD CONSTRAINT fail_allocation CHECK (points <> 40)");
        assertEquals(500, create(1, "{\"addressId\":1,\"cartItemIds\":[1],\"pointsToUse\":60}").getResponse().getStatus());
        assertEquals(0, count("orders"));
        assertEquals(40, remaining(1));
        assertEquals(10, number("SELECT stock FROM product WHERE id=1"));
        jdbc.execute("ALTER TABLE points_order_allocation DROP CONSTRAINT fail_allocation");
        doReturn(0).when(spendMapper).deduct(anyLong(), anyLong(), anyLong());
        assertEquals(409, create(1, "{\"addressId\":1,\"cartItemIds\":[1],\"pointsToUse\":60}").getResponse().getStatus());
        assertEquals(0, count("orders"));
        assertEquals(40, remaining(1));
    }

    @Test
    void inconsistentRefundsRollBackOrderAndInventory() throws Exception {
        var created = create(1, "{\"addressId\":1,\"cartItemIds\":[1],\"pointsToUse\":60}");
        assertEquals(201, created.getResponse().getStatus());
        long id = data(created).path("id").asLong();
        jdbc.update("UPDATE points_order_spend SET user_id=2 WHERE order_id=?", id);
        mvc.perform(post("/api/v1/orders/{id}/cancel", id).header("Authorization", user(1))).andExpect(status().isForbidden());
        assertEquals(8, number("SELECT stock FROM product WHERE id=1"));
        jdbc.update("UPDATE points_order_spend SET user_id=1, status='BROKEN' WHERE order_id=?", id);
        mvc.perform(post("/api/v1/orders/{id}/cancel", id).header("Authorization", user(1))).andExpect(status().isConflict());
        assertEquals(0, remaining(1));
        jdbc.update("UPDATE points_order_spend SET status='SPENT' WHERE order_id=?", id);
        jdbc.update("DELETE FROM points_order_allocation WHERE grant_id=2");
        mvc.perform(post("/api/v1/orders/{id}/cancel", id).header("Authorization", user(1))).andExpect(status().isConflict());
        assertEquals(0, remaining(1));
        jdbc.update("INSERT INTO points_order_allocation(spend_id, grant_id, points) SELECT id, 2, 21 FROM points_order_spend WHERE order_id=?", id);
        mvc.perform(post("/api/v1/orders/{id}/cancel", id).header("Authorization", user(1))).andExpect(status().isConflict());
        assertEquals(0, remaining(1));
        jdbc.update("UPDATE points_order_allocation SET points=20 WHERE grant_id=2");
        jdbc.update("UPDATE points_grant SET remaining_points=awarded_points WHERE id=1");
        mvc.perform(post("/api/v1/orders/{id}/cancel", id).header("Authorization", user(1))).andExpect(status().isConflict());
        assertEquals(0, number("SELECT order_status FROM orders WHERE id=" + id));
        assertEquals(8, number("SELECT stock FROM product WHERE id=1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.5", "\"1\"", "10000000000", "false"})
    void rejectsInvalidPointsAmounts(String value) throws Exception {
        assertEquals(400, create(1, "{\"addressId\":1,\"cartItemIds\":[1],\"pointsToUse\":" + value + "}")
                .getResponse().getStatus());
        assertEquals(0, count("orders"));
    }

    @Test
    void rejectsAnonymousOtherRolesAndDisabledUserHistory() throws Exception {
        mvc.perform(get("/api/v1/points/me/spends")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/points/me/spends").header("Authorization", "Bearer " + tokens.issue(1, AccountType.MERCHANT)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/points/me/spends").header("Authorization", user(3))).andExpect(status().isForbidden());
    }

    @Test
    void twoOrdersCannotSpendTheSamePointsConcurrently() throws Exception {
        jdbc.update("UPDATE shop SET start_price=0 WHERE id=1");
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> first = concurrentOrder(ready, start, 1);
            Callable<Integer> second = concurrentOrder(ready, start, 2);
            var a = executor.submit(first);
            var b = executor.submit(second);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            assertEquals(List.of(201, 409), List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)).stream().sorted().toList());
            assertEquals(1, count("orders"));
            assertEquals(1, count("points_order_spend"));
            assertEquals(40, remaining(1) + remaining(2));
        }
    }

    private Callable<Integer> concurrentOrder(CountDownLatch ready, CountDownLatch start, int cart) {
        return () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
            return create(1, "{\"addressId\":1,\"cartItemIds\":[" + cart + "],\"pointsToUse\":70}").getResponse().getStatus();
        };
    }
    private void insertGrant(long id, long user, long points, LocalDate earned, LocalDate expires) {
        jdbc.update("INSERT INTO points_grant(id,user_id,source_type,event_key,basis_units,awarded_points,remaining_points,earned_on,expires_on) VALUES(?,?,'ORDER',?,?,?,?,?,?)",
                id, user, "grant_" + id, points, points, points, earned, expires);
    }
    private MvcResult create(long id, String body) throws Exception {
        return mvc.perform(post("/api/v1/orders").header("Authorization", user(id))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }
    private String user(long id) { return "Bearer " + tokens.issue(id, AccountType.USER); }
    private JsonNode data(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()).path("data"); }
    private long remaining(long id) { return jdbc.queryForObject("SELECT remaining_points FROM points_grant WHERE id=?", Long.class, id); }
    private int count(String table) { return number("SELECT COUNT(*) FROM " + table); }
    private int number(String sql) { return jdbc.queryForObject(sql, Integer.class); }
    private LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Shanghai")); }
}
