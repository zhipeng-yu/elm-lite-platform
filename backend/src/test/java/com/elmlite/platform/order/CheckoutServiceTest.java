package com.elmlite.platform.order;

import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.service.CheckoutService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:checkout_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@Sql({"/db/h2/order-api-schema.sql", "/db/h2/order-api-data.sql"})
class CheckoutServiceTest {
    @Autowired private CheckoutService checkout;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void helpersRequireExistingTransaction() {
        assertThrows(IllegalTransactionStateException.class, () -> checkout.deductStock(1, 1));
        assertThrows(IllegalTransactionStateException.class, () -> checkout.clearItems(1, List.of(1L)));
        assertEquals(10, jdbc.queryForObject("SELECT stock FROM product WHERE id=1", Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM cart_item", Integer.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void invalidQuantityDoesNotChangeStock(int quantity) {
        BusinessException error = assertThrows(BusinessException.class, () ->
                new TransactionTemplate(transactions).execute(status -> checkout.deductStock(1, quantity)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals(10, jdbc.queryForObject("SELECT stock FROM product WHERE id=1", Integer.class));
    }

    @Test
    void missingProductReturnsNotFound() {
        BusinessException error = assertThrows(BusinessException.class, () ->
                new TransactionTemplate(transactions).execute(status -> checkout.deductStock(999, 1)));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }

    @Test
    void mismatchedCleanupRollsBackDeletionAndNeverDeletesOtherUsersItems() {
        BusinessException error = assertThrows(BusinessException.class, () ->
                new TransactionTemplate(transactions).executeWithoutResult(status -> checkout.clearItems(1, List.of(1L, 3L))));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertEquals(List.of(1L, 2L, 3L), jdbc.queryForList("SELECT id FROM cart_item ORDER BY id", Long.class));
    }
}
