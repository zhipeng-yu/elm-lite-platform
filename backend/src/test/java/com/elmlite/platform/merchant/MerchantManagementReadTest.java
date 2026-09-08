package com.elmlite.platform.merchant;

import com.elmlite.platform.service.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:management_read;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql("/db/h2/merchant-product-schema.sql")
class MerchantManagementReadTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    String token;

    @BeforeEach void prepare() {
        jdbc.update("INSERT INTO merchant(id,account,password_hash,merchant_name,contact_name,contact_phone,status) VALUES (1,'owner','unused','Owner','Test','100',1),(2,'other','unused','Other','Test','100',1)");
        jdbc.update("INSERT INTO shop(id,merchant_id,shop_name,address,start_price,delivery_price,business_status) VALUES (1,1,'Own','Test',12.34,1.01,0),(2,2,'Other','Test',0,0,1)");
        jdbc.update("INSERT INTO product_category(id,shop_id,category_name,sort_order,status) VALUES (1,1,'Enabled',1,1),(2,1,'Disabled',0,0),(3,2,'Other',0,1)");
        jdbc.update("INSERT INTO product(id,shop_id,category_id,product_name,price,stock,status) VALUES (1,1,1,'Active',12.34,5,1),(2,1,2,'Hidden',2.01,0,0),(3,2,3,'Other',1,1,1)");
        token = tokens.issue(1L, JwtTokenService.AccountType.MERCHANT);
    }

    ResultActions read(String path) throws Exception {
        return mvc.perform(get("/api/v1/merchant/shops" + path).header("Authorization", "Bearer " + token));
    }

    @Test void listsOnlyOwnShopsWithResponseMoney() throws Exception {
        read("").andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].startPriceCent").value(1234))
                .andExpect(jsonPath("$.data[0].deliveryPriceCent").value(101))
                .andExpect(jsonPath("$.data[0].merchantId").doesNotExist());
    }

    @Test void includesDisabledCategoriesInDisplayOrder() throws Exception {
        read("/1/categories").andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(2)).andExpect(jsonPath("$.data[0].status").value(0));
    }

    @Test void includesOffShelfProductsWithExactPrices() throws Exception {
        read("/1/products").andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1)).andExpect(jsonPath("$.data[0].priceCent").value(1234))
                .andExpect(jsonPath("$.data[1].status").value(0));
    }

    @ParameterizedTest @ValueSource(strings = {"", "/1/categories", "/1/products"})
    void rejectsAnonymousUserDisabledAndMissingMerchant(String path) throws Exception {
        mvc.perform(get("/api/v1/merchant/shops" + path)).andExpect(status().isUnauthorized());
        token = tokens.issue(1L, JwtTokenService.AccountType.USER);
        read(path).andExpect(status().isForbidden());
        token = tokens.issue(1L, JwtTokenService.AccountType.MERCHANT);
        jdbc.update("UPDATE merchant SET status=0 WHERE id=1");
        read(path).andExpect(status().isForbidden());
        token = tokens.issue(999L, JwtTokenService.AccountType.MERCHANT);
        read(path).andExpect(status().isForbidden());
    }

    @ParameterizedTest @ValueSource(strings = {"categories", "products"})
    void rejectsOtherMissingAndInvalidShop(String resource) throws Exception {
        read("/2/" + resource).andExpect(status().isForbidden());
        read("/999/" + resource).andExpect(status().isNotFound());
        read("/invalid/" + resource).andExpect(status().isBadRequest());
    }

    @Test void returnsEmptyArrays() throws Exception {
        jdbc.update("DELETE FROM product WHERE shop_id=1");
        jdbc.update("DELETE FROM product_category WHERE shop_id=1");
        read("/1/products").andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        read("/1/categories").andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        jdbc.update("DELETE FROM shop WHERE id=1");
        read("").andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    }
}
