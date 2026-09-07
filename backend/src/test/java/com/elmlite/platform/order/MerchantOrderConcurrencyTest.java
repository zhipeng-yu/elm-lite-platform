package com.elmlite.platform.order;

import com.elmlite.platform.dto.OrderRequest;
import com.elmlite.platform.entity.Product;
import com.elmlite.platform.mapper.ProductMapper;
import com.elmlite.platform.service.MerchantProductService;
import com.elmlite.platform.service.OrderService;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:merchant_order;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@Sql({"/db/h2/order-api-schema.sql", "/db/h2/order-api-data.sql"})
class MerchantOrderConcurrencyTest {
    @Autowired private MerchantProductService merchantProducts;
    @Autowired private OrderService orders;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SqlSessionTemplate sqlSession;
    @MockitoSpyBean private ProductMapper products;

    @Test
    void renamingProductDoesNotRestoreStockConsumedAfterMerchantRead() {
        TransactionTemplate checkout = new TransactionTemplate(transactions);
        checkout.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        // 固定交错顺序：商家已读到库存10 -> 独立下单事务提交库存8 -> 商家写入名称。
        doAnswer(invocation -> {
            checkout.executeWithoutResult(status -> orders.create(1, new OrderRequest(1L, List.of(1L), null)));
            return sqlSession.getMapper(ProductMapper.class).updateById((Product) invocation.getArgument(0));
        }).when(products).updateById(any(Product.class));

        merchantProducts.update(1, 1, null, "改名后的商品", null, null, null, null, null);

        assertEquals(8, jdbc.queryForObject("SELECT stock FROM product WHERE id=1", Integer.class));
        assertEquals("改名后的商品", jdbc.queryForObject("SELECT product_name FROM product WHERE id=1", String.class));
        assertEquals("测试饭", jdbc.queryForObject("SELECT product_name FROM order_item", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class));
    }
}
