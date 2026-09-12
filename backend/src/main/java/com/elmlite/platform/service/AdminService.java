package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.entity.Order;
import com.elmlite.platform.entity.OrderItem;
import com.elmlite.platform.entity.Shop;
import com.elmlite.platform.entity.User;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.MerchantMapper;
import com.elmlite.platform.mapper.OrderItemMapper;
import com.elmlite.platform.mapper.OrderMapper;
import com.elmlite.platform.mapper.ShopMapper;
import com.elmlite.platform.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final ShopMapper shopMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    public AdminService(
            UserMapper userMapper,
            MerchantMapper merchantMapper,
            ShopMapper shopMapper,
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper) {
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.shopMapper = shopMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    public List<User> listUsers(String keyword, Integer status) {
        return userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                                .like(User::getUsername, keyword.trim())
                                .or()
                                .like(User::getNickname, keyword.trim()))
                        .eq(status != null, User::getStatus, status)
                        .orderByAsc(User::getId));
    }

    public List<Merchant> listMerchants(String keyword, Integer status) {
        return merchantMapper.selectList(
                Wrappers.<Merchant>lambdaQuery()
                        .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                                .like(Merchant::getAccount, keyword.trim())
                                .or()
                                .like(Merchant::getMerchantName, keyword.trim()))
                        .eq(status != null, Merchant::getStatus, status)
                        .orderByAsc(Merchant::getId));
    }

    public List<Shop> listShops() {
        return shopMapper.selectList(
                Wrappers.<Shop>lambdaQuery().orderByAsc(Shop::getId));
    }

    public List<Order> listOrders(Long shopId, Integer orderStatus) {
        if (orderStatus != null && (orderStatus < 0 || orderStatus > 5)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "订单状态必须为 0-5 的整数");
        }
        return orderMapper.selectList(
                Wrappers.<Order>lambdaQuery()
                        .eq(shopId != null, Order::getShopId, shopId)
                        .eq(orderStatus != null, Order::getOrderStatus, orderStatus)
                        .orderByDesc(Order::getCreatedAt)
                        .orderByDesc(Order::getId));
    }

    public Order getOrder(long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    public List<OrderItem> listOrderItems(long orderId) {
        return orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery()
                        .eq(OrderItem::getOrderId, orderId)
                        .orderByAsc(OrderItem::getId));
    }

    @Transactional
    public User updateUserStatus(long userId, Integer status) {
        validateStatus(status);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        return user;
    }

    @Transactional
    public Merchant updateMerchantStatus(long merchantId, Integer status) {
        validateStatus(status);
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "商家不存在");
        }
        merchant.setStatus(status);
        merchantMapper.updateById(merchant);
        return merchant;
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "状态必须为 0 或 1");
        }
    }
}
