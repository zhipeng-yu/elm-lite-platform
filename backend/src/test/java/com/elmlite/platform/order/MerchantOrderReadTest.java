package com.elmlite.platform.order;

import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.JwtTokenService.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:merchant_order_read;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql({"/db/h2/order-api-schema.sql", "/db/h2/order-api-data.sql"})
class MerchantOrderReadTest {
    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenService tokens;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void existingOrders() {
        jdbc.update("""
                INSERT INTO merchant(id,account,password_hash,merchant_name,contact_name,contact_phone)
                VALUES(2,'other_merchant','unused_test_hash','其他商家','测试联系人','19900000002')
                """);
        jdbc.update("UPDATE shop SET merchant_id=2 WHERE id=2");
        jdbc.update("""
                INSERT INTO shop(id,merchant_id,shop_name,address,business_status)
                VALUES(3,1,'本商家空店铺','测试地址',0)
                """);
        // ID 1 比 ID 2、3 更新；ID 2、3 同时创建，用于分别验证两个排序条件。
        for (int id = 1; id <= 4; id++) {
            jdbc.update("""
                    INSERT INTO orders(id,order_no,user_id,shop_id,address_id,receiver_name,receiver_phone,
                        delivery_address,product_amount,delivery_fee,total_amount,order_status,remark,created_at)
                    VALUES(?,?,1,?,1,'历史收货人','19900000001','历史地址',36.00,3.00,39.00,?,'少辣',?)
                    """, id, "MERCHANT_ORDER" + id, id == 4 ? 2 : 1, id == 2 ? 1 : 0,
                    id == 1 ? "2026-09-10 11:00:00" : "2026-09-10 10:00:00");
            jdbc.update("""
                    INSERT INTO order_item(order_id,product_id,product_name,unit_price,quantity,subtotal)
                    VALUES(?,1,'历史商品',18.00,2,36.00)
                    """, id);
        }
    }

    @Test
    void listsOnlySelectedShopNewestFirstWithProductSummary() throws Exception {
        mvc.perform(get("/api/v1/merchant/shops/1/orders").header("Authorization", merchant(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].id", contains(1, 3, 2)))
                .andExpect(jsonPath("$.data[0].orderNo").value("MERCHANT_ORDER1"))
                .andExpect(jsonPath("$.data[0].shopId").value(1))
                .andExpect(jsonPath("$.data[0].orderStatus").value(0))
                .andExpect(jsonPath("$.data[0].receiverName").value("历史收货人"))
                .andExpect(jsonPath("$.data[0].totalAmountCent").value(3900))
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-09-10T11:00:00+08:00"))
                .andExpect(jsonPath("$.data[0].items[0].productName").value("历史商品"))
                .andExpect(jsonPath("$.data[0].items[0].quantity").value(2))
                .andExpect(jsonPath("$.data[0].receiverPhone").doesNotExist())
                .andExpect(jsonPath("$.data[0].deliveryAddress").doesNotExist());
    }

    @ParameterizedTest
    @CsvSource({"0,2", "1,1", "2,0", "3,0", "4,0", "5,0"})
    void filtersByValidStatus(int orderStatus, int count) throws Exception {
        mvc.perform(get("/api/v1/merchant/shops/1/orders").param("orderStatus", "" + orderStatus)
                        .header("Authorization", merchant(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(count));
        if (count > 0) {
            mvc.perform(get("/api/v1/merchant/shops/1/orders").param("orderStatus", "" + orderStatus)
                            .header("Authorization", merchant(1)))
                    .andExpect(jsonPath("$.data[0].orderStatus").value(orderStatus));
        }
    }

    @Test
    void ownedShopWithoutOrdersReturnsEmptyArray() throws Exception {
        mvc.perform(get("/api/v1/merchant/shops/3/orders").header("Authorization", merchant(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "6", "1.5", "invalid"})
    void rejectsInvalidStatus(String orderStatus) throws Exception {
        mvc.perform(get("/api/v1/merchant/shops/1/orders").param("orderStatus", orderStatus)
                        .header("Authorization", merchant(1)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void detailUsesStoredSnapshotsAndSeparatesBuyerFromReceiver() throws Exception {
        jdbc.update("UPDATE product SET product_name='新名称',price=99.00 WHERE id=1");
        jdbc.update("DELETE FROM delivery_address WHERE id=1");
        mvc.perform(get("/api/v1/merchant/orders/1").header("Authorization", merchant(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderNo").value("MERCHANT_ORDER1"))
                .andExpect(jsonPath("$.data.shopId").value(1))
                .andExpect(jsonPath("$.data.orderStatus").value(0))
                .andExpect(jsonPath("$.data.createdAt").value("2026-09-10T11:00:00+08:00"))
                .andExpect(jsonPath("$.data.buyer.id").value(1))
                .andExpect(jsonPath("$.data.buyer.displayName").value("订单用户"))
                .andExpect(jsonPath("$.data.buyer.username").doesNotExist())
                .andExpect(jsonPath("$.data.buyer.phone").doesNotExist())
                .andExpect(jsonPath("$.data.buyer.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.receiverName").value("历史收货人"))
                .andExpect(jsonPath("$.data.receiverPhone").value("19900000001"))
                .andExpect(jsonPath("$.data.deliveryAddress").value("历史地址"))
                .andExpect(jsonPath("$.data.remark").value("少辣"))
                .andExpect(jsonPath("$.data.productAmountCent").value(3600))
                .andExpect(jsonPath("$.data.deliveryFeeCent").value(300))
                .andExpect(jsonPath("$.data.totalAmountCent").value(3900))
                .andExpect(jsonPath("$.data.items[0].productId").value(1))
                .andExpect(jsonPath("$.data.items[0].productName").value("历史商品"))
                .andExpect(jsonPath("$.data.items[0].unitPriceCent").value(1800))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].subtotalCent").value(3600));
    }

    @ParameterizedTest
    @CsvSource({"/shops/2/orders,403", "/orders/4,403", "/shops/999/orders,404", "/orders/999,404",
            "/shops/invalid/orders,400", "/orders/invalid,400"})
    void rejectsForeignMissingAndMalformedResources(String suffix, int expected) throws Exception {
        mvc.perform(get("/api/v1/merchant" + suffix).header("Authorization", merchant(1)))
                .andExpect(status().is(expected)).andExpect(jsonPath("$.code").value(expected));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/shops/1/orders", "/orders/1"})
    void rejectsAnonymousAndUserIdentity(String suffix) throws Exception {
        mvc.perform(get("/api/v1/merchant" + suffix))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        mvc.perform(get("/api/v1/merchant" + suffix)
                        .header("Authorization", "Bearer " + tokens.issue(1, AccountType.USER)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/shops/1/orders", "/orders/1"})
    void rejectsDisabledOrMissingMerchantWithPreviouslyIssuedToken(String suffix) throws Exception {
        String authorization = merchant(1);
        jdbc.update("UPDATE merchant SET status=0 WHERE id=1");
        mvc.perform(get("/api/v1/merchant" + suffix).header("Authorization", authorization))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
        mvc.perform(get("/api/v1/merchant" + suffix).header("Authorization", merchant(999)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
    }

    private String merchant(long id) { return "Bearer " + tokens.issue(id, AccountType.MERCHANT); }
}
