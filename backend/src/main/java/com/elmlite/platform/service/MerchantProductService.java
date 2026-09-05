package com.elmlite.platform.service;

import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.entity.Product;
import com.elmlite.platform.entity.ProductCategory;
import com.elmlite.platform.entity.Shop;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.MerchantMapper;
import com.elmlite.platform.mapper.ProductCategoryMapper;
import com.elmlite.platform.mapper.ProductMapper;
import com.elmlite.platform.mapper.ShopMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class MerchantProductService {

    private static final long MAX_PRICE_CENT = 9_999_999_999L;

    private final MerchantMapper merchantMapper;
    private final ShopMapper shopMapper;
    private final ProductCategoryMapper productCategoryMapper;
    private final ProductMapper productMapper;

    public MerchantProductService(
            MerchantMapper merchantMapper,
            ShopMapper shopMapper,
            ProductCategoryMapper productCategoryMapper,
            ProductMapper productMapper) {
        this.merchantMapper = merchantMapper;
        this.shopMapper = shopMapper;
        this.productCategoryMapper = productCategoryMapper;
        this.productMapper = productMapper;
    }

    @Transactional
    public Product create(
            long merchantId,
            long shopId,
            Long categoryId,
            String productName,
            String description,
            String imageUrl,
            Long priceCent,
            Integer stock) {

        requireActiveMerchant(merchantId);

        Shop shop = requireOwnedShop(merchantId, shopId);

        ProductCategory category =
                requireUsableCategory(
                        merchantId,
                        shop,
                        categoryId);

        validateProductName(productName);
        validateDescription(description);
        validateImageUrl(imageUrl);
        validatePrice(priceCent);
        validateStock(stock);

        Product product = new Product();
        product.setShopId(shopId);
        product.setCategoryId(category.getId());
        product.setProductName(productName.trim());
        product.setDescription(description);
        product.setImageUrl(imageUrl);
        product.setPrice(BigDecimal.valueOf(priceCent, 2));
        product.setStock(stock);
        product.setStatus(1);

        productMapper.insert(product);

        return product;
    }

    @Transactional
    public Product update(
            long merchantId,
            long productId,
            Long categoryId,
            String productName,
            String description,
            String imageUrl,
            Long priceCent,
            Integer stock,
            Integer status) {

        requireActiveMerchant(merchantId);

        Product product = productMapper.selectById(productId);

        if (product == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "商品不存在");
        }

        Shop shop =
                requireOwnedShop(
                        merchantId,
                        product.getShopId());

        if (categoryId != null) {
            ProductCategory category =
                    requireUsableCategory(
                            merchantId,
                            shop,
                            categoryId);

            product.setCategoryId(category.getId());
        }

        if (productName != null) {
            validateProductName(productName);
            product.setProductName(productName.trim());
        }

        if (description != null) {
            validateDescription(description);
            product.setDescription(description);
        }

        if (imageUrl != null) {
            validateImageUrl(imageUrl);
            product.setImageUrl(imageUrl);
        }

        if (priceCent != null) {
            validatePrice(priceCent);
            product.setPrice(
                    BigDecimal.valueOf(priceCent, 2));
        }

        if (stock != null) {
            validateStock(stock);
            product.setStock(stock);
        }

        if (status != null) {
            if (status != 0 && status != 1) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "商品状态必须为0或1");
            }

            product.setStatus(status);
        }

        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        return product;
    }

    private ProductCategory requireUsableCategory(
            long merchantId,
            Shop currentShop,
            Long categoryId) {

        if (categoryId == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "分类不能为空");
        }

        ProductCategory category =
                productCategoryMapper.selectById(categoryId);

        if (category == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "分类不存在");
        }

        if (!currentShop.getId().equals(category.getShopId())) {

            Shop categoryShop =
                    shopMapper.selectById(category.getShopId());

            if (categoryShop != null
                    && !Long.valueOf(merchantId)
                    .equals(categoryShop.getMerchantId())) {

                throw new BusinessException(
                        HttpStatus.FORBIDDEN,
                        "无权使用其他商家的分类");
            }

            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "分类不属于当前店铺");
        }

        if (!Integer.valueOf(1).equals(category.getStatus())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "停用分类不能关联商品");
        }

        return category;
    }

    private Shop requireOwnedShop(
            long merchantId,
            long shopId) {

        Shop shop = shopMapper.selectById(shopId);

        if (shop == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "店铺不存在");
        }

        if (!Long.valueOf(merchantId)
                .equals(shop.getMerchantId())) {

            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "无权操作该店铺");
        }

        return shop;
    }

    private void requireActiveMerchant(long merchantId) {
        Merchant merchant =
                merchantMapper.selectById(merchantId);

        if (merchant == null
                || !Integer.valueOf(1)
                .equals(merchant.getStatus())) {

            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "商家账号不可用");
        }
    }

    private void validateProductName(String productName) {
        if (productName == null
                || productName.isBlank()) {

            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "商品名称不能为空");
        }

        if (productName.trim().length() > 100) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "商品名称不能超过100个字符");
        }
    }

    private void validateDescription(String description) {
        if (description != null
                && description.length() > 255) {

            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "商品描述不能超过255个字符");
        }
    }

    private void validateImageUrl(String imageUrl) {
        if (imageUrl != null
                && imageUrl.length() > 255) {

            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "图片地址不能超过255个字符");
        }
    }

    private void validatePrice(Long priceCent) {
        if (priceCent == null
                || priceCent <= 0
                || priceCent > MAX_PRICE_CENT) {

            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "商品价格超出允许范围");
        }
    }

    private void validateStock(Integer stock) {
        if (stock == null || stock < 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "商品库存不能为负数");
        }
    }
}
