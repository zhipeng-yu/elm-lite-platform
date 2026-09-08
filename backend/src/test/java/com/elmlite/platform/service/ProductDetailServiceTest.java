package com.elmlite.platform.service;

import com.elmlite.platform.entity.Product;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.ProductCategoryMapper;
import com.elmlite.platform.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductDetailServiceTest {
    @Test void missingReferencedCategoryReturnsNotFoundInsteadOfBrokenDetail() {
        ProductMapper products = mock(ProductMapper.class);
        ProductCategoryMapper categories = mock(ProductCategoryMapper.class);
        Product product = new Product();
        product.setStatus(1);
        product.setCategoryId(9L);
        when(products.selectById(1L)).thenReturn(product);
        when(categories.selectById(9L)).thenReturn(null);

        BusinessException error = assertThrows(BusinessException.class,
                () -> new ProductService(products, categories).getProduct(1L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }
}
