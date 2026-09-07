package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.CartItem;
import com.elmlite.platform.entity.Product;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.CartItemMapper;
import com.elmlite.platform.mapper.ProductMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class CheckoutService {
    private final ProductMapper products;
    private final CartItemMapper carts;

    public CheckoutService(ProductMapper products, CartItemMapper carts) {
        this.products = products;
        this.carts = carts;
    }

    public Product deductStock(long productId, int quantity) {
        if (quantity <= 0) throw new BusinessException(HttpStatus.BAD_REQUEST, "商品数量必须为正整数");
        Product product = products.lockById(productId);
        if (product == null) throw new BusinessException(HttpStatus.NOT_FOUND, "商品不存在");
        if (products.deductStock(productId, quantity) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "商品已下架或库存不足");
        }
        return product;
    }

    public void clearItems(long userId, List<Long> cartItemIds) {
        int deleted = carts.delete(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId).in(CartItem::getId, cartItemIds));
        if (deleted != cartItemIds.size()) {
            throw new BusinessException(HttpStatus.CONFLICT, "购物车已变化，请重新确认");
        }
    }
}
