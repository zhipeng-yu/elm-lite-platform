package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.Order;
import com.elmlite.platform.entity.OrderItem;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RiderOrderService {
 private final RiderMapper riders; private final OrderMapper orders; private final OrderItemMapper items; private final ShopMapper shops;
 public RiderOrderService(RiderMapper riders, OrderMapper orders, OrderItemMapper items, ShopMapper shops) {
  this.riders=riders; this.orders=orders; this.items=items; this.shops=shops;
 }
 public List<Available> available(long riderId) { active(riderId); return orders.selectList(Wrappers.<Order>lambdaQuery()
  .eq(Order::getOrderStatus,2).isNull(Order::getRiderId).orderByDesc(Order::getCreatedAt,Order::getId)).stream().map(this::available).toList(); }
 public List<OrderService.Summary> mine(long riderId) { active(riderId); return orders.selectList(Wrappers.<Order>lambdaQuery()
  .eq(Order::getRiderId,riderId).orderByDesc(Order::getCreatedAt,Order::getId)).stream().map(OrderService.Summary::from).toList(); }
 public OrderService.Detail get(long riderId,long id) { active(riderId); Order o=orders.selectById(id); requireOwned(riderId,o); return detail(o); }
 @Transactional public OrderService.Detail claim(long riderId,long id) {
  active(riderId); Order o=orders.lockById(id); if(o==null) throw error(HttpStatus.NOT_FOUND,"订单不存在");
  if(Long.valueOf(riderId).equals(o.getRiderId())) return detail(o);
  if(o.getRiderId()!=null || !Integer.valueOf(2).equals(o.getOrderStatus()) || orders.claim(id,riderId)!=1) throw error(HttpStatus.CONFLICT,"订单不可领取");
  o.setRiderId(riderId); return detail(o);
 }
 @Transactional public OrderService.Detail dispatch(long riderId,long id) { return change(riderId,id,2,3,false); }
 @Transactional public OrderService.Detail complete(long riderId,long id) { return change(riderId,id,3,4,true); }
 private OrderService.Detail change(long riderId,long id,int expected,int next,boolean idempotent) {
  active(riderId); Order o=orders.lockById(id); requireOwned(riderId,o);
  if(idempotent && Integer.valueOf(next).equals(o.getOrderStatus())) return detail(o);
  if(!Integer.valueOf(expected).equals(o.getOrderStatus()) || orders.updateStatus(id,expected,next)!=1) throw error(HttpStatus.CONFLICT,"订单状态不允许此操作");
  o.setOrderStatus(next); return detail(o);
 }
 private void requireOwned(long riderId,Order o) { if(o==null) throw error(HttpStatus.NOT_FOUND,"订单不存在"); if(!Long.valueOf(riderId).equals(o.getRiderId())) throw error(HttpStatus.FORBIDDEN,"无权操作该任务"); }
 private OrderService.Detail detail(Order o) { return OrderService.Detail.from(o,lines(o.getId())); }
 private List<OrderService.Item> lines(long id) { return items.selectList(Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId,id).orderByAsc(OrderItem::getId)).stream().map(OrderService.Item::from).toList(); }
 private Available available(Order o) { var s=shops.selectById(o.getShopId()); return new Available(o.getId(),o.getOrderNo(),o.getShopId(),s.getShopName(),s.getAddress(),o.getOrderStatus(),o.getTotalAmount().movePointRight(2).longValueExact(),o.getCreatedAt().atOffset(ZoneOffset.ofHours(8)),lines(o.getId())); }
 private void active(long id) { var r=riders.selectById(id); if(r==null || !Integer.valueOf(1).equals(r.getStatus())) throw error(HttpStatus.FORBIDDEN,"骑手账号不可用"); }
 private BusinessException error(HttpStatus status,String message) { return new BusinessException(status,message); }
 public record Available(Long id,String orderNo,Long shopId,String shopName,String shopAddress,Integer orderStatus,Long totalAmountCent,OffsetDateTime createdAt,List<OrderService.Item> items) {}
}
