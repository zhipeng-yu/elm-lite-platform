package com.elmlite.platform.coupon;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

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
class MerchantCouponTest {

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

    private Shop ownerShop;
    private Shop otherShop;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        Merchant owner = newMerchant("coupon_owner");
        Merchant other = newMerchant("coupon_other");

        ownerShop = newShop(owner.getId(), "Coupon Shop");
        otherShop = newShop(other.getId(), "Other Coupon Shop");

        ownerToken = jwtTokenService.issue(
                owner.getId(),
                JwtTokenService.AccountType.MERCHANT);
    }

    @Test
    void merchantCanCreateCouponForOwnShop() throws Exception {
        mockMvc.perform(
                        post("/api/v1/merchant/shops/"
                                + ownerShop.getId()
                                + "/coupons")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.shopId")
                        .value(ownerShop.getId()))
                .andExpect(jsonPath("$.data.name")
                        .value("Lunch Discount"))
                .andExpect(jsonPath("$.data.thresholdCent")
                        .value(2000))
                .andExpect(jsonPath("$.data.discountCent")
                        .value(500))
                .andExpect(jsonPath("$.data.startsAt")
                        .value("2026-09-12T00:00:00"))
                .andExpect(jsonPath("$.data.expiresAt")
                        .value("2026-09-30T00:00:00"))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void merchantCannotCreateCouponForAnotherMerchantsShop()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/merchant/shops/"
                                + otherShop.getId()
                                + "/coupons")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validBody()))
                .andExpect(status().isForbidden());

        assertEquals(0L, couponMapper.selectCount(null));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(
                        post("/api/v1/merchant/shops/"
                                + ownerShop.getId()
                                + "/coupons")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "   ",
                                          "thresholdCent": 2000,
                                          "discountCent": 500,
                                          "startsAt": "2026-09-12T00:00:00",
                                          "expiresAt": "2026-09-30T00:00:00"
                                        }
                                        """))
                .andExpect(status().isBadRequest());

        assertEquals(0L, couponMapper.selectCount(null));
    }

    @Test
    void createRejectsNegativeThreshold() throws Exception {
        mockMvc.perform(
                        post("/api/v1/merchant/shops/"
                                + ownerShop.getId()
                                + "/coupons")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Lunch Discount",
                                          "thresholdCent": -1,
                                          "discountCent": 500,
                                          "startsAt": "2026-09-12T00:00:00",
                                          "expiresAt": "2026-09-30T00:00:00"
                                        }
                                        """))
                .andExpect(status().isBadRequest());

        assertEquals(0L, couponMapper.selectCount(null));
    }

    @Test
    void createRejectsNonPositiveDiscount() throws Exception {
        mockMvc.perform(
                        post("/api/v1/merchant/shops/"
                                + ownerShop.getId()
                                + "/coupons")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Lunch Discount",
                                          "thresholdCent": 2000,
                                          "discountCent": 0,
                                          "startsAt": "2026-09-12T00:00:00",
                                          "expiresAt": "2026-09-30T00:00:00"
                                        }
                                        """))
                .andExpect(status().isBadRequest());

        assertEquals(0L, couponMapper.selectCount(null));
    }

    @Test
    void createRejectsInvalidTimeRange() throws Exception {
        mockMvc.perform(
                        post("/api/v1/merchant/shops/"
                                + ownerShop.getId()
                                + "/coupons")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Lunch Discount",
                                          "thresholdCent": 2000,
                                          "discountCent": 500,
                                          "startsAt": "2026-09-30T00:00:00",
                                          "expiresAt": "2026-09-30T00:00:00"
                                        }
                                        """))
                .andExpect(status().isBadRequest());

        assertEquals(0L, couponMapper.selectCount(null));
    }

    private String validBody() {
        return """
                {
                  "name": "Lunch Discount",
                  "thresholdCent": 2000,
                  "discountCent": 500,
                  "startsAt": "2026-09-12T00:00:00",
                  "expiresAt": "2026-09-30T00:00:00"
                }
                """;
    }

    private Merchant newMerchant(String account) {
        Merchant merchant = new Merchant();
        merchant.setAccount(account);
        merchant.setPasswordHash("unused_test_hash");
        merchant.setMerchantName("Coupon Merchant");
        merchant.setContactName("Tester");
        merchant.setContactPhone("19900000003");
        merchant.setStatus(1);
        merchantMapper.insert(merchant);
        return merchant;
    }

    private Shop newShop(Long merchantId, String name) {
        Shop shop = new Shop();
        shop.setMerchantId(merchantId);
        shop.setShopName(name);
        shop.setAddress("Test Address");
        shop.setStartPrice(new BigDecimal("0.00"));
        shop.setDeliveryPrice(new BigDecimal("0.00"));
        shop.setBusinessStatus(1);
        shopMapper.insert(shop);
        return shop;
    }
}