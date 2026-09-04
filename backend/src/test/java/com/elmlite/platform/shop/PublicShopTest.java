package com.elmlite.platform.shop;

import com.elmlite.platform.mapper.ShopMapper;
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
        "spring.datasource.url=jdbc:h2:mem:public_shop_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
@Sql({"/db/h2/shop-schema.sql", "/db/h2/shop-data.sql"})
class PublicShopTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShopMapper shopMapper;

    @Test
    void anonymousListUsesContractFieldsAndIntegerCents() throws Exception {
        mockMvc.perform(get("/api/v1/shops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].shopName").value("Campus Food Shop"))
                .andExpect(jsonPath("$.data[0].startPriceCent").value(1500))
                .andExpect(jsonPath("$.data[0].deliveryPriceCent").value(300))
                .andExpect(jsonPath("$.data[0].businessStatus").value(1))
                .andExpect(jsonPath("$.data[0].address").doesNotExist())
                .andExpect(jsonPath("$.data[0].merchantId").doesNotExist());
    }

    @Test
    void emptyListReturnsArray() throws Exception {
        shopMapper.deleteById(1L);
        mockMvc.perform(get("/api/v1/shops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void anonymousDetailIncludesAddressAndExactCents() throws Exception {
        var shop = shopMapper.selectById(1L);
        shop.setStartPrice(new java.math.BigDecimal("12.34"));
        shop.setBusinessStatus(2);
        shopMapper.updateById(shop);
        mockMvc.perform(get("/api/v1/shops/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.address").value("Test Address 1"))
                .andExpect(jsonPath("$.data.startPriceCent").value(1234))
                .andExpect(jsonPath("$.data.businessStatus").value(2))
                .andExpect(jsonPath("$.data.merchantId").doesNotExist());
    }

    @Test
    void missingShopReturnsUnifiedNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/shops/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void malformedShopIdReturnsUnifiedBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/shops/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
