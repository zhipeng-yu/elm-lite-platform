package com.elmlite.platform.mapper;

import com.elmlite.platform.entity.CartItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cart_item_mapper_test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@Sql(
        scripts = {
                "/db/h2/cart-item-schema.sql",
                "/db/h2/cart-item-data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class CartItemMapperTest {

    @Autowired
    private CartItemMapper cartItemMapper;

    @Test
    void shouldMapCartItemFieldsWhenSelectingById() {
        CartItem cartItem = cartItemMapper.selectById(1L);

        assertNotNull(cartItem);
        assertEquals(1L, cartItem.getUserId());
        assertEquals(2L, cartItem.getProductId());
        assertEquals(1, cartItem.getQuantity());
    }

    @Test
    void shouldInsertCartItem() {
        CartItem cartItem = newCartItem(1L, 1L, 2);

        assertEquals(1, cartItemMapper.insert(cartItem));
        assertNotNull(cartItem.getId());
    }

    @Test
    void shouldRejectDuplicateUserAndProduct() {
        CartItem cartItem = newCartItem(1L, 2L, 3);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> cartItemMapper.insert(cartItem)
        );
    }

    @Test
    void shouldRejectCartItemWithUnknownUser() {
        CartItem cartItem = newCartItem(999L, 1L, 1);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> cartItemMapper.insert(cartItem)
        );
    }

    @Test
    void shouldRejectCartItemWithUnknownProduct() {
        CartItem cartItem = newCartItem(1L, 999L, 1);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> cartItemMapper.insert(cartItem)
        );
    }

    private CartItem newCartItem(Long userId, Long productId, Integer quantity) {
        CartItem cartItem = new CartItem();
        cartItem.setUserId(userId);
        cartItem.setProductId(productId);
        cartItem.setQuantity(quantity);
        return cartItem;
    }
}
