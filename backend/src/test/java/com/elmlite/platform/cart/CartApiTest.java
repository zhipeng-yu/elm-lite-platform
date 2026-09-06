package com.elmlite.platform.cart;

import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.JwtTokenService.AccountType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:cart_api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql({
        "/db/h2/cart-api-schema.sql",
        "/db/h2/cart-api-data.sql"
})
class CartApiTest {

    private static final String URL = "/api/v1/cart/items";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokenService tokens;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void listsOnlyCurrentUsersCartItems() throws Exception {
        mvc.perform(get(URL)
                        .header("Authorization", user(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].productId").value(1))
                .andExpect(jsonPath("$.data[0].shopId").value(10))
                .andExpect(jsonPath("$.data[0].productName").value("Beef Rice"))
                .andExpect(jsonPath("$.data[0].priceCent").value(1800))
                .andExpect(jsonPath("$.data[0].quantity").value(2))
                .andExpect(jsonPath("$.data[0].subtotalCent").value(3600))
                .andExpect(jsonPath("$.data[0].userId").doesNotExist());
    }

    @Test
    void returnsEmptyArrayForEmptyCart() throws Exception {
        jdbc.update("DELETE FROM cart_item WHERE user_id = 2");

        mvc.perform(get(URL)
                        .header("Authorization", user(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void addsAvailableProductToCurrentUsersCart() throws Exception {
        mvc.perform(post(URL)
                        .header("Authorization", user(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 2,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.productId").value(2))
                .andExpect(jsonPath("$.data.quantity").value(2))
                .andExpect(jsonPath("$.data.priceCent").value(650))
                .andExpect(jsonPath("$.data.subtotalCent").value(1300))
                .andExpect(jsonPath("$.data.userId").doesNotExist());

        assertEquals(
                Integer.valueOf(2),
                jdbc.queryForObject(
                        """
                        SELECT quantity
                        FROM cart_item
                        WHERE user_id = 1 AND product_id = 2
                        """,
                        Integer.class));
    }

    private String user(long id) {
        return "Bearer " + tokens.issue(id, AccountType.USER);
    }
}
