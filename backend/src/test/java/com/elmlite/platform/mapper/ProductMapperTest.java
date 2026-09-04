package com.elmlite.platform.mapper;

import com.elmlite.platform.entity.Product;
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
        "spring.datasource.url=jdbc:h2:mem:product_mapper_test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@Sql(
        scripts = {
                "/db/h2/product-schema.sql",
                "/db/h2/product-data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class ProductMapperTest {

    @Autowired
    private ProductMapper productMapper;

    @Test
    void shouldMapProductFieldsAndExactPriceWhenSelectingById() {
        Product product = productMapper.selectById(1L);

        assertNotNull(product);
        assertEquals(1L, product.getShopId());
        assertEquals(1L, product.getCategoryId());
        assertEquals("Beef Rice", product.getProductName());
        assertEquals("Main food test product", product.getDescription());
        assertEquals(0, product.getPrice().compareTo(new BigDecimal("18.00")));
        assertEquals(100, product.getStock());
        assertEquals(1, product.getStatus());
    }

    @Test
    void shouldInsertProductWithExactPrice() {
        Product product = newProduct(1L, 1L, "Noodles", "12.34");

        assertEquals(1, productMapper.insert(product));
        assertNotNull(product.getId());

        Product saved = productMapper.selectById(product.getId());
        assertNotNull(saved);
        assertEquals(0, saved.getPrice().compareTo(new BigDecimal("12.34")));
    }

    @Test
    void shouldRejectProductWithUnknownShop() {
        Product product = newProduct(999L, 1L, "Unknown Shop Product", "10.00");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> productMapper.insert(product)
        );
    }

    @Test
    void shouldRejectProductWithUnknownCategory() {
        Product product = newProduct(1L, 999L, "Unknown Category Product", "10.00");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> productMapper.insert(product)
        );
    }

    private Product newProduct(
            Long shopId,
            Long categoryId,
            String productName,
            String price
    ) {
        Product product = new Product();
        product.setShopId(shopId);
        product.setCategoryId(categoryId);
        product.setProductName(productName);
        product.setDescription("Product mapper test data");
        product.setPrice(new BigDecimal(price));
        product.setStock(20);
        product.setStatus(1);
        return product;
    }
}
