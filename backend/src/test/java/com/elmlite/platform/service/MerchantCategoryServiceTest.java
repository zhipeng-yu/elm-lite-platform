package com.elmlite.platform.service;

import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.entity.ProductCategory;
import com.elmlite.platform.entity.Shop;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.MerchantMapper;
import com.elmlite.platform.mapper.ProductCategoryMapper;
import com.elmlite.platform.mapper.ShopMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantCategoryServiceTest {
    @Mock private MerchantMapper merchantMapper;
    @Mock private ShopMapper shopMapper;
    @Mock private ProductCategoryMapper categoryMapper;
    @InjectMocks private MerchantCategoryService categoryService;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void duplicateKeyDuringWriteReturnsConflict(boolean updating) {
        Merchant merchant = new Merchant();
        merchant.setStatus(1);
        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        Shop shop = new Shop();
        shop.setId(2L);
        shop.setMerchantId(1L);
        when(shopMapper.selectById(2L)).thenReturn(shop);

        if (updating) {
            ProductCategory category = new ProductCategory();
            category.setId(3L);
            category.setShopId(2L);
            category.setCategoryName("Original");
            when(categoryMapper.selectById(3L)).thenReturn(category);
            when(categoryMapper.updateById(any(ProductCategory.class)))
                    .thenThrow(new DuplicateKeyException("duplicate category name"));
        } else {
            when(categoryMapper.insert(any(ProductCategory.class)))
                    .thenThrow(new DuplicateKeyException("duplicate category name"));
        }

        BusinessException error = assertThrows(BusinessException.class, () -> {
            if (updating) {
                categoryService.update(1L, 3L, "Taken name", null, null);
            } else {
                categoryService.create(1L, 2L, "Taken name", 0);
            }
        });

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
    }
}
