package com.elmlite.platform.coupon;

import com.elmlite.platform.entity.Coupon;
import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.entity.Shop;
import com.elmlite.platform.mapper.CouponMapper;
import com.elmlite.platform.mapper.MerchantMapper;
import com.elmlite.platform.mapper.ShopMapper;
import com.elmlite.platform.service.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:merchant_coupon_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql(
        scripts = "/db/h2/coupon-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class CouponClaimTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JdbcTemplate jdbc;

    private Shop shop;
    private String userToken;

    @BeforeEach
    void setUp() {
        jdbc.update("""
                INSERT INTO users (
                    id,
                    username,
                    phone,
                    password_hash,
                    nickname,
                    gender,
                    status
                )
                VALUES (
                    1,
                    'coupon_user',
                    '19900000001',
                    'unused_test_hash',
                    'Coupon User',
                    0,
                    1
                )
                """);

        Merchant merchant =
                newMerchant("claim_merchant");

        shop = newShop(
                merchant.getId(),
                "Claim Shop");

        userToken = jwtTokenService.issue(
                1L,
                JwtTokenService.AccountType.USER);
    }

    @Test
    void userCanClaimCouponOnce() throws Exception {
        Coupon coupon = newCoupon(
                1,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(1));

        mockMvc.perform(
                        post("/api/v1/coupons/"
                                + coupon.getId()
                                + "/claims")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0));

        assertEquals(
                Integer.valueOf(1),
                claimCount(coupon.getId()));

        assertEquals(
                Integer.valueOf(0),
                jdbc.queryForObject(
                        """
                        SELECT status
                        FROM user_coupon
                        WHERE user_id = ?
                          AND coupon_id = ?
                        """,
                        Integer.class,
                        1L,
                        coupon.getId()));
    }

    @ParameterizedTest
    @EnumSource(value = JwtTokenService.AccountType.class, names = {"MERCHANT", "RIDER", "ADMIN"})
    void otherRolesCannotClaimForUserWithSameId(JwtTokenService.AccountType type) throws Exception {
        Coupon coupon = newCoupon(1, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(1));
        mockMvc.perform(post("/api/v1/coupons/" + coupon.getId() + "/claims")
                        .header("Authorization", "Bearer " + jwtTokenService.issue(1L, type)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
        assertEquals(0, claimCount(coupon.getId()));
    }

    @Test
    void duplicateClaimReturnsConflict()
            throws Exception {

        Coupon coupon = newCoupon(
                1,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(1));

        mockMvc.perform(
                        post("/api/v1/coupons/"
                                + coupon.getId()
                                + "/claims")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/v1/coupons/"
                                + coupon.getId()
                                + "/claims")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        assertEquals(
                Integer.valueOf(1),
                claimCount(coupon.getId()));
    }

    @Test
    void disabledCouponCannotBeClaimed()
            throws Exception {

        Coupon coupon = newCoupon(
                0,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(1));

        mockMvc.perform(
                        post("/api/v1/coupons/"
                                + coupon.getId()
                                + "/claims")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        assertEquals(
                Integer.valueOf(0),
                claimCount(coupon.getId()));
    }

    @Test
    void notStartedCouponCannotBeClaimed()
            throws Exception {

        Coupon coupon = newCoupon(
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(1));

        mockMvc.perform(
                        post("/api/v1/coupons/"
                                + coupon.getId()
                                + "/claims")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        assertEquals(
                Integer.valueOf(0),
                claimCount(coupon.getId()));
    }

    @Test
    void expiredCouponCannotBeClaimed()
            throws Exception {

        Coupon coupon = newCoupon(
                1,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusHours(1));

        mockMvc.perform(
                        post("/api/v1/coupons/"
                                + coupon.getId()
                                + "/claims")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        assertEquals(
                Integer.valueOf(0),
                claimCount(coupon.getId()));
    }

    private Integer claimCount(Long couponId) {
        return jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM user_coupon
                WHERE user_id = ?
                  AND coupon_id = ?
                """,
                Integer.class,
                1L,
                couponId);
    }

    private Coupon newCoupon(
            int enabled,
            LocalDateTime startsAt,
            LocalDateTime expiresAt) {

        Coupon coupon = new Coupon();
        coupon.setShopId(shop.getId());
        coupon.setName("Claimable Coupon");
        coupon.setThresholdAmount(
                new BigDecimal("20.00"));
        coupon.setDiscountAmount(
                new BigDecimal("5.00"));
        coupon.setStartsAt(startsAt);
        coupon.setExpiresAt(expiresAt);
        coupon.setEnabled(enabled);

        couponMapper.insert(coupon);
        return coupon;
    }

    private Merchant newMerchant(String account) {
        Merchant merchant = new Merchant();
        merchant.setAccount(account);
        merchant.setPasswordHash(
                "unused_test_hash");
        merchant.setMerchantName(
                "Coupon Merchant");
        merchant.setContactName("Tester");
        merchant.setContactPhone(
                "19900000003");
        merchant.setStatus(1);

        merchantMapper.insert(merchant);
        return merchant;
    }

    private Shop newShop(
            Long merchantId,
            String name) {

        Shop shop = new Shop();
        shop.setMerchantId(merchantId);
        shop.setShopName(name);
        shop.setAddress("Test Address");
        shop.setStartPrice(
                new BigDecimal("0.00"));
        shop.setDeliveryPrice(
                new BigDecimal("0.00"));
        shop.setBusinessStatus(1);

        shopMapper.insert(shop);
        return shop;
    }
}
