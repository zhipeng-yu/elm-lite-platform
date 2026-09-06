package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.CartItem;
import com.elmlite.platform.entity.Product;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.CartItemMapper;
import com.elmlite.platform.mapper.ProductMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final UserService userService;

    public CartService(
            CartItemMapper cartItemMapper,
            ProductMapper productMapper,
            UserService userService) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.userService = userService;
    }

    public List<CartItemResponse> list(long userId) {
        userService.getCurrent(userId);

        return cartItemMapper.selectList(
                        Wrappers.<CartItem>lambdaQuery()
                                .eq(CartItem::getUserId, userId)
                                .orderByAsc(CartItem::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CartItemResponse add(
            long userId,
            Long productId,
            Integer quantity) {

        userService.getCurrent(userId);

        validateQuantity(quantity);

        Product product = productMapper.selectById(productId);

        if (product == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "商品不存在");
        }

        validateProductAvailable(product);

        CartItem existing = cartItemMapper.selectOne(
                Wrappers.<CartItem>lambdaQuery()
                        .eq(CartItem::getUserId, userId)
                        .eq(CartItem::getProductId, productId));

        if (existing == null) {
            validateSingleShop(userId, product);
        }

        int targetQuantity = quantity;

        if (existing != null) {
            targetQuantity = Math.addExact(
                    existing.getQuantity(),
                    quantity);
        }

        validateStock(product, targetQuantity);

        if (existing != null) {
            existing.setQuantity(targetQuantity);
            cartItemMapper.updateById(existing);
            return toResponse(existing, product);
        }

        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setProductId(productId);
        item.setQuantity(quantity);

        cartItemMapper.insert(item);

        return toResponse(item, product);
    }

    @Transactional
    public CartItemResponse update(
            long userId,
            long id,
            Integer quantity) {

        userService.getCurrent(userId);

        validateQuantity(quantity);

        CartItem item = requireOwned(userId, id);

        Product product =
                productMapper.selectById(item.getProductId());

        if (product == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "商品不存在");
        }

        validateProductAvailable(product);
        validateStock(product, quantity);

        item.setQuantity(quantity);
        cartItemMapper.updateById(item);

        return toResponse(item, product);
    }

    @Transactional
    public void delete(
            long userId,
            long id) {

        userService.getCurrent(userId);

        CartItem item = requireOwned(userId, id);

        cartItemMapper.deleteById(item.getId());
    }

    @Transactional
    public void clearPurchasedItems(
            long userId,
            List<Long> productIds) {

        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        cartItemMapper.delete(
                Wrappers.<CartItem>lambdaQuery()
                        .eq(CartItem::getUserId, userId)
                        .in(CartItem::getProductId, productIds));
    }

    private void validateSingleShop(
            long userId,
            Product newProduct) {

        List<CartItem> currentItems =
                cartItemMapper.selectList(
                        Wrappers.<CartItem>lambdaQuery()
                                .eq(CartItem::getUserId, userId));

        for (CartItem item : currentItems) {
            Product existingProduct =
                    productMapper.selectById(item.getProductId());

            if (existingProduct != null
                    && !existingProduct.getShopId()
                    .equals(newProduct.getShopId())) {

                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "购物车只能包含同一店铺的商品");
            }
        }
    }

    private CartItem requireOwned(
            long userId,
            long id) {

        CartItem item = cartItemMapper.selectById(id);

        if (item == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "购物车项不存在");
        }

        if (!Long.valueOf(userId).equals(item.getUserId())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "无权操作该购物车项");
        }

        return item;
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "商品数量必须为正整数");
        }
    }

    private void validateProductAvailable(Product product) {
        if (!Integer.valueOf(1).equals(product.getStatus())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "商品已下架");
        }
    }

    private void validateStock(
            Product product,
            int quantity) {

        if (quantity > product.getStock()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "库存不足");
        }
    }

    private CartItemResponse toResponse(CartItem item) {
        return toResponse(
                item,
                productMapper.selectById(item.getProductId()));
    }

    private CartItemResponse toResponse(
            CartItem item,
            Product product) {

        long priceCent =
                product.getPrice()
                        .movePointRight(2)
                        .longValueExact();

        return new CartItemResponse(
                item.getId(),
                product.getId(),
                product.getShopId(),
                product.getProductName(),
                product.getImageUrl(),
                priceCent,
                product.getStock(),
                product.getStatus(),
                item.getQuantity(),
                Math.multiplyExact(
                        priceCent,
                        item.getQuantity()));
    }

    public record CartItemResponse(
            Long id,
            Long productId,
            Long shopId,
            String productName,
            String imageUrl,
            Long priceCent,
            Integer stock,
            Integer status,
            Integer quantity,
            Long subtotalCent) {
    }
}
