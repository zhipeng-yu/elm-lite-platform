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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @Test
    void repeatedAddAccumulatesQuantityWithoutDuplicateRow() throws Exception {
        mvc.perform(post(URL)
                        .header("Authorization", user(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 1,
                                  "quantity": 3
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.quantity").value(5));

        assertEquals(
                Integer.valueOf(5),
                jdbc.queryForObject(
                        """
                        SELECT quantity
                        FROM cart_item
                        WHERE user_id = 1 AND product_id = 1
                        """,
                        Integer.class));

        assertEquals(
                Integer.valueOf(1),
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM cart_item
                        WHERE user_id = 1 AND product_id = 1
                        """,
                        Integer.class));
    }

    @Test
    void rejectsNonPositiveQuantity() throws Exception {
        for (int quantity : new int[]{0, -1}) {
            mvc.perform(post(URL)
                            .header("Authorization", user(1))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "productId": 2,
                                      "quantity": %d
                                    }
                                    """.formatted(quantity)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    @Test
    void rejectsOffShelfProduct() throws Exception {
        jdbc.update("UPDATE product SET status = 0 WHERE id = 2");

        mvc.perform(post(URL)
                        .header("Authorization", user(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 2,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void rejectsQuantityGreaterThanStock() throws Exception {
        mvc.perform(post(URL)
                        .header("Authorization", user(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 2,
                                  "quantity": 6
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void rejectsFractionalQuantity() throws Exception {
        mvc.perform(post(URL)
                        .header("Authorization", user(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 2,
                                  "quantity": 1.5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void updatesOwnedCartItemQuantity() throws Exception {
        mvc.perform(patch(URL + "/1")
                        .header("Authorization", user(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 4
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.productId").value(1))
                .andExpect(jsonPath("$.data.quantity").value(4))
                .andExpect(jsonPath("$.data.subtotalCent").value(7200))
                .andExpect(jsonPath("$.data.userId").doesNotExist());

        assertEquals(
                Integer.valueOf(4),
                jdbc.queryForObject(
                        "SELECT quantity FROM cart_item WHERE id = 1",
                        Integer.class));
    }

    @Test
    void rejectsZeroQuantityOnUpdate() throws Exception {
        mvc.perform(patch(URL + "/1")
                        .header("Authorization", user(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        assertEquals(
                Integer.valueOf(2),
                jdbc.queryForObject(
                        "SELECT quantity FROM cart_item WHERE id = 1",
                        Integer.class));
    }

    @Test
    void rejectsUpdateBeyondStock() throws Exception {
        mvc.perform(patch(URL + "/1")
                        .header("Authorization", user(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 11
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        assertEquals(
                Integer.valueOf(2),
                jdbc.queryForObject(
                        "SELECT quantity FROM cart_item WHERE id = 1",
                        Integer.class));
    }

    @Test
    void forbidsUpdatingAnotherUsersCartItem() throws Exception {
        mvc.perform(patch(URL + "/2")
                        .header("Authorization", user(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void deletesOwnedCartItem() throws Exception {
        mvc.perform(delete(URL + "/1")
                        .header("Authorization", user(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        assertEquals(
                Integer.valueOf(0),
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM cart_item WHERE id = 1",
                        Integer.class));
    }

    @Test
    void rejectsAddingProductFromDifferentShop() throws Exception {
        jdbc.update("""
                INSERT INTO product (
                    id,
                    shop_id,
                    category_id,
                    product_name,
                    description,
                    image_url,
                    price,
                    stock,
                    status
                )
                VALUES (
                    3,
                    20,
                    2,
                    'Noodles',
                    'Different shop product',
                    'noodles.png',
                    15.00,
                    8,
                    1
                )
                """);

        mvc.perform(post(URL)
                        .header("Authorization", user(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 3,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        assertEquals(
                Integer.valueOf(0),
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM cart_item
                        WHERE user_id = 1 AND product_id = 3
                        """,
                        Integer.class));

        assertEquals(
                Integer.valueOf(1),
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM cart_item
                        WHERE user_id = 1
                        """,
                        Integer.class));
    }

    private String user(long id) {
        return "Bearer " + tokens.issue(id, AccountType.USER);
    }
}
