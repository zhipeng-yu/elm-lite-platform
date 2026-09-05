package com.elmlite.platform.service;

import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.entity.ProductCategory;
import com.elmlite.platform.entity.Shop;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.MerchantMapper;
import com.elmlite.platform.mapper.ProductCategoryMapper;
import com.elmlite.platform.mapper.ShopMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MerchantCategoryService {

    private final MerchantMapper merchantMapper;
    private final ShopMapper shopMapper;
    private final ProductCategoryMapper productCategoryMapper;

    public MerchantCategoryService(
            MerchantMapper merchantMapper,
            ShopMapper shopMapper,
            ProductCategoryMapper productCategoryMapper) {
        this.merchantMapper = merchantMapper;
        this.shopMapper = shopMapper;
        this.productCategoryMapper = productCategoryMapper;
    }

    @Transactional
    public ProductCategory create(
            long merchantId,
            long shopId,
            String categoryName,
            Integer sortOrder) {

        requireActiveMerchant(merchantId);
        Shop shop = requireOwnedShop(merchantId, shopId);

        if (categoryName == null || categoryName.isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "分类名称不能为空");
        }

        String normalizedName = categoryName.trim();

        if (normalizedName.length() > 50) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "分类名称不能超过50个字符");
        }

        validateSortOrder(sortOrder);

        ProductCategory category = new ProductCategory();
        category.setShopId(shop.getId());
        category.setCategoryName(normalizedName);
        category.setSortOrder(sortOrder == null ? 0 : sortOrder);
        category.setStatus(1);

        try {
            productCategoryMapper.insert(category);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "同店铺分类名称已存在");
        }
        return category;
    }

    @Transactional
    public ProductCategory update(
            long merchantId,
            long categoryId,
            String categoryName,
            Integer sortOrder,
            Integer status) {

        requireActiveMerchant(merchantId);

        ProductCategory category =
                productCategoryMapper.selectById(categoryId);

        if (category == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND, "分类不存在");
        }

        requireOwnedShop(merchantId, category.getShopId());

        if (categoryName != null) {
            if (categoryName.isBlank()) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST, "分类名称不能为空");
            }

            String normalizedName = categoryName.trim();

            if (normalizedName.length() > 50) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST, "分类名称不能超过50个字符");
            }

            category.setCategoryName(normalizedName);
        }

        if (sortOrder != null) {
            validateSortOrder(sortOrder);
            category.setSortOrder(sortOrder);
        }

        if (status != null) {
            if (status != 0 && status != 1) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST, "分类状态必须为0或1");
            }
            category.setStatus(status);
        }

        category.setUpdatedAt(LocalDateTime.now());
        try {
            productCategoryMapper.updateById(category);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "同店铺分类名称已存在");
        }

        return category;
    }

    private Shop requireOwnedShop(long merchantId, long shopId) {
        Shop shop = shopMapper.selectById(shopId);

        if (shop == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND, "店铺不存在");
        }

        if (!Long.valueOf(merchantId).equals(shop.getMerchantId())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "无权操作该店铺");
        }

        return shop;
    }

    private void requireActiveMerchant(long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);

        if (merchant == null ||
                !Integer.valueOf(1).equals(merchant.getStatus())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "商家账号不可用");
        }
    }

    private void validateSortOrder(Integer sortOrder) {
        if (sortOrder != null && sortOrder < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "显示顺序不能为负数");
        }
    }
}
