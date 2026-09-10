package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.Order;
import com.elmlite.platform.entity.OrderItem;
import com.elmlite.platform.entity.User;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.MerchantMapper;
import com.elmlite.platform.mapper.OrderItemMapper;
import com.elmlite.platform.mapper.OrderMapper;
import com.elmlite.platform.mapper.ShopMapper;
import com.elmlite.platform.mapper.UserMapper;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MerchantOrderService {
    private final MerchantMapper merchants;
    private final ShopMapper shops;
    private final OrderMapper orders;
    private final OrderItemMapper items;
    private final UserMapper users;

    public MerchantOrderService(MerchantMapper merchants, ShopMapper shops, OrderMapper orders,
                                OrderItemMapper items, UserMapper users) {
        this.merchants = merchants;
        this.shops = shops;
        this.orders = orders;
        this.items = items;
        this.users = users;
    }

    public List<Summary> list(long merchantId, long shopId, Integer orderStatus) {
        requireActiveMerchant(merchantId);
        requireOwnedShop(merchantId, shopId);
        if (orderStatus != null && (orderStatus < 0 || orderStatus > 5)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "订单状态必须为0至5");
        }
        List<Order> selected = orders.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getShopId, shopId).eq(orderStatus != null, Order::getOrderStatus, orderStatus)
                .orderByDesc(Order::getCreatedAt, Order::getId));
        if (selected.isEmpty()) return List.of();
        var linesByOrder = items.selectList(Wrappers.<OrderItem>lambdaQuery()
                        .in(OrderItem::getOrderId, selected.stream().map(Order::getId).toList())
                        .orderByAsc(OrderItem::getId)).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId,
                        Collectors.mapping(line -> new Item(line.getProductName(), line.getQuantity()),
                                Collectors.toList())));
        return selected.stream().map(order -> new Summary(OrderService.Summary.from(order),
                order.getReceiverName(), linesByOrder.getOrDefault(order.getId(), List.of()))).toList();
    }

    public Detail get(long merchantId, long id) {
        requireActiveMerchant(merchantId);
        Order order = orders.selectById(id);
        if (order == null) throw new BusinessException(HttpStatus.NOT_FOUND, "订单不存在");
        requireOwnedShop(merchantId, order.getShopId());
        List<OrderService.Item> lines = items.selectList(Wrappers.<OrderItem>lambdaQuery()
                        .eq(OrderItem::getOrderId, id).orderByAsc(OrderItem::getId)).stream()
                .map(OrderService.Item::from).toList();
        // 历史订单不因顾客停用而消失；仅取展示所需账号字段，收货信息仍使用订单快照。
        var buyer = users.selectOne(Wrappers.<User>lambdaQuery().select(User::getId, User::getNickname)
                .eq(User::getId, order.getUserId()));
        return new Detail(OrderService.Detail.from(order, lines), new Buyer(buyer.getId(), buyer.getNickname()));
    }

    private void requireActiveMerchant(long merchantId) {
        var merchant = merchants.selectById(merchantId);
        if (merchant == null || !Integer.valueOf(1).equals(merchant.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "商家账号不可用");
        }
    }

    private void requireOwnedShop(long merchantId, long shopId) {
        var shop = shops.selectById(shopId);
        if (shop == null) throw new BusinessException(HttpStatus.NOT_FOUND, "店铺不存在");
        if (!Long.valueOf(merchantId).equals(shop.getMerchantId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权查看该店铺订单");
        }
    }

    // 展开已有订单 DTO，保持与顾客接口相同的平铺 JSON 字段及快照转换。
    public record Summary(@JsonUnwrapped OrderService.Summary order, String receiverName, List<Item> items) { }
    public record Item(String productName, Integer quantity) { }
    public record Detail(@JsonUnwrapped OrderService.Detail order, Buyer buyer) { }
    public record Buyer(Long id, String displayName) { }
}
