package com.elmlite.platform.product;

import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.entity.Product;
import com.elmlite.platform.entity.ProductCategory;
import com.elmlite.platform.entity.ProductDetailImage;
import com.elmlite.platform.entity.Shop;
import com.elmlite.platform.mapper.MerchantMapper;
import com.elmlite.platform.mapper.ProductCategoryMapper;
import com.elmlite.platform.mapper.ProductDetailImageMapper;
import com.elmlite.platform.mapper.ProductMapper;
import com.elmlite.platform.mapper.ShopMapper;
import com.elmlite.platform.service.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:merchant_product_detail_image_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql(
        scripts = "/db/h2/merchant-product-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class MerchantProductDetailImageTest {

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
    private ProductDetailImageMapper productDetailImageMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    private Merchant owner;
    private Merchant other;
    private Shop ownerShop;
    private Shop otherShop;
    private ProductCategory ownerCategory;
    private ProductCategory otherCategory;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        owner = newMerchant("detail_owner");
        other = newMerchant("detail_other");

        ownerShop = newShop(owner.getId(), "Owner Shop");
        otherShop = newShop(other.getId(), "Other Shop");

        ownerCategory =
                newCategory(ownerShop.getId(), "Main Food");

        otherCategory =
                newCategory(otherShop.getId(), "Other Food");

        ownerToken =
                jwtTokenService.issue(
                        owner.getId(),
                        JwtTokenService.AccountType.MERCHANT);
    }

    @Test
    void merchantProductListReturnsDetailImagesInOrder()
            throws Exception {

        Product product =
                newProduct(
                        ownerShop.getId(),
                        ownerCategory.getId());

        newDetailImage(
                product.getId(),
                "https://example.com/detail-2.jpg",
                1);

        newDetailImage(
                product.getId(),
                "https://example.com/detail-1.jpg",
                0);

        mockMvc.perform(
                        get("/api/v1/merchant/shops/"
                                + ownerShop.getId()
                                + "/products")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath(
                        "$.data[0].detailImageUrls.length()")
                        .value(2))
                .andExpect(jsonPath(
                        "$.data[0].detailImageUrls[0]")
                        .value(
                                "https://example.com/detail-1.jpg"))
                .andExpect(jsonPath(
                        "$.data[0].detailImageUrls[1]")
                        .value(
                                "https://example.com/detail-2.jpg"));
    }

    @Test
    void merchantCanPatchDetailImages()
            throws Exception {

        Product product =
                newProduct(
                        ownerShop.getId(),
                        ownerCategory.getId());

        newDetailImage(
                product.getId(),
                "https://example.com/old.jpg",
                0);

        mockMvc.perform(
                        patch("/api/v1/merchant/products/"
                                + product.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "detailImageUrls": [
                                            "https://example.com/new-1.jpg",
                                            "/images/products/new-2.jpg"
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath(
                        "$.data.detailImageUrls.length()")
                        .value(2))
                .andExpect(jsonPath(
                        "$.data.detailImageUrls[0]")
                        .value(
                                "https://example.com/new-1.jpg"))
                .andExpect(jsonPath(
                        "$.data.detailImageUrls[1]")
                        .value(
                                "/images/products/new-2.jpg"));

        List<ProductDetailImage> saved =
                productDetailImageMapper.selectByProductId(
                        product.getId());

        assertEquals(2, saved.size());
        assertEquals(
                "https://example.com/new-1.jpg",
                saved.get(0).getImageUrl());
        assertEquals(
                "/images/products/new-2.jpg",
                saved.get(1).getImageUrl());
    }

    @Test
    void patchWithoutDetailImageUrlsKeepsExistingImages()
            throws Exception {

        Product product =
                newProduct(
                        ownerShop.getId(),
                        ownerCategory.getId());

        newDetailImage(
                product.getId(),
                "https://example.com/keep.jpg",
                0);

        mockMvc.perform(
                        patch("/api/v1/merchant/products/"
                                + product.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "productName": "Updated Product"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productName")
                        .value("Updated Product"))
                .andExpect(jsonPath(
                        "$.data.detailImageUrls.length()")
                        .value(1))
                .andExpect(jsonPath(
                        "$.data.detailImageUrls[0]")
                        .value(
                                "https://example.com/keep.jpg"));

        List<ProductDetailImage> saved =
                productDetailImageMapper.selectByProductId(
                        product.getId());

        assertEquals(1, saved.size());
        assertEquals(
                "https://example.com/keep.jpg",
                saved.get(0).getImageUrl());
    }

    @Test
    void patchWithEmptyDetailImageUrlsClearsImages()
            throws Exception {

        Product product =
                newProduct(
                        ownerShop.getId(),
                        ownerCategory.getId());

        newDetailImage(
                product.getId(),
                "https://example.com/old-1.jpg",
                0);

        newDetailImage(
                product.getId(),
                "https://example.com/old-2.jpg",
                1);

        mockMvc.perform(
                        patch("/api/v1/merchant/products/"
                                + product.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "detailImageUrls": []
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data.detailImageUrls.length()")
                        .value(0));

        assertEquals(
                0,
                productDetailImageMapper
                        .selectByProductId(product.getId())
                        .size());
    }

    @Test
    void patchRejectsMoreThanThreeDetailImages()
            throws Exception {

        Product product =
                newProduct(
                        ownerShop.getId(),
                        ownerCategory.getId());

        mockMvc.perform(
                        patch("/api/v1/merchant/products/"
                                + product.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "detailImageUrls": [
                                            "https://example.com/1.jpg",
                                            "https://example.com/2.jpg",
                                            "https://example.com/3.jpg",
                                            "https://example.com/4.jpg"
                                          ]
                                        }
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void merchantCannotPatchAnotherMerchantsDetailImages()
            throws Exception {

        Product product =
                newProduct(
                        otherShop.getId(),
                        otherCategory.getId());

        newDetailImage(
                product.getId(),
                "https://example.com/original.jpg",
                0);

        mockMvc.perform(
                        patch("/api/v1/merchant/products/"
                                + product.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "detailImageUrls": [
                                            "https://example.com/hacked.jpg"
                                          ]
                                        }
                                        """))
                .andExpect(status().isForbidden());

        List<ProductDetailImage> saved =
                productDetailImageMapper.selectByProductId(
                        product.getId());

        assertEquals(1, saved.size());
        assertEquals(
                "https://example.com/original.jpg",
                saved.get(0).getImageUrl());
    }

    @Test
    void createRejectsInvalidDetailImageUrl()
            throws Exception {

        Map<String, Object> body =
                Map.of(
                        "categoryId",
                        ownerCategory.getId(),
                        "productName",
                        "Invalid Image Product",
                        "description",
                        "Invalid detail image",
                        "imageUrl",
                        "https://example.com/cover.jpg",
                        "detailImageUrls",
                        List.of("ftp://example.com/detail.jpg"),
                        "priceCent",
                        1800L,
                        "stock",
                        10);

        mockMvc.perform(
                        post("/api/v1/merchant/shops/"
                                + ownerShop.getId()
                                + "/products")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken)
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        body)))
                .andExpect(status().isBadRequest());

        assertEquals(
                0L,
                productMapper.selectCount(null).longValue());
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

    private Shop newShop(
            Long merchantId,
            String shopName) {

        Shop shop = new Shop();
        shop.setMerchantId(merchantId);
        shop.setShopName(shopName);
        shop.setAddress("Test Address");
        shop.setStartPrice(new BigDecimal("0.00"));
        shop.setDeliveryPrice(new BigDecimal("0.00"));
        shop.setBusinessStatus(1);
        shopMapper.insert(shop);
        return shop;
    }

    private ProductCategory newCategory(
            Long shopId,
            String categoryName) {

        ProductCategory category =
                new ProductCategory();

        category.setShopId(shopId);
        category.setCategoryName(categoryName);
        category.setSortOrder(1);
        category.setStatus(1);

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
        product.setImageUrl(
                "https://example.com/cover.jpg");
        product.setPrice(new BigDecimal("18.00"));
        product.setStock(10);
        product.setStatus(1);

        productMapper.insert(product);
        return product;
    }

    private ProductDetailImage newDetailImage(
            Long productId,
            String imageUrl,
            int sortOrder) {

        ProductDetailImage detailImage =
                new ProductDetailImage();

        detailImage.setProductId(productId);
        detailImage.setImageUrl(imageUrl);
        detailImage.setSortOrder(sortOrder);

        productDetailImageMapper.insert(detailImage);
        return detailImage;
    }
}