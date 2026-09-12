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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:merchant_order_action;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql({"/db/h2/order-api-schema.sql", "/db/h2/order-api-data.sql"})
class MerchantOrderActionTest {
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
        for (int id = 1; id <= 4; id++) {
            jdbc.update("""
                    INSERT INTO orders(id,order_no,user_id,shop_id,address_id,receiver_name,receiver_phone,
                        delivery_address,product_amount,delivery_fee,total_amount,order_status,remark,created_at)
                    VALUES(?,?,1,?,1,'历史收货人','19900000001','历史地址',36.00,3.00,39.00,?,'少辣',CURRENT_TIMESTAMP)
                    """, id, "ACTION" + id, id == 4 ? 2 : 1, id - 1);
            jdbc.update("""
                    INSERT INTO order_item(order_id,product_id,product_name,unit_price,quantity,subtotal)
                    VALUES(?,1,'历史商品',18.00,2,36.00)
                    """, id);
        }
    }

    @Test
    void confirmsThenStartsPreparingAndReturnsUpdatedDetail() throws Exception {
        mvc.perform(post("/api/v1/merchant/orders/1/confirm").header("Authorization", merchant(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(1))
                .andExpect(jsonPath("$.data.receiverPhone").value("19900000001"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));

        mvc.perform(post("/api/v1/merchant/orders/1/prepare").header("Authorization", merchant(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(2));
    }

    @ParameterizedTest
    @CsvSource({"2,confirm", "1,prepare", "3,prepare"})
    void rejectsIllegalTransitions(long orderId, String action) throws Exception {
        mvc.perform(post("/api/v1/merchant/orders/" + orderId + "/" + action)
                        .header("Authorization", merchant(1)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(409));
    }

    @ParameterizedTest
    @CsvSource({"4,confirm,403", "999,confirm,404"})
    void rejectsForeignAndMissingOrders(long orderId, String action, int expected) throws Exception {
        mvc.perform(post("/api/v1/merchant/orders/" + orderId + "/" + action)
                        .header("Authorization", merchant(1)))
                .andExpect(status().is(expected)).andExpect(jsonPath("$.code").value(expected));
    }

    @Test
    void rejectsUserIdentity() throws Exception {
        mvc.perform(post("/api/v1/merchant/orders/1/confirm")
                        .header("Authorization", "Bearer " + tokens.issue(1, AccountType.USER)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
    }

    private String merchant(long id) { return "Bearer " + tokens.issue(id, AccountType.MERCHANT); }
}
