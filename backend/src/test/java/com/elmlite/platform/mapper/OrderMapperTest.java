package com.elmlite.platform.mapper;

import com.elmlite.platform.entity.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:order_mapper_test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@Sql(
        scripts = {
                "/db/h2/order-schema.sql",
                "/db/h2/order-data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class OrderMapperTest {

    @Autowired
    private OrderMapper orderMapper;

    @Test
    void shouldMapOrderFieldsAndExactAmountsWhenSelectingById() {
        Order order = orderMapper.selectById(1L);

        assertNotNull(order);
        assertEquals("TEST202609020001", order.getOrderNo());
        assertEquals(1L, order.getUserId());
        assertEquals(1L, order.getShopId());
        assertEquals(1L, order.getAddressId());
        assertEquals("Test User", order.getReceiverName());
        assertEquals("19900000001", order.getReceiverPhone());
        assertEquals("Test Campus Dormitory 1", order.getDeliveryAddress());
        assertEquals(0, order.getProductAmount().compareTo(new BigDecimal("36.00")));
        assertEquals(0, order.getDeliveryFee().compareTo(new BigDecimal("3.00")));
        assertEquals(0, order.getTotalAmount().compareTo(new BigDecimal("39.00")));
        assertEquals(0, order.getOrderStatus());
        assertEquals("Initial test order", order.getRemark());
    }

    @Test
    void shouldInsertOrderWithoutAddressId() {
        Order order = newOrder("TEST202609030002");
        order.setAddressId(null);

        assertEquals(1, orderMapper.insert(order));
        assertNotNull(order.getId());

        Order saved = orderMapper.selectById(order.getId());
        assertNotNull(saved);
        assertNull(saved.getAddressId());
        assertEquals(0, saved.getTotalAmount().compareTo(new BigDecimal("23.50")));
    }

    @Test
    void shouldRejectDuplicateOrderNumber() {
        Order order = newOrder("TEST202609020001");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> orderMapper.insert(order)
        );
    }

    @Test
    void shouldRejectOrderWithUnknownUser() {
        Order order = newOrder("TEST202609030003");
        order.setUserId(999L);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> orderMapper.insert(order)
        );
    }

    @Test
    void shouldRejectOrderWithUnknownShop() {
        Order order = newOrder("TEST202609030004");
        order.setShopId(999L);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> orderMapper.insert(order)
        );
    }

    @Test
    void shouldRejectOrderWithUnknownAddress() {
        Order order = newOrder("TEST202609030005");
        order.setAddressId(999L);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> orderMapper.insert(order)
        );
    }

    private Order newOrder(String orderNo) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(1L);
        order.setShopId(1L);
        order.setAddressId(1L);
        order.setReceiverName("New Receiver");
        order.setReceiverPhone("19900000003");
        order.setDeliveryAddress("Test Address 2");
        order.setProductAmount(new BigDecimal("20.50"));
        order.setDeliveryFee(new BigDecimal("3.00"));
        order.setTotalAmount(new BigDecimal("23.50"));
        order.setOrderStatus(0);
        order.setRemark("Order mapper test data");
        return order;
    }
}
