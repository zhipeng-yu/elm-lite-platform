package com.elmlite.platform.order;

import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.JwtTokenService.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:order_read;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql({"/db/h2/order-api-schema.sql", "/db/h2/order-api-data.sql"})
class OrderReadTest {
    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenService tokens;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void existingOrders() {
        for (int id = 1; id <= 3; id++) {
            jdbc.update("""
                    INSERT INTO orders(id,order_no,user_id,shop_id,address_id,receiver_name,receiver_phone,
                        delivery_address,product_amount,delivery_fee,total_amount,order_status,remark,created_at)
                    VALUES(?,?,?,1,1,'历史收货人','19900000001','历史地址',36.00,3.00,39.00,?,NULL,?)
                    """, id, "ORDER" + id, id == 3 ? 2 : 1, id - 1, "2026-09-06 10:00:00");
        }
        jdbc.update("""
                INSERT INTO order_item(order_id,product_id,product_name,unit_price,quantity,subtotal)
                VALUES(1,1,'历史商品',18.00,2,36.00)
                """);
    }

    @Test
    void listsOnlyOwnOrdersNewestFirstWithoutPrivateAddressFields() throws Exception {
        mvc.perform(get("/api/v1/orders").header("Authorization", user(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].id", contains(2, 1)))
                .andExpect(jsonPath("$.data[0].orderNo").value("ORDER2"))
                .andExpect(jsonPath("$.data[0].shopId").value(1))
                .andExpect(jsonPath("$.data[0].orderStatus").value(1))
                .andExpect(jsonPath("$.data[0].totalAmountCent").value(3900))
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-09-06T10:00:00+08:00"))
                .andExpect(jsonPath("$.data[0].userId").doesNotExist())
                .andExpect(jsonPath("$.data[0].receiverPhone").doesNotExist());
    }

    @Test
    void emptyListIsAnArray() throws Exception {
        jdbc.update("DELETE FROM orders WHERE user_id=2");
        mvc.perform(get("/api/v1/orders").header("Authorization", user(2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void detailReturnsStoredSnapshotsAfterSourceChangesAndAddressDeletion() throws Exception {
        jdbc.update("UPDATE product SET product_name='改名',price=99.00 WHERE id=1");
        jdbc.update("DELETE FROM delivery_address WHERE id=1");
        mvc.perform(get("/api/v1/orders/1").header("Authorization", user(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderStatus").value(0))
                .andExpect(jsonPath("$.data.receiverName").value("历史收货人"))
                .andExpect(jsonPath("$.data.receiverPhone").value("19900000001"))
                .andExpect(jsonPath("$.data.deliveryAddress").value("历史地址"))
                .andExpect(jsonPath("$.data.productAmountCent").value(3600))
                .andExpect(jsonPath("$.data.deliveryFeeCent").value(300))
                .andExpect(jsonPath("$.data.totalAmountCent").value(3900))
                .andExpect(jsonPath("$.data.items[0].productId").value(1))
                .andExpect(jsonPath("$.data.items[0].productName").value("历史商品"))
                .andExpect(jsonPath("$.data.items[0].unitPriceCent").value(1800))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].subtotalCent").value(3600))
                .andExpect(jsonPath("$.data.userId").doesNotExist());
    }

    @ParameterizedTest
    @CsvSource({"3,403", "999,404", "invalid,400"})
    void rejectsForeignMissingAndMalformedIds(String id, int expected) throws Exception {
        mvc.perform(get("/api/v1/orders/" + id).header("Authorization", user(1)))
                .andExpect(status().is(expected)).andExpect(jsonPath("$.code").value(expected));
    }

    @ParameterizedTest
    @CsvSource({"3,403", "999,404"})
    void checksActiveUserOnBothReads(long id, int expected) throws Exception {
        for (String path : new String[]{"/api/v1/orders", "/api/v1/orders/1"}) {
            mvc.perform(get(path).header("Authorization", user(id)))
                    .andExpect(status().is(expected)).andExpect(jsonPath("$.code").value(expected));
        }
    }

    private String user(long id) { return "Bearer " + tokens.issue(id, AccountType.USER); }
}
