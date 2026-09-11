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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class CouponQueryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    private Shop shop;
    private String merchantToken;

    @BeforeEach
    void setUp() {
        Merchant merchant =
                newMerchant("coupon_query_merchant");

        shop = newShop(
                merchant.getId(),
                "Coupon Query Shop");

        merchantToken =
                jwtTokenService.issue(
                        merchant.getId(),
                        JwtTokenService.AccountType.MERCHANT);
    }

    @Test
    void merchantCanListAllCouponsIncludingDisabled()
            throws Exception {

        newCoupon(
                "Claimable Coupon",
                1,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(1));

        newCoupon(
                "Disabled Coupon",
                0,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(1));

        mockMvc.perform(
                        get("/api/v1/merchant/shops/"
                                + shop.getId()
                                + "/coupons")
                                .header(
                                        "Authorization",
                                        "Bearer " + merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].name")
                        .value(hasItems(
                                "Claimable Coupon",
                                "Disabled Coupon")))
                .andExpect(jsonPath("$.data[*].enabled")
                        .value(hasItems(
                                true,
                                false)));
    }

    @Test
    void publicListReturnsOnlyCurrentlyClaimableCoupons()
            throws Exception {

        newCoupon(
                "Claimable Coupon",
                1,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(1));

        newCoupon(
                "Disabled Coupon",
                0,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(1));

        newCoupon(
                "Future Coupon",
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(1));

        newCoupon(
                "Expired Coupon",
                1,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusHours(1));

        mockMvc.perform(
                        get("/api/v1/shops/"
                                + shop.getId()
                                + "/coupons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").isNumber())
                .andExpect(jsonPath("$.data[0].shopId")
                        .value(shop.getId()))
                .andExpect(jsonPath("$.data[0].name")
                        .value("Claimable Coupon"))
                .andExpect(jsonPath("$.data[0].thresholdCent")
                        .value(2000))
                .andExpect(jsonPath("$.data[0].discountCent")
                        .value(500))
                .andExpect(jsonPath("$.data[0].startsAt").exists())
                .andExpect(jsonPath("$.data[0].expiresAt").exists())
                .andExpect(jsonPath("$.data[0].enabled")
                        .value(true));
    }

    private Coupon newCoupon(
            String name,
            int enabled,
            LocalDateTime startsAt,
            LocalDateTime expiresAt) {

        Coupon coupon = new Coupon();
        coupon.setShopId(shop.getId());
        coupon.setName(name);
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
                "Coupon Query Merchant");
        merchant.setContactName("Tester");
        merchant.setContactPhone(
                "19900000004");
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