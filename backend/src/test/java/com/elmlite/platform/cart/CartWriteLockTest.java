package com.elmlite.platform.cart;

import com.elmlite.platform.service.CartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:cart_write_lock;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=5000")
@Sql({"/db/h2/cart-api-schema.sql", "/db/h2/cart-api-data.sql"})
class CartWriteLockTest {
    @Autowired private CartService carts;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;

    @Test
    void concurrentAddsDoNotLoseQuantity() throws Exception {
        jdbc.update("UPDATE product SET stock = 100 WHERE id = 1");
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Void> add = () -> {
                ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                for (int i = 0; i < 10; i++) carts.add(1, 1L, 1);
                return null;
            };
            var first = executor.submit(add);
            var second = executor.submit(add);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }
        assertEquals(22, jdbc.queryForObject("SELECT quantity FROM cart_item WHERE id = 1", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM cart_item WHERE user_id = 1", Integer.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"add", "update", "delete"})
    void cartWritesWaitForTheUserLockHeldByCheckout(String operation) throws Exception {
        var transaction = new TransactionTemplate(transactions);
        var held = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var started = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var checkout = executor.submit(() -> transaction.executeWithoutResult(status -> {
                jdbc.queryForObject("SELECT id FROM users WHERE id = 1 FOR UPDATE", Long.class);
                held.countDown();
                try {
                    assertTrue(release.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
            }));
            assertTrue(held.await(5, TimeUnit.SECONDS));
            var write = executor.submit(() -> {
                started.countDown();
                switch (operation) {
                    case "add" -> carts.add(1, 1L, 1);
                    case "update" -> carts.update(1, 1, 4);
                    case "delete" -> carts.delete(1, 1);
                    default -> throw new AssertionError(operation);
                }
            });
            try {
                assertTrue(started.await(5, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> write.get(500, TimeUnit.MILLISECONDS));
                assertEquals(2, jdbc.queryForObject("SELECT quantity FROM cart_item WHERE id = 1", Integer.class));
            } finally {
                release.countDown();
            }
            checkout.get(5, TimeUnit.SECONDS);
            write.get(5, TimeUnit.SECONDS);
            assertEquals(operation.equals("delete") ? 0 : 1,
                    jdbc.queryForObject("SELECT COUNT(*) FROM cart_item WHERE id = 1", Integer.class));
            if (!operation.equals("delete")) {
                assertEquals(operation.equals("add") ? 3 : 4,
                        jdbc.queryForObject("SELECT quantity FROM cart_item WHERE id = 1", Integer.class));
            }
        }
    }
}
