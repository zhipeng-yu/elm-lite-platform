package com.elmlite.platform.cart;

import com.elmlite.platform.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:cart_order_support;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@Sql({
        "/db/h2/cart-api-schema.sql",
        "/db/h2/cart-api-data.sql"
})
class CartOrderSupportTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void clearsOnlyPurchasedCartItemsForCurrentUser() {
        jdbc.update("""
                INSERT INTO cart_item (
                    id,
                    user_id,
                    product_id,
                    quantity
                )
                VALUES (
                    3,
                    1,
                    2,
                    1
                )
                """);

        cartService.clearPurchasedItems(
                1L,
                List.of(1L));

        assertEquals(
                Integer.valueOf(0),
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM cart_item
                        WHERE user_id = 1
                          AND product_id = 1
                        """,
                        Integer.class));

        assertEquals(
                Integer.valueOf(1),
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM cart_item
                        WHERE user_id = 1
                          AND product_id = 2
                        """,
                        Integer.class));

        assertEquals(
                Integer.valueOf(1),
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM cart_item
                        WHERE user_id = 2
                          AND product_id = 2
                        """,
                        Integer.class));
    }

    @Test
    void clearingMissingProductIdsDoesNotDeleteOtherItems() {
        cartService.clearPurchasedItems(
                1L,
                List.of(999L));

        assertEquals(
                Integer.valueOf(1),
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM cart_item
                        WHERE user_id = 1
                        """,
                        Integer.class));

        assertEquals(
                Integer.valueOf(1),
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM cart_item
                        WHERE user_id = 2
                        """,
                        Integer.class));
    }

    @Test
    void clearingEmptyProductListDoesNothing() {
        cartService.clearPurchasedItems(
                1L,
                List.of());

        assertEquals(
                Integer.valueOf(1),
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM cart_item
                        WHERE user_id = 1
                        """,
                        Integer.class));

        assertEquals(
                Integer.valueOf(1),
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM cart_item
                        WHERE user_id = 2
                        """,
                        Integer.class));
    }
}
