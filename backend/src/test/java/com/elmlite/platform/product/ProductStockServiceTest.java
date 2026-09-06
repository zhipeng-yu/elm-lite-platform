package com.elmlite.platform.product;

import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:product_stock;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@Sql({
        "/db/h2/cart-api-schema.sql",
        "/db/h2/cart-api-data.sql"
})
class ProductStockServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deductsAvailableStock() {
        productService.validateAndDeduct(1L, 3);

        assertEquals(
                Integer.valueOf(7),
                jdbc.queryForObject(
                        "SELECT stock FROM product WHERE id = 1",
                        Integer.class));
    }

    @Test
    void rejectsMissingProduct() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.validateAndDeduct(999L, 1));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void rejectsOffShelfProduct() {
        jdbc.update(
                "UPDATE product SET status = 0 WHERE id = 1");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.validateAndDeduct(1L, 1));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());

        assertEquals(
                Integer.valueOf(10),
                jdbc.queryForObject(
                        "SELECT stock FROM product WHERE id = 1",
                        Integer.class));
    }

    @Test
    void rejectsInsufficientStockWithoutChangingStock() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.validateAndDeduct(2L, 6));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());

        assertEquals(
                Integer.valueOf(5),
                jdbc.queryForObject(
                        "SELECT stock FROM product WHERE id = 2",
                        Integer.class));
    }

    @Test
    void rejectsNonPositiveQuantity() {
        for (int quantity : new int[]{0, -1}) {
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> productService.validateAndDeduct(
                            1L,
                            quantity));

            assertEquals(
                    HttpStatus.BAD_REQUEST,
                    exception.getStatus());
        }

        assertEquals(
                Integer.valueOf(10),
                jdbc.queryForObject(
                        "SELECT stock FROM product WHERE id = 1",
                        Integer.class));
    }
}
