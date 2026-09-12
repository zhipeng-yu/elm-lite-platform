package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.entity.Admin;
import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.entity.Order;
import com.elmlite.platform.entity.OrderItem;
import com.elmlite.platform.entity.Shop;
import com.elmlite.platform.entity.User;
import com.elmlite.platform.service.AdminAuthService;
import com.elmlite.platform.service.AdminService;
import com.elmlite.platform.service.JwtTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminAuthService adminAuthService;
    private final AdminService adminService;

    public AdminController(AdminAuthService adminAuthService, AdminService adminService) {
        this.adminAuthService = adminAuthService;
        this.adminService = adminService;
    }

    @PostMapping("/auth/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AdminAuthService.LoginResult result =
                adminAuthService.login(request.username(), request.password());
        Admin admin = result.admin();
        return ApiResponse.success(new LoginResponse(
                result.accessToken(),
                JwtTokenService.EXPIRES_IN_SECONDS,
                new AdminView(admin.getId(), admin.getUsername())));
    }

    @GetMapping("/users")
    public ApiResponse<List<UserView>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(adminService.listUsers(keyword, status)
                .stream().map(UserView::of).toList());
    }

    @GetMapping("/merchants")
    public ApiResponse<List<MerchantView>> listMerchants(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(adminService.listMerchants(keyword, status)
                .stream().map(MerchantView::of).toList());
    }

    @GetMapping("/shops")
    public ApiResponse<List<ShopView>> listShops() {
        return ApiResponse.success(adminService.listShops()
                .stream().map(ShopView::of).toList());
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderView>> listOrders(
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) Integer orderStatus) {
        return ApiResponse.success(adminService.listOrders(shopId, orderStatus)
                .stream().map(OrderView::of).toList());
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<OrderDetailView> getOrder(@PathVariable long id) {
        Order order = adminService.getOrder(id);
        List<ItemView> items = adminService.listOrderItems(id)
                .stream().map(ItemView::of).toList();
        return ApiResponse.success(OrderDetailView.of(order, items));
    }

    @PatchMapping("/users/{id}")
    public ApiResponse<UserView> updateUserStatus(
            @PathVariable long id,
            @Valid @RequestBody StatusRequest request) {
        return ApiResponse.success(
                UserView.of(adminService.updateUserStatus(id, request.status())));
    }

    @PatchMapping("/merchants/{id}")
    public ApiResponse<MerchantView> updateMerchantStatus(
            @PathVariable long id,
            @Valid @RequestBody StatusRequest request) {
        return ApiResponse.success(
                MerchantView.of(adminService.updateMerchantStatus(id, request.status())));
    }

    private static long toCent(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    public record LoginRequest(
            @NotBlank(message = "管理员账号不能为空") String username,
            @NotBlank(message = "密码不能为空") String password) {
        @Override
        public String toString() {
            return "LoginRequest[REDACTED]";
        }
    }

    public record LoginResponse(
            String accessToken,
            long expiresIn,
            AdminView admin) {
        @Override
        public String toString() {
            return "LoginResponse[REDACTED]";
        }
    }

    public record StatusRequest(
            @NotNull(message = "状态不能为空") Integer status) {
    }

    public record AdminView(Long id, String username) {
    }

    public record UserView(Long id, String username, String nickname, Integer status,
                           LocalDateTime createdAt) {
        static UserView of(User user) {
            return new UserView(user.getId(), user.getUsername(), user.getNickname(),
                    user.getStatus(), user.getCreatedAt());
        }
    }

    public record MerchantView(Long id, String account, String merchantName,
                               String contactName, Integer status, LocalDateTime createdAt) {
        static MerchantView of(Merchant merchant) {
            return new MerchantView(merchant.getId(), merchant.getAccount(),
                    merchant.getMerchantName(), merchant.getContactName(),
                    merchant.getStatus(), merchant.getCreatedAt());
        }
    }

    public record ShopView(Long id, Long merchantId, String shopName,
                           Integer businessStatus, LocalDateTime createdAt) {
        static ShopView of(Shop shop) {
            return new ShopView(shop.getId(), shop.getMerchantId(), shop.getShopName(),
                    shop.getBusinessStatus(), shop.getCreatedAt());
        }
    }

    public record OrderView(Long id, String orderNo, Long userId, Long shopId,
                            Integer orderStatus, long productAmountCent,
                            long deliveryFeeCent, long totalAmountCent,
                            LocalDateTime createdAt) {
        static OrderView of(Order order) {
            return new OrderView(order.getId(), order.getOrderNo(), order.getUserId(),
                    order.getShopId(), order.getOrderStatus(),
                    toCent(order.getProductAmount()), toCent(order.getDeliveryFee()),
                    toCent(order.getTotalAmount()), order.getCreatedAt());
        }
    }

    public record OrderDetailView(Long id, String orderNo, Long userId, Long shopId,
                                  Integer orderStatus, long productAmountCent,
                                  long deliveryFeeCent, long totalAmountCent,
                                  LocalDateTime createdAt, String receiverName,
                                  String receiverPhone, String deliveryAddress,
                                  String remark, List<ItemView> items) {
        static OrderDetailView of(Order order, List<ItemView> items) {
            OrderView view = OrderView.of(order);
            return new OrderDetailView(view.id(), view.orderNo(), view.userId(),
                    view.shopId(), view.orderStatus(), view.productAmountCent(),
                    view.deliveryFeeCent(), view.totalAmountCent(), view.createdAt(),
                    order.getReceiverName(), order.getReceiverPhone(),
                    order.getDeliveryAddress(), order.getRemark(), items);
        }
    }

    public record ItemView(Long productId, String productName, long unitPriceCent,
                           Integer quantity, long subtotalCent) {
        static ItemView of(OrderItem item) {
            return new ItemView(item.getProductId(), item.getProductName(),
                    toCent(item.getUnitPrice()), item.getQuantity(),
                    toCent(item.getSubtotal()));
        }
    }
}
