package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.Product;
import com.elmlite.platform.entity.ProductCategory;
import com.elmlite.platform.entity.ProductDetailImage;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.CartItemMapper;
import com.elmlite.platform.mapper.ProductCategoryMapper;
import com.elmlite.platform.mapper.ProductDetailImageMapper;
import com.elmlite.platform.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductCategoryMapper productCategoryMapper;
    private final ProductDetailImageMapper productDetailImageMapper;
    private final CartItemMapper cartItemMapper;

    @Autowired
    public ProductService(
            ProductMapper productMapper,
            ProductCategoryMapper productCategoryMapper,
            ProductDetailImageMapper productDetailImageMapper,
            CartItemMapper cartItemMapper) {

        this.productMapper = productMapper;
        this.productCategoryMapper = productCategoryMapper;
        this.productDetailImageMapper = productDetailImageMapper;
        this.cartItemMapper = cartItemMapper;
    }

    public ProductService(
            ProductMapper productMapper,
            ProductCategoryMapper productCategoryMapper) {

        this.productMapper = productMapper;
        this.productCategoryMapper = productCategoryMapper;
        this.productDetailImageMapper = null;
        this.cartItemMapper = null;
    }

    public List<CategoryResponse> listCategories(long shopId) {
        return productCategoryMapper.selectList(
                        Wrappers.<ProductCategory>lambdaQuery()
                                .eq(ProductCategory::getShopId, shopId)
                                .eq(ProductCategory::getStatus, 1)
                                .orderByAsc(ProductCategory::getSortOrder)
                                .orderByAsc(ProductCategory::getId))
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public List<ProductListResponse> listProducts(
            long shopId,
            Long categoryId) {

        var query = Wrappers.<Product>lambdaQuery()
                .eq(Product::getShopId, shopId)
                .eq(Product::getStatus, 1)
                .orderByAsc(Product::getId);

        if (categoryId != null) {
            query.eq(Product::getCategoryId, categoryId);
        }

        return productMapper.selectList(query)
                .stream()
                .map(product -> ProductListResponse.from(
                        product,
                        availableStock(product)))
                .toList();
    }

    public ProductDetailResponse getProduct(long id) {
        Product product = productMapper.selectById(id);

        if (product == null
                || !Integer.valueOf(1)
                .equals(product.getStatus())) {

            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "商品不存在");
        }

        ProductCategory category =
                productCategoryMapper.selectById(
                        product.getCategoryId());

        if (category == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "商品分类不存在");
        }

        List<String> detailImageUrls =
                productDetailImageMapper == null
                        ? List.of()
                        : productDetailImageMapper
                                .selectByProductId(product.getId())
                                .stream()
                                .map(ProductDetailImage::getImageUrl)
                                .toList();

        return ProductDetailResponse.from(
                product,
                category,
                detailImageUrls,
                availableStock(product));
    }

    private int availableStock(Product product) {
        long reserved = cartItemMapper == null
                ? 0
                : cartItemMapper.sumQuantityByProductId(product.getId());
        return (int) Math.max(0L, (long) product.getStock() - reserved);
    }

    public record CategoryResponse(
            Long id,
            String categoryName,
            Integer sortOrder) {

        static CategoryResponse from(
                ProductCategory category) {

            return new CategoryResponse(
                    category.getId(),
                    category.getCategoryName(),
                    category.getSortOrder());
        }
    }

    public record ProductListResponse(
            Long id,
            Long categoryId,
            String productName,
            String description,
            String imageUrl,
            Long priceCent,
            Integer stock,
            Integer status) {

        static ProductListResponse from(
                Product product,
                int availableStock) {
            return new ProductListResponse(
                    product.getId(),
                    product.getCategoryId(),
                    product.getProductName(),
                    product.getDescription(),
                    product.getImageUrl(),
                    product.getPrice()
                            .movePointRight(2)
                            .longValueExact(),
                    availableStock,
                    product.getStatus());
        }
    }

    public record ProductDetailResponse(
            Long id,
            Long shopId,
            Long categoryId,
            String categoryName,
            String productName,
            String description,
            String imageUrl,
            List<String> detailImageUrls,
            Long priceCent,
            Integer stock,
            Integer status) {

        static ProductDetailResponse from(
                Product product,
                ProductCategory category,
                List<String> detailImageUrls,
                int availableStock) {

            return new ProductDetailResponse(
                    product.getId(),
                    product.getShopId(),
                    product.getCategoryId(),
                    category.getCategoryName(),
                    product.getProductName(),
                    product.getDescription(),
                    product.getImageUrl(),
                    detailImageUrls,
                    product.getPrice()
                            .movePointRight(2)
                            .longValueExact(),
                    availableStock,
                    product.getStatus());
        }
    }
}
