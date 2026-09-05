package com.elmlite.platform.product;

import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.entity.ProductCategory;
import com.elmlite.platform.entity.Shop;
import com.elmlite.platform.mapper.MerchantMapper;
import com.elmlite.platform.mapper.ProductCategoryMapper;
import com.elmlite.platform.mapper.ShopMapper;
import com.elmlite.platform.service.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:merchant_category_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
@Sql(
        scripts = "/db/h2/merchant-category-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class MerchantCategoryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private ProductCategoryMapper productCategoryMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    private Merchant owner;
    private Merchant other;
    private Shop ownerShop;
    private Shop otherShop;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        owner = newMerchant("category_owner");
        other = newMerchant("category_other");

        ownerShop = newShop(owner.getId(), "Owner Shop");
        otherShop = newShop(other.getId(), "Other Shop");

        ownerToken = jwtTokenService.issue(
                owner.getId(),
                JwtTokenService.AccountType.MERCHANT);
    }

    @Test
    void merchantCanCreateCategoryWithDefaultEnabledStatus() throws Exception {
        String response = mockMvc.perform(
                        post("/api/v1/merchant/shops/" + ownerShop.getId() + "/categories")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "categoryName": "Main Food",
                                          "sortOrder": 1
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.categoryName").value("Main Food"))
                .andExpect(jsonPath("$.data.sortOrder").value(1))
                .andExpect(jsonPath("$.data.status").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(response)
                .path("data")
                .path("id")
                .asLong();

        ProductCategory saved = productCategoryMapper.selectById(id);

        assertNotNull(saved);
        assertEquals(ownerShop.getId(), saved.getShopId());
        assertEquals("Main Food", saved.getCategoryName());
        assertEquals(Integer.valueOf(1), saved.getSortOrder());
        assertEquals(Integer.valueOf(1), saved.getStatus());
    }

    @Test
    void merchantCanPatchOwnCategory() throws Exception {
        ProductCategory category =
                newCategory(ownerShop.getId(), "Old Name", 1, 1);

        mockMvc.perform(
                        patch("/api/v1/merchant/categories/" + category.getId())
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "categoryName": "New Name",
                                          "sortOrder": 5,
                                          "status": 0
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.categoryName").value("New Name"))
                .andExpect(jsonPath("$.data.sortOrder").value(5))
                .andExpect(jsonPath("$.data.status").value(0));

        ProductCategory saved =
                productCategoryMapper.selectById(category.getId());

        assertEquals("New Name", saved.getCategoryName());
        assertEquals(Integer.valueOf(5), saved.getSortOrder());
        assertEquals(Integer.valueOf(0), saved.getStatus());
    }

    @Test
    void duplicateCategoryNameInSameShopReturnsConflict() throws Exception {
        newCategory(ownerShop.getId(), "Drinks", 1, 1);

        mockMvc.perform(
                        post("/api/v1/merchant/shops/" + ownerShop.getId() + "/categories")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "categoryName": "Drinks",
                                          "sortOrder": 2
                                        }
                                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        assertEquals(
                1L,
                productCategoryMapper.selectCount(null).longValue());
    }

    @Test
    void merchantCannotCreateCategoryForAnotherMerchantsShop() throws Exception {
        mockMvc.perform(
                        post("/api/v1/merchant/shops/" + otherShop.getId() + "/categories")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "categoryName": "Forbidden",
                                          "sortOrder": 1
                                        }
                                        """))
                .andExpect(status().isForbidden());

        assertEquals(
                0L,
                productCategoryMapper.selectCount(null).longValue());
    }

    @Test
    void merchantCannotPatchAnotherMerchantsCategory() throws Exception {
        ProductCategory category =
                newCategory(otherShop.getId(), "Other Category", 1, 1);

        mockMvc.perform(
                        patch("/api/v1/merchant/categories/" + category.getId())
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": 0
                                        }
                                        """))
                .andExpect(status().isForbidden());

        assertEquals(
                Integer.valueOf(1),
                productCategoryMapper
                        .selectById(category.getId())
                        .getStatus());
    }

    @Test
    void missingCategoryReturnsNotFound() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/merchant/categories/999999")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": 0
                                        }
                                        """))
                .andExpect(status().isNotFound());
    }

    private Merchant newMerchant(String account) {
        Merchant merchant = new Merchant();
        merchant.setAccount(account);
        merchant.setPasswordHash("unused_test_hash");
        merchant.setMerchantName("Test Merchant");
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

    private ProductCategory newCategory(
            Long shopId,
            String name,
            int sortOrder,
            int status) {

        ProductCategory category = new ProductCategory();
        category.setShopId(shopId);
        category.setCategoryName(name);
        category.setSortOrder(sortOrder);
        category.setStatus(status);
        productCategoryMapper.insert(category);
        return category;
    }
}
