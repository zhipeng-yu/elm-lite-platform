package com.elmlite.platform.mapper;

import com.elmlite.platform.entity.ProductCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:product_category_mapper_test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@Sql(
        scripts = {
                "/db/h2/product-category-schema.sql",
                "/db/h2/product-category-data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class ProductCategoryMapperTest {

    @Autowired
    private ProductCategoryMapper productCategoryMapper;

    @Test
    void shouldMapCategoryFieldsWhenSelectingById() {
        ProductCategory category = productCategoryMapper.selectById(1L);

        assertNotNull(category);
        assertEquals(1L, category.getShopId());
        assertEquals("Main Food", category.getCategoryName());
        assertEquals(1, category.getSortOrder());
        assertEquals(1, category.getStatus());
    }

    @Test
    void shouldInsertCategory() {
        ProductCategory category = newCategory(1L, "Desserts");

        assertEquals(1, productCategoryMapper.insert(category));
        assertNotNull(category.getId());
    }

    @Test
    void shouldRejectDuplicateCategoryNameInSameShop() {
        ProductCategory category = newCategory(1L, "Main Food");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> productCategoryMapper.insert(category)
        );
    }

    @Test
    void shouldRejectCategoryWithUnknownShop() {
        ProductCategory category = newCategory(999L, "Unknown Shop Category");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> productCategoryMapper.insert(category)
        );
    }

    private ProductCategory newCategory(Long shopId, String categoryName) {
        ProductCategory category = new ProductCategory();
        category.setShopId(shopId);
        category.setCategoryName(categoryName);
        category.setSortOrder(3);
        category.setStatus(1);
        return category;
    }
}
