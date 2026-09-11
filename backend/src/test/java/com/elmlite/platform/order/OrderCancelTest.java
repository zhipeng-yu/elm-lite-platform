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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:order_cancel;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql({"/db/h2/order-api-schema.sql", "/db/h2/order-api-data.sql"})
class OrderCancelTest {
    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenService tokens;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void existingOrders() {
        for (int id = 1; id <= 4; id++) {
            jdbc.update("""
                    INSERT INTO orders(id,order_no,user_id,shop_id,address_id,receiver_name,receiver_phone,
                        delivery_address,product_amount,delivery_fee,total_amount,order_status,created_at)
                    VALUES(?,?,?,1,1,'收货人','19900000001','历史地址',48.50,3.00,51.50,?,CURRENT_TIMESTAMP)
                    """, id, "CANCEL" + id, id == 4 ? 2 : 1, id - 1);
            jdbc.update("""
                    INSERT INTO order_item(order_id,product_id,product_name,unit_price,quantity,subtotal)
                    VALUES(?,1,'测试饭',18.00,2,36.00),(?,2,'测试面',12.50,1,12.50)
                    """, id, id);
        }
    }

    @Test
    void cancelsPendingOrderAndRestoresEveryProductIncludingOffShelfProduct() throws Exception {
        jdbc.update("UPDATE product SET status=0 WHERE id=1");

        mvc.perform(post("/api/v1/orders/1/cancel").header("Authorization", user(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(5))
                .andExpect(jsonPath("$.data.items.length()").value(2));

        assertEquals(12, stock(1));
        assertEquals(6, stock(2));
    }

    @Test
    void repeatedCancellationReturnsOrderWithoutRestoringTwice() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/api/v1/orders/1/cancel").header("Authorization", user(1)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(5));
        }
        assertEquals(12, stock(1));
        assertEquals(6, stock(2));
    }

    @ParameterizedTest
    @CsvSource({"2,409", "3,409", "4,403", "999,404"})
    void rejectsNonPendingForeignAndMissingOrders(long orderId, int expected) throws Exception {
        mvc.perform(post("/api/v1/orders/" + orderId + "/cancel").header("Authorization", user(1)))
                .andExpect(status().is(expected)).andExpect(jsonPath("$.code").value(expected));
    }

    @Test
    void stockOverflowRollsBackStatusAndEarlierRestorations() throws Exception {
        jdbc.update("UPDATE product SET stock=2147483647 WHERE id=2");

        mvc.perform(post("/api/v1/orders/1/cancel").header("Authorization", user(1)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(409));

        assertEquals(0, jdbc.queryForObject("SELECT order_status FROM orders WHERE id=1", Integer.class));
        assertEquals(10, stock(1));
        assertEquals(2147483647, stock(2));
    }

    @Test
    void cancellationAndConfirmationCannotBothWin() {
        var cancel = CompletableFuture.supplyAsync(() -> perform("/api/v1/orders/1/cancel", user(1)));
        var confirm = CompletableFuture.supplyAsync(() -> perform("/api/v1/merchant/orders/1/confirm", merchant(1)));
        List<Integer> statuses = List.of(cancel.join(), confirm.join()).stream().sorted().toList();

        assertEquals(List.of(200, 409), statuses);
        int finalStatus = jdbc.queryForObject("SELECT order_status FROM orders WHERE id=1", Integer.class);
        assertEquals(finalStatus == 5 ? 12 : 10, stock(1));
        assertEquals(finalStatus == 5 ? 6 : 5, stock(2));
    }

    private int perform(String path, String authorization) {
        try {
            return mvc.perform(post(path).header("Authorization", authorization)).andReturn().getResponse().getStatus();
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    private int stock(long productId) {
        return jdbc.queryForObject("SELECT stock FROM product WHERE id=?", Integer.class, productId);
    }

    private String user(long id) { return "Bearer " + tokens.issue(id, AccountType.USER); }
    private String merchant(long id) { return "Bearer " + tokens.issue(id, AccountType.MERCHANT); }
}
