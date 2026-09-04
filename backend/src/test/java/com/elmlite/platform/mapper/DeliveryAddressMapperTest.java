package com.elmlite.platform.mapper;

import com.elmlite.platform.entity.DeliveryAddress;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:delivery_address_mapper_test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@Sql(
        scripts = {
                "/db/h2/delivery-address-schema.sql",
                "/db/h2/delivery-address-data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class DeliveryAddressMapperTest {

    @Autowired
    private DeliveryAddressMapper deliveryAddressMapper;

    @Test
    void shouldMapDeliveryAddressFieldsWhenSelectingById() {
        DeliveryAddress address = deliveryAddressMapper.selectById(1L);

        assertNotNull(address);
        assertEquals(1L, address.getUserId());
        assertEquals("Test User", address.getReceiverName());
        assertEquals("19900000001", address.getReceiverPhone());
        assertEquals("Test Campus Dormitory 1", address.getAddressDetail());
        assertEquals("School", address.getAddressLabel());
        assertEquals(1, address.getIsDefault());
    }

    @Test
    void shouldInsertAddressWithNullLabel() {
        DeliveryAddress address = newAddress(1L);
        address.setAddressLabel(null);

        assertEquals(1, deliveryAddressMapper.insert(address));
        assertNotNull(address.getId());

        DeliveryAddress saved = deliveryAddressMapper.selectById(address.getId());
        assertNotNull(saved);
        assertNull(saved.getAddressLabel());
    }

    @Test
    void shouldRejectAddressWithUnknownUser() {
        DeliveryAddress address = newAddress(999L);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> deliveryAddressMapper.insert(address)
        );
    }

    @Test
    void shouldRejectAddressWithoutReceiverName() {
        DeliveryAddress address = newAddress(1L);
        address.setReceiverName(null);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> deliveryAddressMapper.insert(address)
        );
    }

    private DeliveryAddress newAddress(Long userId) {
        DeliveryAddress address = new DeliveryAddress();
        address.setUserId(userId);
        address.setReceiverName("New Receiver");
        address.setReceiverPhone("19900000003");
        address.setAddressDetail("Test Address 2");
        address.setAddressLabel("Home");
        address.setIsDefault(0);
        return address;
    }
}
