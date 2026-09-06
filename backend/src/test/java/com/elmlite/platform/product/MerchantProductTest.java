package com.elmlite.platform.product;

import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.entity.Product;
import com.elmlite.platform.entity.ProductCategory;
import com.elmlite.platform.entity.Shop;
import com.elmlite.platform.mapper.MerchantMapper;
import com.elmlite.platform.mapper.ProductCategoryMapper;
import com.elmlite.platform.mapper.ProductMapper;
import com.elmlite.platform.mapper.ShopMapper;
import com.elmlite.platform.service.JwtTokenService;
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
        "spring.datasource.url=jdbc:h2:mem:merchant_product_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql(
        scripts = "/db/h2/merchant-product-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class MerchantProductTest {

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
    private ProductMapper productMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    private Merchant owner;
    private Merchant other;
    private Shop ownerShop;
    private Shop otherShop;
    private ProductCategory ownerCategory;
    private ProductCategory disabledCategory;
    private ProductCategory otherCategory;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        owner = newMerchant("product_owner");
        other = newMerchant("product_other");

        ownerShop = newShop(owner.getId(), "Owner Shop");
        otherShop = newShop(other.getId(), "Other Shop");

        ownerCategory = newCategory(
                ownerShop.getId(), "Main Food", 1);

        disabledCategory = newCategory(
                ownerShop.getId(), "Disabled", 0);

        otherCategory = newCategory(
                otherShop.getId(), "Other Food", 1);

        ownerToken = jwtTokenService.issue(
                owner.getId(),
                JwtTokenService.AccountType.MERCHANT);
    }

    @Test
    void merchantCanCreateProductWithExactMoneyAndDefaultEnabledStatus()
            throws Exception {

        String response = mockMvc.perform(
                        post("/api/v1/merchant/shops/"
                                + ownerShop.getId()
                                + "/products")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.categoryId")
                        .value(ownerCategory.getId()))
                .andExpect(jsonPath("$.data.productName")
                        .value("Beef Rice"))
                .andExpect(jsonPath("$.data.priceCent")
                        .value(1800))
                .andExpect(jsonPath("$.data.stock")
                        .value(100))
                .andExpect(jsonPath("$.data.status")
                        .value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(response)
                .path("data")
                .path("id")
                .asLong();

        Product saved = productMapper.selectById(id);

        assertNotNull(saved);
        assertEquals(ownerShop.getId(), saved.getShopId());
        assertEquals(ownerCategory.getId(), saved.getCategoryId());
        assertEquals("Beef Rice", saved.getProductName());
        assertEquals(
                0,
                new BigDecimal("18.00")
                        .compareTo(saved.getPrice()));
        assertEquals(Integer.valueOf(100), saved.getStock());
        assertEquals(Integer.valueOf(1), saved.getStatus());
    }

    @Test
    void merchantCanPatchOwnProduct() throws Exception {
        Product product = newProduct(
                ownerShop.getId(),
                ownerCategory.getId());

        mockMvc.perform(
                        patch("/api/v1/merchant/products/"
                                + product.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "categoryId": %d,
                                          "productName": "Updated Rice",
                                          "description": "Updated description",
                                          "imageUrl": "https://example.com/updated-rice.jpg",
                                          "priceCent": 2500,
                                          "stock": 8,
                                          "status": 0
                                        }
                                        """.formatted(ownerCategory.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.productName")
                        .value("Updated Rice"))
                .andExpect(jsonPath("$.data.priceCent")
                        .value(2500))
                .andExpect(jsonPath("$.data.stock")
                        .value(8))
                .andExpect(jsonPath("$.data.status")
                        .value(0));

        Product saved =
                productMapper.selectById(product.getId());

        assertEquals(ownerCategory.getId(), saved.getCategoryId());
        assertEquals("Updated Rice", saved.getProductName());
        assertEquals(
                "Updated description",
                saved.getDescription());
        assertEquals(
                "https://example.com/updated-rice.jpg",
                saved.getImageUrl());
        assertEquals(
                0,
                new BigDecimal("25.00")
                        .compareTo(saved.getPrice()));
        assertEquals(Integer.valueOf(8), saved.getStock());
        assertEquals(Integer.valueOf(0), saved.getStatus());
    }

    @ParameterizedTest
    @ValueSource(longs = {
            0L,
            10_000_000_000L
    })
    void rejectsOutOfRangePrice(long priceCent)
            throws Exception {

        Map<String, Object> body = validBody();
        body.put("priceCent", priceCent);

        mockMvc.perform(
                        post("/api/v1/merchant/shops/"
                                + ownerShop.getId()
                                + "/products")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        body)))
                .andExpect(status().isBadRequest());

        assertEquals(
                0L,
                productMapper.selectCount(null).longValue());
    }

    @Test
    void rejectsNegativeStock() throws Exception {
        Map<String, Object> body = validBody();
        body.put("stock", -1);

        mockMvc.perform(
                        post("/api/v1/merchant/shops/"
                                + ownerShop.getId()
                                + "/products")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        body)))
                .andExpect(status().isBadRequest());

        assertEquals(
                0L,
                productMapper.selectCount(null).longValue());
    }

    @Test
    void rejectsMissingCategory() throws Exception {
        Map<String, Object> body = validBody();
        body.put("categoryId", 999999L);

        mockMvc.perform(
                        post("/api/v1/merchant/shops/"
                                + ownerShop.getId()
                                + "/products")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        body)))
                .andExpect(status().isNotFound());

        assertEquals(
                0L,
                productMapper.selectCount(null).longValue());
    }

    @Test
    void rejectsCategoryFromAnotherMerchantsShop() throws Exception {
        Map<String, Object> body = validBody();
        body.put("categoryId", otherCategory.getId());

        mockMvc.perform(
                        post("/api/v1/merchant/shops/"
                                + ownerShop.getId()
                                + "/products")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        body)))
                .andExpect(status().isForbidden());

        assertEquals(
                0L,
                productMapper.selectCount(null).longValue());
    }

    @Test
    void rejectsDisabledCategory() throws Exception {
        Map<String, Object> body = validBody();
        body.put("categoryId", disabledCategory.getId());

        mockMvc.perform(
                        post("/api/v1/merchant/shops/"
                                + ownerShop.getId()
                                + "/products")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        body)))
                .andExpect(status().isBadRequest());

        assertEquals(
                0L,
                productMapper.selectCount(null).longValue());
    }

    @Test
    void merchantCannotCreateProductForAnotherMerchantsShop()
            throws Exception {

        Map<String, Object> body = validBody();
        body.put("categoryId", otherCategory.getId());

        mockMvc.perform(
                        post("/api/v1/merchant/shops/"
                                + otherShop.getId()
                                + "/products")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        body)))
                .andExpect(status().isForbidden());

        assertEquals(
                0L,
                productMapper.selectCount(null).longValue());
    }

    @Test
    void merchantCannotPatchAnotherMerchantsProduct()
            throws Exception {

        Product product = newProduct(
                otherShop.getId(),
                otherCategory.getId());

        mockMvc.perform(
                        patch("/api/v1/merchant/products/"
                                + product.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": 0
                                        }
                                        """))
                .andExpect(status().isForbidden());

        assertEquals(
                Integer.valueOf(1),
                productMapper
                        .selectById(product.getId())
                        .getStatus());
    }

    @Test
    void missingProductReturnsNotFound() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/merchant/products/999999")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": 0
                                        }
                                        """))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"categoryId", "priceCent", "stock"})
    void createRejectsFractionalIntegerFields(String field) throws Exception {
        Map<String, Object> body = validBody();
        body.put(field, switch (field) {
            case "categoryId" -> ownerCategory.getId() + 0.5;
            case "priceCent" -> 1800.9;
            default -> -0.5;
        });

        mockMvc.perform(post("/api/v1/merchant/shops/" + ownerShop.getId() + "/products")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        assertEquals(0L, productMapper.selectCount(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"categoryId", "priceCent", "stock", "status"})
    void patchRejectsFractionalIntegerFields(String field) throws Exception {
        Product product = newProduct(ownerShop.getId(), ownerCategory.getId());
        double value = switch (field) {
            case "categoryId" -> ownerCategory.getId() + 0.5;
            case "priceCent" -> 2500.9;
            default -> 0.5;
        };

        mockMvc.perform(patch("/api/v1/merchant/products/" + product.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(field, value))))
                .andExpect(status().isBadRequest());

        Product saved = productMapper.selectById(product.getId());
        assertEquals(ownerCategory.getId(), saved.getCategoryId());
        assertEquals(0, new BigDecimal("18.00").compareTo(saved.getPrice()));
        assertEquals(10, saved.getStock());
        assertEquals(1, saved.getStatus());
    }

    private Map<String, Object> validBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("categoryId", ownerCategory.getId());
        body.put("productName", "Beef Rice");
        body.put("description", "Fresh beef rice");
        body.put(
                "imageUrl",
                "https://example.com/beef-rice.jpg");
        body.put("priceCent", 1800L);
        body.put("stock", 100);
        return body;
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
            int status) {

        ProductCategory category = new ProductCategory();
        category.setShopId(shopId);
        category.setCategoryName(name);
        category.setSortOrder(1);
        category.setStatus(status);
        productCategoryMapper.insert(category);
        return category;
    }

    private Product newProduct(
            Long shopId,
            Long categoryId) {

        Product product = new Product();
        product.setShopId(shopId);
        product.setCategoryId(categoryId);
        product.setProductName("Original Product");
        product.setDescription("Original description");
        product.setPrice(new BigDecimal("18.00"));
        product.setStock(10);
        product.setStatus(1);
        productMapper.insert(product);
        return product;
    }
}
