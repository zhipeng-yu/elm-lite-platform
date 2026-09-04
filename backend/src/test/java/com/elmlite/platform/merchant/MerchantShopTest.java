package com.elmlite.platform.merchant;

import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.entity.Shop;
import com.elmlite.platform.mapper.MerchantMapper;
import com.elmlite.platform.mapper.ShopMapper;
import com.elmlite.platform.service.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:merchant_shop_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
@Sql(
        scripts = "/db/h2/shop-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class MerchantShopTest {

    private static final String URL = "/api/v1/merchant/shops";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    private Merchant owner;
    private Merchant other;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        owner = newMerchant("shop_owner");
        other = newMerchant("other_owner");
        ownerToken = jwtTokenService.issue(
                owner.getId(), JwtTokenService.AccountType.MERCHANT);
    }

    @Test
    void createsShopWithExactMoneyAndTokenOwner() throws Exception {
        Map<String, Object> body = validBody();
        // 即使客户端传其他商家 ID，也不能改变店铺归属。
        body.put("merchantId", other.getId());

        String response = mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.shopName").value("校园食堂"))
                .andExpect(jsonPath("$.data.startPriceCent").value(1234))
                .andExpect(jsonPath("$.data.deliveryPriceCent").value(101))
                .andExpect(jsonPath("$.data.businessStatus").value(0))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(response).path("data").path("id").asLong();
        Shop saved = shopMapper.selectById(id);

        assertNotNull(saved);
        assertEquals(owner.getId(), saved.getMerchantId());
        assertEquals("校园食堂", saved.getShopName());
        assertEquals("学校一号楼", saved.getAddress());
        assertEquals(0, new BigDecimal("12.34").compareTo(saved.getStartPrice()));
        assertEquals(0, new BigDecimal("1.01").compareTo(saved.getDeliveryPrice()));
        assertEquals(Integer.valueOf(0), saved.getBusinessStatus());
        assertEquals(1L, shopMapper.selectCount(null).longValue());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void ownerCanChangeBusinessStatus(int businessStatus) throws Exception {
        Shop shop = newShop(owner.getId());

        mockMvc.perform(patch(URL + "/" + shop.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("businessStatus", businessStatus))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.businessStatus").value(businessStatus));

        assertEquals(Integer.valueOf(businessStatus),
                shopMapper.selectById(shop.getId()).getBusinessStatus());
    }

    @Test
    void rejectsAnonymousCreation() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isUnauthorized());

        assertEquals(0L, shopMapper.selectCount(null).longValue());
    }

    @Test
    void rejectsUserTokenEvenWhenSubjectMatchesMerchantId() throws Exception {
        String userToken = jwtTokenService.issue(
                owner.getId(), JwtTokenService.AccountType.USER);

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isForbidden());

        assertEquals(0L, shopMapper.selectCount(null).longValue());
    }

    @Test
    void rejectsChangingAnotherMerchantsShop() throws Exception {
        Shop shop = newShop(other.getId());

        mockMvc.perform(patch(URL + "/" + shop.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessStatus\":1}"))
                .andExpect(status().isForbidden());

        assertEquals(Integer.valueOf(0),
                shopMapper.selectById(shop.getId()).getBusinessStatus());
    }

    @Test
    void rejectsDisabledMerchantWithPreviouslyIssuedToken() throws Exception {
        owner.setStatus(0);
        merchantMapper.updateById(owner);

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isForbidden());

        assertEquals(0L, shopMapper.selectCount(null).longValue());
    }

    @Test
    void returnsNotFoundForMissingShop() throws Exception {
        mockMvc.perform(patch(URL + "/999999")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessStatus\":1}"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 3})
    void rejectsInvalidBusinessStatus(int businessStatus) throws Exception {
        Shop shop = newShop(owner.getId());

        mockMvc.perform(patch(URL + "/" + shop.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("businessStatus", businessStatus))))
                .andExpect(status().isBadRequest());

        assertEquals(Integer.valueOf(0),
                shopMapper.selectById(shop.getId()).getBusinessStatus());
    }

    @ParameterizedTest
    @ValueSource(longs = {-1L, 10_000_000_000L})
    void rejectsOutOfRangeMoney(long amount) throws Exception {
        for (String field : new String[]{"startPriceCent", "deliveryPriceCent"}) {
            Map<String, Object> body = validBody();
            body.put(field, amount);

            mockMvc.perform(post(URL)
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.fieldErrors." + field).exists());
        }

        assertEquals(0L, shopMapper.selectCount(null).longValue());
    }

    private Map<String, Object> validBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("shopName", "校园食堂");
        body.put("description", "现做现卖");
        body.put("address", "学校一号楼");
        body.put("startPriceCent", 1234L);
        body.put("deliveryPriceCent", 101L);
        return body;
    }

    private Merchant newMerchant(String account) {
        Merchant merchant = new Merchant();
        merchant.setAccount(account);
        // 本测试直接签发 Token，不执行密码登录。
        merchant.setPasswordHash("unused_test_hash");
        merchant.setMerchantName("测试商家");
        merchant.setContactName("联系人");
        merchant.setContactPhone("19900000003");
        merchant.setStatus(1);
        merchantMapper.insert(merchant);
        return merchant;
    }

    private Shop newShop(Long merchantId) {
        Shop shop = new Shop();
        shop.setMerchantId(merchantId);
        shop.setShopName("已有店铺");
        shop.setAddress("学校二号楼");
        shop.setStartPrice(new BigDecimal("0.00"));
        shop.setDeliveryPrice(new BigDecimal("0.00"));
        shop.setBusinessStatus(0);
        shopMapper.insert(shop);
        return shop;
    }
}
