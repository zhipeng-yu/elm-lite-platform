package com.elmlite.platform.mapper;

import com.elmlite.platform.entity.Shop;
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
        "spring.datasource.url=jdbc:h2:mem:shop_mapper_test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@Sql(
        scripts = {
                "/db/h2/shop-schema.sql",
                "/db/h2/shop-data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class ShopMapperTest {

    @Autowired
    private ShopMapper shopMapper;

    @Test
    void shouldMapShopAndMoneyFieldsWhenSelectingById() {
        Shop shop = shopMapper.selectById(1L);

        assertNotNull(shop);
        assertEquals(1L, shop.getMerchantId());
        assertEquals("Campus Food Shop", shop.getShopName());
        assertEquals(new BigDecimal("15.00"), shop.getStartPrice());
        assertEquals(new BigDecimal("3.00"), shop.getDeliveryPrice());
        assertEquals(1, shop.getBusinessStatus());
    }

    @Test
    void shouldInsertShopWithExactMoneyValues() {
        Shop shop = newShop(1L);
        shop.setStartPrice(new BigDecimal("20.50"));
        shop.setDeliveryPrice(new BigDecimal("4.25"));

        assertEquals(1, shopMapper.insert(shop));
        assertNotNull(shop.getId());

        Shop saved = shopMapper.selectById(shop.getId());
        assertEquals(new BigDecimal("20.50"), saved.getStartPrice());
        assertEquals(new BigDecimal("4.25"), saved.getDeliveryPrice());
    }

    @Test
    void shouldRejectShopWithUnknownMerchant() {
        Shop shop = newShop(999L);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> shopMapper.insert(shop)
        );
    }

    private Shop newShop(Long merchantId) {
        Shop shop = new Shop();
        shop.setMerchantId(merchantId);
        shop.setShopName("New Shop");
        shop.setDescription("Shop mapper test");
        shop.setAddress("Test Address 2");
        shop.setStartPrice(new BigDecimal("10.00"));
        shop.setDeliveryPrice(new BigDecimal("2.00"));
        shop.setBusinessStatus(1);
        return shop;
    }
}
