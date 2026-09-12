package com.elmlite.platform.coupon;

import com.elmlite.platform.service.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:my_coupon_query_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql(
        scripts = "/db/h2/coupon-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class MyCouponQueryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void loggedInUserWithoutCouponsGetsEmptyList() throws Exception {
        jdbc.update("""
                INSERT INTO users (
                    id, username, password_hash, nickname, gender, status
                )
                VALUES (
                    1, 'my_coupon_user', 'unused_test_hash',
                    'My Coupon User', 0, 1
                )
                """);

        String token = jwtTokenService.issue(
                1L,
                JwtTokenService.AccountType.USER);

        mockMvc.perform(
                        get("/api/v1/coupons/mine")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void userCanQueryOnlyOwnCoupons() throws Exception {
        insertQueryAccounts();

        jdbc.update("""
                INSERT INTO shop (
                    id, merchant_id, shop_name, address, business_status
                )
                VALUES (10, 1, 'Query Shop', 'Test Address', 1)
                """);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        jdbc.update("""
                INSERT INTO coupon (
                    id, shop_id, name, threshold_amount, discount_amount,
                    starts_at, expires_at, enabled
                )
                VALUES (20, 10, 'My Coupon', 20.00, 5.00, ?, ?, 1)
                """,
                now.minusHours(1),
                now.plusDays(1));

        jdbc.update("""
                INSERT INTO user_coupon (id, user_id, coupon_id, status)
                VALUES (101, 1, 20, 0), (102, 2, 20, 0)
                """);

        String token = jwtTokenService.issue(
                1L, JwtTokenService.AccountType.USER);

        mockMvc.perform(
                        get("/api/v1/coupons/mine")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].userCouponId").value(101))
                .andExpect(jsonPath("$.data[0].couponId").value(20))
                .andExpect(jsonPath("$.data[0].shopId").value(10))
                .andExpect(jsonPath("$.data[0].name").value("My Coupon"))
                .andExpect(jsonPath("$.data[0].thresholdCent").value(2000))
                .andExpect(jsonPath("$.data[0].discountCent").value(500))
                .andExpect(jsonPath("$.data[0].startsAt").isString())
                .andExpect(jsonPath("$.data[0].expiresAt").isString())
                .andExpect(jsonPath("$.data[0].enabled").value(true))
                .andExpect(jsonPath("$.data[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data[0].userId").doesNotExist());
    }

    @Test
    void merchantCannotQueryUserCouponsEvenWithSameAccountId()
            throws Exception {
        insertQueryAccounts();

        String token = jwtTokenService.issue(
                1L, JwtTokenService.AccountType.MERCHANT);

        mockMvc.perform(
                        get("/api/v1/coupons/mine")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    private void insertQueryAccounts() {
        jdbc.update("""
                INSERT INTO users (
                    id, username, password_hash, nickname, gender, status
                )
                VALUES
                    (1, 'mine_owner', 'unused_test_hash', 'Owner', 0, 1),
                    (2, 'mine_other', 'unused_test_hash', 'Other', 0, 1)
                """);

        jdbc.update("""
                INSERT INTO merchant (
                    id, account, password_hash, merchant_name,
                    contact_name, contact_phone, status
                )
                VALUES (
                    1, 'mine_merchant', 'unused_test_hash',
                    'Query Merchant', 'Tester', '19900000003', 1
                )
                """);
    }

}
