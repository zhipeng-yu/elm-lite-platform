package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.dto.OrderRequest;
import com.elmlite.platform.entity.CartItem;
import com.elmlite.platform.entity.DeliveryAddress;
import com.elmlite.platform.entity.Order;
import com.elmlite.platform.entity.OrderItem;
import com.elmlite.platform.entity.Product;
import com.elmlite.platform.entity.Shop;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.CartItemMapper;
import com.elmlite.platform.mapper.OrderItemMapper;
import com.elmlite.platform.mapper.OrderMapper;
import com.elmlite.platform.mapper.ShopMapper;
import com.elmlite.platform.mapper.UserMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderMapper orders;
    private final OrderItemMapper items;
    private final UserService users;
    private final UserMapper userMapper;
    private final AddressService addresses;
    private final CartItemMapper carts;
    private final ShopMapper shops;
    private final CheckoutService checkout;
    private final Validator validator;

    public OrderService(OrderMapper orders, OrderItemMapper items, UserService users, UserMapper userMapper,
                        AddressService addresses, CartItemMapper carts, ShopMapper shops,
                        CheckoutService checkout, Validator validator) {
        this.orders = orders;
        this.items = items;
        this.users = users;
        this.userMapper = userMapper;
        this.addresses = addresses;
        this.carts = carts;
        this.shops = shops;
        this.checkout = checkout;
        this.validator = validator;
    }

    @Transactional
    public Detail create(long userId, OrderRequest request) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
        if (new HashSet<>(request.cartItemIds()).size() != request.cartItemIds().size()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "购物车项不能重复");
        }
        // 与地址写入共用用户锁；同一购物车的重复提交在前一事务结束后重新读取。
        userMapper.lockById(userId);
        users.getCurrent(userId);
        DeliveryAddress address = addresses.get(userId, request.addressId());
        List<CartItem> selected = carts.selectList(Wrappers.<CartItem>lambdaQuery()
                .in(CartItem::getId, request.cartItemIds()).orderByAsc(CartItem::getProductId).last("FOR UPDATE"));
        if (selected.size() != request.cartItemIds().size()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "购物车项不存在");
        }
        for (CartItem cart : selected) {
            if (!Long.valueOf(userId).equals(cart.getUserId())) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "无权操作该购物车项");
            }
        }

        Long shopId = null;
        BigDecimal productAmount = BigDecimal.ZERO;
        List<OrderItem> lines = new ArrayList<>();
        for (CartItem cart : selected) {
            Product product = checkout.deductStock(cart.getProductId(), cart.getQuantity());
            if (shopId != null && !shopId.equals(product.getShopId())) {
                throw new BusinessException(HttpStatus.CONFLICT, "一个订单只能包含同一家店铺的商品");
            }
            shopId = product.getShopId();
            OrderItem line = new OrderItem();
            line.setProductId(product.getId());
            line.setProductName(product.getProductName());
            line.setUnitPrice(product.getPrice());
            line.setQuantity(cart.getQuantity());
            line.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            productAmount = productAmount.add(line.getSubtotal());
            lines.add(line);
        }
        Shop shop = shops.selectOne(Wrappers.<Shop>lambdaQuery().eq(Shop::getId, shopId).last("FOR UPDATE"));
        if (shop == null) throw new BusinessException(HttpStatus.NOT_FOUND, "店铺不存在");
        if (!Integer.valueOf(1).equals(shop.getBusinessStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "店铺未营业");
        }
        if (productAmount.compareTo(shop.getStartPrice()) < 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "商品金额未达到起送价");
        }
        BigDecimal total = productAmount.add(shop.getDeliveryPrice());
        BigDecimal maximum = new BigDecimal("99999999.99");
        if (productAmount.compareTo(maximum) > 0 || total.compareTo(maximum) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "订单金额超出支持范围");
        }

        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setShopId(shopId);
        order.setAddressId(address.getId());
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setDeliveryAddress(address.getAddressDetail());
        order.setProductAmount(productAmount);
        order.setDeliveryFee(shop.getDeliveryPrice());
        order.setTotalAmount(total);
        order.setOrderStatus(0);
        order.setRemark(request.remark());
        order.setCreatedAt(LocalDateTime.now(ZoneOffset.ofHours(8)));
        orders.insert(order);
        for (OrderItem line : lines) {
            line.setOrderId(order.getId());
            items.insert(line);
        }
        checkout.clearItems(userId, request.cartItemIds());
        return get(userId, order.getId());
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
