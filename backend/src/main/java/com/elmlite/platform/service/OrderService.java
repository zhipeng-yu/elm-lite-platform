package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.Order;
import com.elmlite.platform.entity.OrderItem;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.OrderItemMapper;
import com.elmlite.platform.mapper.OrderMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class OrderService {
    private final OrderMapper orders;
    private final OrderItemMapper items;
    private final UserService users;

    public OrderService(OrderMapper orders, OrderItemMapper items, UserService users) {
        this.orders = orders;
        this.items = items;
        this.users = users;
    }

    public List<Summary> list(long userId) {
        users.getCurrent(userId);
        return orders.selectList(Wrappers.<Order>lambdaQuery().eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt, Order::getId)).stream().map(Summary::from).toList();
    }

    public Detail get(long userId, long id) {
        users.getCurrent(userId);
        Order order = orders.selectById(id);
        if (order == null) throw new BusinessException(HttpStatus.NOT_FOUND, "订单不存在");
        if (!Long.valueOf(userId).equals(order.getUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权查看该订单");
        }
        List<Item> lines = items.selectList(Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, id)
                .orderByAsc(OrderItem::getId)).stream().map(Item::from).toList();
        return new Detail(order.getId(), order.getOrderNo(), order.getShopId(), order.getOrderStatus(),
                order.getTotalAmount().movePointRight(2).longValueExact(),
                order.getCreatedAt().atOffset(ZoneOffset.ofHours(8)), order.getReceiverName(),
                order.getReceiverPhone(), order.getDeliveryAddress(),
                order.getProductAmount().movePointRight(2).longValueExact(),
                order.getDeliveryFee().movePointRight(2).longValueExact(), order.getRemark(), lines);
    }

    public record Summary(Long id, String orderNo, Long shopId, Integer orderStatus,
                          Long totalAmountCent, OffsetDateTime createdAt) {
        static Summary from(Order order) {
            return new Summary(order.getId(), order.getOrderNo(), order.getShopId(), order.getOrderStatus(),
                    order.getTotalAmount().movePointRight(2).longValueExact(),
                    order.getCreatedAt().atOffset(ZoneOffset.ofHours(8)));
        }
    }

    public record Detail(Long id, String orderNo, Long shopId, Integer orderStatus, Long totalAmountCent,
                         OffsetDateTime createdAt, String receiverName, String receiverPhone, String deliveryAddress,
                         Long productAmountCent, Long deliveryFeeCent, String remark, List<Item> items) { }

    public record Item(Long productId, String productName, Long unitPriceCent, Integer quantity, Long subtotalCent) {
        static Item from(OrderItem item) {
            return new Item(item.getProductId(), item.getProductName(),
                    item.getUnitPrice().movePointRight(2).longValueExact(), item.getQuantity(),
                    item.getSubtotal().movePointRight(2).longValueExact());
        }
    }
}
