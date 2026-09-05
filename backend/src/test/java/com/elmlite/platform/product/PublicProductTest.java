package com.elmlite.platform.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:public_product_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
@Sql({
        "/db/h2/public-product-schema.sql",
        "/db/h2/public-product-data.sql"
})
class PublicProductTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousCategoryListReturnsOnlyEnabledCategories() throws Exception {
        mockMvc.perform(get("/api/v1/shops/1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].categoryName").value("Main Food"))
                .andExpect(jsonPath("$.data[0].sortOrder").value(1))
                .andExpect(jsonPath("$.data[1].id").value(2));
    }

    @Test
    void anonymousProductListReturnsOnlyOnSaleProductsAndIntegerCents() throws Exception {
        mockMvc.perform(get("/api/v1/shops/1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].productName").value("Beef Rice"))
                .andExpect(jsonPath("$.data[0].priceCent").value(1800))
                .andExpect(jsonPath("$.data[0].stock").value(100))
                .andExpect(jsonPath("$.data[0].status").value(1))
                .andExpect(jsonPath("$.data[0].categoryName").doesNotExist())
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].priceCent").value(650))
                .andExpect(jsonPath("$.data[1].stock").value(0));
    }

    @Test
    void productListCanFilterByCategoryId() throws Exception {
        mockMvc.perform(get("/api/v1/shops/1/products")
                        .param("categoryId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].categoryId").value(2));
    }

    @Test
    void anonymousProductDetailReturnsCategoryNameAndExactCents() throws Exception {
        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.shopId").value(1))
                .andExpect(jsonPath("$.data.categoryId").value(1))
                .andExpect(jsonPath("$.data.categoryName").value("Main Food"))
                .andExpect(jsonPath("$.data.productName").value("Beef Rice"))
                .andExpect(jsonPath("$.data.priceCent").value(1800))
                .andExpect(jsonPath("$.data.stock").value(100))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    @Test
    void offShelfProductDetailReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/products/3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void missingProductReturnsUnifiedNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
