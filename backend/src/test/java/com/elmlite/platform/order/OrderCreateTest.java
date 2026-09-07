package com.elmlite.platform.order;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.elmlite.platform.entity.CartItem;
import com.elmlite.platform.mapper.CartItemMapper;
import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.JwtTokenService.AccountType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:order_create;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql({"/db/h2/order-api-schema.sql", "/db/h2/order-api-data.sql"})
class OrderCreateTest {
    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenService tokens;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @MockitoSpyBean private CartItemMapper carts;

    @Test
    void createsSnapshotsWithServerPricesAndClearsOnlySelectedItems() throws Exception {
        jdbc.update("UPDATE product SET price=19.25 WHERE id=1");
        String result = create(1, "{\"addressId\":1,\"cartItemIds\":[1],\"remark\":\" 少辣 \"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(0))
                .andExpect(jsonPath("$.data.orderNo").isString())
                .andExpect(jsonPath("$.data.productAmountCent").value(3850))
                .andExpect(jsonPath("$.data.deliveryFeeCent").value(300))
                .andExpect(jsonPath("$.data.totalAmountCent").value(4150))
                .andExpect(jsonPath("$.data.receiverName").value("测试收货人"))
                .andExpect(jsonPath("$.data.receiverPhone").value("19900000001"))
                .andExpect(jsonPath("$.data.deliveryAddress").value("测试一号楼"))
                .andExpect(jsonPath("$.data.remark").value("少辣"))
                .andExpect(jsonPath("$.data.items[0].unitPriceCent").value(1925))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].subtotalCent").value(3850))
                .andExpect(jsonPath("$.data.userId").doesNotExist()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(result).path("data").path("id").asLong();
        assertEquals(8, count("SELECT stock FROM product WHERE id=1"));
        assertEquals(List.of(2L, 3L), jdbc.queryForList("SELECT id FROM cart_item ORDER BY id", Long.class));
        assertEquals(1, count("SELECT COUNT(*) FROM orders WHERE user_id=1"));
        assertEquals(1, count("SELECT COUNT(*) FROM order_item"));
        mvc.perform(get("/api/v1/orders/" + id).header("Authorization", user(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalAmountCent").value(4150));
    }

    @Test
    void multipleItemsUseExactCentArithmetic() throws Exception {
        create(1, "{\"addressId\":1,\"cartItemIds\":[2,1]}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.productAmountCent").value(4850))
                .andExpect(jsonPath("$.data.totalAmountCent").value(5150));
        assertEquals(8, count("SELECT stock FROM product WHERE id=1"));
        assertEquals(4, count("SELECT stock FROM product WHERE id=2"));
        assertEquals(1, count("SELECT COUNT(*) FROM cart_item"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "[]", "{", "",
            "{\"addressId\":1}", "{\"addressId\":null,\"cartItemIds\":[1]}",
            "{\"addressId\":0,\"cartItemIds\":[1]}", "{\"addressId\":1.5,\"cartItemIds\":[1]}",
            "{\"addressId\":\"1\",\"cartItemIds\":[1]}", "{\"addressId\":1,\"cartItemIds\":[]}",
            "{\"addressId\":1,\"cartItemIds\":[1,1]}", "{\"addressId\":1,\"cartItemIds\":[null]}",
            "{\"addressId\":1,\"cartItemIds\":[-1]}", "{\"addressId\":1,\"cartItemIds\":[1.5]}",
            "{\"addressId\":1,\"cartItemIds\":[\"1\"]}", "{\"addressId\":1,\"cartItemIds\":1}",
            "{\"addressId\":1,\"cartItemIds\":[9223372036854775808]}",
            "{\"addressId\":1,\"cartItemIds\":[1],\"userId\":2}",
            "{\"addressId\":1,\"cartItemIds\":[1],\"totalAmountCent\":1}",
            "{\"addressId\":1,\"cartItemIds\":[1],\"remark\":true}"})
    void invalidRequestsDoNotChangeData(String body) throws Exception {
        create(1, body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        unchanged();
    }

    @Test
    void remarkLengthIsValidatedAndBoundaryAccepted() throws Exception {
        create(1, json.writeValueAsString(Map.of("addressId", 1, "cartItemIds", List.of(1), "remark", "字".repeat(256))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.fieldErrors.remark").isString());
        unchanged();
        create(1, json.writeValueAsString(Map.of("addressId", 1, "cartItemIds", List.of(1), "remark", "字".repeat(255))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.remark").value("字".repeat(255)));
    }

    @ParameterizedTest
    @CsvSource({"1,2,1,403", "1,999,1,404", "1,1,3,403", "1,1,999,404", "3,1,1,403", "999,1,1,404"})
    void validatesCurrentUserAndOwnedResources(long userId, long addressId, long cartId, int expected) throws Exception {
        create(userId, json.writeValueAsString(Map.of("addressId", addressId, "cartItemIds", List.of(cartId))))
                .andExpect(status().is(expected)).andExpect(jsonPath("$.code").value(expected));
        unchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"UPDATE product SET status=0 WHERE id=1", "UPDATE product SET stock=1 WHERE id=1",
            "UPDATE shop SET business_status=0 WHERE id=1", "UPDATE shop SET business_status=2 WHERE id=1",
            "UPDATE shop SET start_price=36.01 WHERE id=1"})
    void rejectsUnavailableStockShopOrMinimumPrice(String setup) throws Exception {
        jdbc.update(setup);
        int stockBefore = count("SELECT stock FROM product WHERE id=1");
        create(1, "{\"addressId\":1,\"cartItemIds\":[1]}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(409));
        assertEquals(stockBefore, count("SELECT stock FROM product WHERE id=1"));
        assertEquals(0, count("SELECT COUNT(*) FROM orders"));
        assertEquals(3, count("SELECT COUNT(*) FROM cart_item"));
    }

    @Test
    void rejectsCrossShopSelection() throws Exception {
        jdbc.update("INSERT INTO cart_item(id,user_id,product_id,quantity) VALUES(4,1,3,1)");
        create(1, "{\"addressId\":1,\"cartItemIds\":[1,4]}").andExpect(status().isConflict());
        assertEquals(10, count("SELECT stock FROM product WHERE id=1"));
        assertEquals(10, count("SELECT stock FROM product WHERE id=3"));
        assertEquals(0, count("SELECT COUNT(*) FROM orders"));
        assertEquals(4, count("SELECT COUNT(*) FROM cart_item"));
    }

    @Test
    void laterStockFailureRollsBackEarlierDeduction() throws Exception {
        jdbc.update("UPDATE cart_item SET quantity=6 WHERE id=2");
        create(1, "{\"addressId\":1,\"cartItemIds\":[1,2]}").andExpect(status().isConflict());
        unchanged();
    }

    @Test
    void cartCleanupFailureRollsBackStockOrderAndItems() throws Exception {
        doThrow(new DataIntegrityViolationException("synthetic cleanup failure"))
                .when(carts).delete(org.mockito.ArgumentMatchers.<Wrapper<CartItem>>any());
        create(1, "{\"addressId\":1,\"cartItemIds\":[1]}").andExpect(status().isInternalServerError());
        verify(carts).delete(org.mockito.ArgumentMatchers.<Wrapper<CartItem>>any());
        unchanged();
    }

    @Test
    void acceptsExactMinimumAndStockAndNormalizesBlankRemark() throws Exception {
        jdbc.update("UPDATE product SET stock=2 WHERE id=1");
        jdbc.update("UPDATE shop SET start_price=36.00 WHERE id=1");
        create(1, "{\"addressId\":1,\"cartItemIds\":[1],\"remark\":\"  \"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.remark").value(org.hamcrest.Matchers.nullValue()));
        assertEquals(0, count("SELECT stock FROM product WHERE id=1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UPDATE product SET price=99999999.99 WHERE id=1",
            "UPDATE product SET price=49999999.99 WHERE id=1"})
    void rejectsSubtotalOrTotalBeyondDatabasePrecision(String setup) throws Exception {
        jdbc.update(setup);
        create(1, "{\"addressId\":1,\"cartItemIds\":[1]}").andExpect(status().isConflict());
        unchanged();
    }

    @Test
    void acceptsMaximumRepresentableTotal() throws Exception {
        jdbc.update("UPDATE product SET price=99999996.99 WHERE id=1");
        jdbc.update("UPDATE cart_item SET quantity=1 WHERE id=1");
        create(1, "{\"addressId\":1,\"cartItemIds\":[1],\"remark\":null}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.totalAmountCent").value(9999999999L));
    }

    @Test
    void concurrentUsersCannotOversellLastStock() throws Exception {
        jdbc.update("UPDATE product SET stock=2 WHERE id=1");
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> { start.await(); return create(1, "{\"addressId\":1,\"cartItemIds\":[1]}")
                    .andReturn().getResponse().getStatus(); });
            var second = pool.submit(() -> { start.await(); return create(2, "{\"addressId\":2,\"cartItemIds\":[3]}")
                    .andReturn().getResponse().getStatus(); });
            start.countDown();
            assertEquals(List.of(201, 409), java.util.stream.Stream.of(first.get(15, TimeUnit.SECONDS),
                    second.get(15, TimeUnit.SECONDS)).sorted().toList());
        }
        assertEquals(1, count("SELECT COUNT(*) FROM orders"));
        assertEquals(1, count("SELECT COUNT(*) FROM order_item"));
        assertEquals(2, count("SELECT COUNT(*) FROM cart_item"));
        assertEquals(2, count("SELECT stock FROM product WHERE id=1")
                + count("SELECT SUM(quantity) FROM order_item"));
    }

    @Test
    void concurrentDuplicateCheckoutCreatesOnlyOneOrder() throws Exception {
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var calls = java.util.stream.IntStream.range(0, 2).mapToObj(i -> pool.submit(() -> {
                start.await();
                return create(1, "{\"addressId\":1,\"cartItemIds\":[1]}").andReturn().getResponse().getStatus();
            })).toList();
            start.countDown();
            var codes = List.of(calls.get(0).get(15, TimeUnit.SECONDS), calls.get(1).get(15, TimeUnit.SECONDS));
            assertTrue(codes.contains(201)); assertTrue(codes.contains(404));
        }
        assertEquals(1, count("SELECT COUNT(*) FROM orders"));
        assertEquals(8, count("SELECT stock FROM product WHERE id=1"));
    }

    private ResultActions create(long id, String body) throws Exception {
        return mvc.perform(post("/api/v1/orders").header("Authorization", user(id))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String user(long id) { return "Bearer " + tokens.issue(id, AccountType.USER); }
    private int count(String sql) { return jdbc.queryForObject(sql, Integer.class); }
    private void unchanged() {
        assertEquals(0, count("SELECT COUNT(*) FROM orders"));
        assertEquals(0, count("SELECT COUNT(*) FROM order_item"));
        assertEquals(10, count("SELECT stock FROM product WHERE id=1"));
        assertEquals(5, count("SELECT stock FROM product WHERE id=2"));
        assertEquals(3, count("SELECT COUNT(*) FROM cart_item"));
    }
}
