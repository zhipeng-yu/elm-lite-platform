package com.elmlite.platform.coupon;

import com.elmlite.platform.dto.CouponUsageResult;
import com.elmlite.platform.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:coupon_usage_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@Sql(
        scripts = "/db/h2/coupon-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class CouponUsageTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);

        jdbc.update("""
                INSERT INTO users (
                    id, username, password_hash, nickname, gender, status
                )
                VALUES (1, 'usage_user', 'unused_test_hash', 'Usage User', 0, 1)
                """);

        jdbc.update("""
                INSERT INTO merchant (
                    id, account, password_hash, merchant_name,
                    contact_name, contact_phone, status
                )
                VALUES (
                    1, 'usage_merchant', 'unused_test_hash',
                    'Usage Merchant', 'Tester', '19900000003', 1
                )
                """);

        jdbc.update("""
                INSERT INTO shop (
                    id, merchant_id, shop_name, address, business_status
                )
                VALUES (10, 1, 'Usage Shop', 'Test Address', 1)
                """);

        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO coupon (
                    id, shop_id, name, threshold_amount, discount_amount,
                    starts_at, expires_at, enabled
                )
                VALUES (20, 10, 'Usage Coupon', 20.00, 5.00, ?, ?, 1)
                """, now.minusDays(1), now.plusDays(1));

        jdbc.update("""
                INSERT INTO user_coupon (id, user_id, coupon_id, status)
                VALUES (100, 1, 20, 0)
                """);
    }

    @Test
    void consumeValidCouponReturnsDiscountAndMarksUsed() {
        CouponUsageResult result = transaction.execute(status ->
                couponService.consumeCoupon(1L, 100L, 10L, 2500L));

        assertNotNull(result);
        assertEquals(100L, result.userCouponId());
        assertEquals(20L, result.couponId());
        assertEquals(500L, result.discountAmountCent());
        assertEquals(Integer.valueOf(1), currentUsageStatus());
    }

    @Test
    void returnUsedCouponMarksUnused() {
        jdbc.update("UPDATE user_coupon SET status = 1 WHERE id = 100");

        transaction.executeWithoutResult(status ->
                couponService.returnCoupon(1L, 100L));

        assertEquals(Integer.valueOf(0), currentUsageStatus());
    }

    private Integer currentUsageStatus() {
        return jdbc.queryForObject(
                "SELECT status FROM user_coupon WHERE id = 100",
                Integer.class);
    }
}
