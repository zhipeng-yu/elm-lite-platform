package com.elmlite.platform.points;

import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.JwtTokenService.AccountType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:order_points_award;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql({"/db/h2/order-api-schema.sql", "/db/h2/order-api-data.sql", "/db/h2/rider-order-schema.sql",
      "/db/h2/rider-order-data.sql", "/db/h2/order-points-award-schema.sql"})
class OrderPointsAwardApiTest {
    @Autowired MockMvc mvc;
    @Autowired JwtTokenService tokens;
    @Autowired JdbcTemplate jdbc;

    @Test
    void completionAwardsOnlyOnceAndUsesNetProductAmount() throws Exception {
        jdbc.update("UPDATE orders SET discount_amount=2.25, total_amount=18.75 WHERE id=14");
        jdbc.update("UPDATE points_rule SET points_per_award=3, valid_days=30 WHERE source_type='ORDER'");
        assertEquals(200, complete(1, 14));
        assertEquals(1, count());
        assertEquals(1575L, number("SELECT basis_units FROM points_grant WHERE event_key='order:14'"));
        assertEquals(45L, number("SELECT awarded_points FROM points_grant WHERE event_key='order:14'"));
        assertEquals(today().plusDays(30), jdbc.queryForObject("SELECT expires_on FROM points_grant WHERE event_key='order:14'", LocalDate.class));
        assertEquals(200, complete(1, 14));
        assertEquals(1, count());
        mvc.perform(get("/api/v1/points/me").header("Authorization", token(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.availablePoints").value(45));
    }

    @Test
    void zeroRewardIsStillRecordedAndLegacyCompletedOrdersAreNotBackfilled() throws Exception {
        jdbc.update("UPDATE orders SET discount_amount=18 WHERE id=14");
        assertEquals(200, complete(1, 14));
        assertEquals(1, count());
        assertEquals(0L, number("SELECT awarded_points FROM points_grant WHERE event_key='order:14'"));
        assertEquals(200, complete(1, 15));
        assertEquals(1, count());
    }

    @Test
    void disabledOrMissingRuleDoesNotPreventDeliveryOrAwardRetroactively() throws Exception {
        jdbc.update("UPDATE points_rule SET enabled=0 WHERE source_type='ORDER'");
        assertEquals(200, complete(1, 14));
        assertEquals(0, count());
        jdbc.update("UPDATE points_rule SET enabled=1 WHERE source_type='ORDER'");
        assertEquals(200, complete(1, 14));
        assertEquals(0, count());
        jdbc.update("UPDATE orders SET order_status=3 WHERE id=12");
        jdbc.update("DELETE FROM points_rule WHERE source_type='ORDER'");
        assertEquals(200, complete(1, 12));
        assertEquals(0, count());
    }

    @Test
    void disabledCustomerDoesNotStopDeliveryAndCanSeePointsAfterReactivation() throws Exception {
        jdbc.update("UPDATE users SET status=0 WHERE id=1");
        assertEquals(200, complete(1, 14));
        assertEquals(1, count());
        mvc.perform(get("/api/v1/points/me").header("Authorization", token(1, AccountType.USER)))
                .andExpect(status().isForbidden());
        jdbc.update("UPDATE users SET status=1 WHERE id=1");
        mvc.perform(get("/api/v1/points/me").header("Authorization", token(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.availablePoints").value(18));
    }

    @Test
    void failureToWritePointsRollsBackCompletionAndCanBeRetried() throws Exception {
        jdbc.execute("ALTER TABLE points_grant ADD CONSTRAINT fail_order_award CHECK (event_key <> 'order:14')");
        assertEquals(500, complete(1, 14));
        assertEquals(3L, number("SELECT order_status FROM orders WHERE id=14"));
        assertEquals(0, count());
        jdbc.execute("ALTER TABLE points_grant DROP CONSTRAINT fail_order_award");
        assertEquals(200, complete(1, 14));
        assertEquals(1, count());
    }

    @Test
    void invalidDiscountOrExcessiveAwardRollsBackTheOrder() throws Exception {
        jdbc.update("UPDATE orders SET discount_amount=19 WHERE id=14");
        assertEquals(409, complete(1, 14));
        assertEquals(3L, number("SELECT order_status FROM orders WHERE id=14"));
        jdbc.update("UPDATE orders SET product_amount=99999999.99, discount_amount=0 WHERE id=14");
        jdbc.update("UPDATE points_rule SET units_per_award=1, points_per_award=9999999999 WHERE source_type='ORDER'");
        assertEquals(409, complete(1, 14));
        assertEquals(3L, number("SELECT order_status FROM orders WHERE id=14"));
        assertEquals(0, count());
    }

    @Test
    void existingEventIsReusedOrConflictsWhenItsBasisDiffers() throws Exception {
        insertEvent(1800);
        assertEquals(200, complete(1, 14));
        assertEquals(1, count());
        jdbc.update("UPDATE orders SET order_status=3 WHERE id=14");
        jdbc.update("UPDATE points_grant SET basis_units=1700 WHERE event_key='order:14'");
        assertEquals(409, complete(1, 14));
        assertEquals(3L, number("SELECT order_status FROM orders WHERE id=14"));
        assertEquals(1, count());
    }

    @Test
    void wrongRiderOrOrderStateCannotEarnPoints() throws Exception {
        assertEquals(403, complete(2, 14));
        assertEquals(403, complete(3, 14));
        assertEquals(409, complete(1, 12));
        assertEquals(404, complete(1, 999));
        assertEquals(0, count());
    }

    @Test
    void concurrentCompletionCreatesOneGrant() throws Exception {
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> run = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                return complete(1, 14);
            };
            var a = pool.submit(run);
            var b = pool.submit(run);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            assertEquals(List.of(200, 200), List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)));
            assertEquals(1, count());
            assertEquals(18L, number("SELECT awarded_points FROM points_grant WHERE event_key='order:14'"));
        }
    }

    private int complete(long riderId, long orderId) throws Exception {
        return mvc.perform(post("/api/v1/rider/orders/{id}/complete", orderId)
                .header("Authorization", token(riderId, AccountType.RIDER))).andReturn().getResponse().getStatus();
    }
    private String token(long id, AccountType role) { return "Bearer " + tokens.issue(id, role); }
    private int count() { return jdbc.queryForObject("SELECT COUNT(*) FROM points_grant", Integer.class); }
    private long number(String sql) { return jdbc.queryForObject(sql, Long.class); }
    private LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Shanghai")); }
    private void insertEvent(long basis) {
        jdbc.update("INSERT INTO points_grant(user_id,source_type,event_key,basis_units,awarded_points,remaining_points,earned_on,expires_on) VALUES(1,'ORDER','order:14',?,18,18,?,?)", basis, today(), today().plusDays(365));
    }
}
