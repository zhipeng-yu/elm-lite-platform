package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.CartItem;
import com.elmlite.platform.entity.Product;
import com.elmlite.platform.mapper.CartItemMapper;
import com.elmlite.platform.mapper.ProductMapper;
import org.springframework.stereotype.Service;

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

    public CartItemResponse add(
            long userId,
            Long productId,
            Integer quantity) {

        userService.getCurrent(userId);

        Product product = productMapper.selectById(productId);

        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setProductId(productId);
        item.setQuantity(quantity);

        cartItemMapper.insert(item);

        return toResponse(item, product);
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
                Math.multiplyExact(priceCent, item.getQuantity()));
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
