package com.elmlite.platform.mapper;

import com.elmlite.platform.entity.OrderItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:order_item_mapper_test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@Sql(
        scripts = {
                "/db/h2/order-item-schema.sql",
                "/db/h2/order-item-data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class OrderItemMapperTest {

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Test
    void shouldMapOrderItemFieldsAndExactAmountsWhenSelectingById() {
        OrderItem orderItem = orderItemMapper.selectById(1L);

        assertNotNull(orderItem);
        assertEquals(1L, orderItem.getOrderId());
        assertEquals(1L, orderItem.getProductId());
        assertEquals("Beef Rice", orderItem.getProductName());
        assertEquals(0, orderItem.getUnitPrice().compareTo(new BigDecimal("18.00")));
        assertEquals(2, orderItem.getQuantity());
        assertEquals(0, orderItem.getSubtotal().compareTo(new BigDecimal("36.00")));
    }

    @Test
    void shouldInsertOrderItemWithExactAmounts() {
        OrderItem orderItem = newOrderItem(1L, 2L);

        assertEquals(1, orderItemMapper.insert(orderItem));
        assertNotNull(orderItem.getId());

        OrderItem saved = orderItemMapper.selectById(orderItem.getId());
        assertNotNull(saved);
        assertEquals(0, saved.getUnitPrice().compareTo(new BigDecimal("6.50")));
        assertEquals(0, saved.getSubtotal().compareTo(new BigDecimal("13.00")));
    }

    @Test
    void shouldRejectDuplicateProductInSameOrder() {
        OrderItem orderItem = newOrderItem(1L, 1L);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> orderItemMapper.insert(orderItem)
        );
    }

    @Test
    void shouldRejectOrderItemWithUnknownOrder() {
        OrderItem orderItem = newOrderItem(999L, 2L);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> orderItemMapper.insert(orderItem)
        );
    }

    @Test
    void shouldRejectOrderItemWithUnknownProduct() {
        OrderItem orderItem = newOrderItem(1L, 999L);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> orderItemMapper.insert(orderItem)
        );
    }

    private OrderItem newOrderItem(Long orderId, Long productId) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(orderId);
        orderItem.setProductId(productId);
        orderItem.setProductName("Lemon Water");
        orderItem.setUnitPrice(new BigDecimal("6.50"));
        orderItem.setQuantity(2);
        orderItem.setSubtotal(new BigDecimal("13.00"));
        return orderItem;
    }
}
