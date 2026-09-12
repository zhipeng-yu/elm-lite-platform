package com.elmlite.platform.admin;

import com.elmlite.platform.entity.Admin;
import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.entity.Order;
import com.elmlite.platform.entity.OrderItem;
import com.elmlite.platform.entity.Shop;
import com.elmlite.platform.entity.User;
import com.elmlite.platform.mapper.AdminMapper;
import com.elmlite.platform.mapper.MerchantMapper;
import com.elmlite.platform.mapper.OrderItemMapper;
import com.elmlite.platform.mapper.OrderMapper;
import com.elmlite.platform.mapper.ShopMapper;
import com.elmlite.platform.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        scripts = "/db/h2/admin-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class AdminQueryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long userAId;
    private Long merchantAId;
    private Long shopId;
    private Long orderAId;

    @BeforeEach
    void prepareData() throws Exception {
        Admin admin = new Admin();
        admin.setUsername("root_admin");
        admin.setPasswordHash(passwordEncoder.encode("admin_password_123"));
        admin.setStatus(1);
        adminMapper.insert(admin);

        User userA = new User();
        userA.setUsername("user_a");
        userA.setNickname("用户甲");
        userA.setPhone("19900000001");
        userA.setPasswordHash(passwordEncoder.encode("user_password_123"));
        userA.setStatus(1);
        userMapper.insert(userA);
        userAId = userA.getId();

        User userB = new User();
        userB.setUsername("user_b");
        userB.setNickname("用户乙");
        userB.setPasswordHash(passwordEncoder.encode("user_password_123"));
        userB.setStatus(0);
        userMapper.insert(userB);

        Merchant merchantA = new Merchant();
        merchantA.setAccount("merchant_a");
        merchantA.setMerchantName("商家甲");
        merchantA.setContactName("甲联系人");
        merchantA.setContactPhone("19900000002");
        merchantA.setPasswordHash(passwordEncoder.encode("merchant_password_123"));
        merchantA.setStatus(1);
        merchantMapper.insert(merchantA);
        merchantAId = merchantA.getId();

        Merchant merchantB = new Merchant();
        merchantB.setAccount("merchant_b");
        merchantB.setMerchantName("商家乙");
        merchantB.setContactName("乙联系人");
        merchantB.setContactPhone("19900000003");
        merchantB.setPasswordHash(passwordEncoder.encode("merchant_password_123"));
        merchantB.setStatus(0);
        merchantMapper.insert(merchantB);

        Shop shop = new Shop();
        shop.setMerchantId(merchantAId);
        shop.setShopName("校园美食店");
        shop.setDescription("演示店铺");
        shop.setAddress("测试地址1号");
        shop.setStartPrice(new BigDecimal("15.00"));
        shop.setDeliveryPrice(new BigDecimal("3.00"));
        shop.setBusinessStatus(1);
        shopMapper.insert(shop);
        shopId = shop.getId();

        Order orderA = new Order();
        orderA.setOrderNo("ADMIN202609110001");
        orderA.setUserId(userAId);
        orderA.setShopId(shopId);
        orderA.setReceiverName("用户甲");
        orderA.setReceiverPhone("19900000001");
        orderA.setDeliveryAddress("测试校区1号宿舍楼");
        orderA.setProductAmount(new BigDecimal("36.00"));
        orderA.setDeliveryFee(new BigDecimal("3.00"));
        orderA.setTotalAmount(new BigDecimal("39.00"));
        orderA.setOrderStatus(0);
        orderA.setRemark("少辣");
        orderMapper.insert(orderA);
        orderAId = orderA.getId();

        OrderItem item = new OrderItem();
        item.setOrderId(orderAId);
        item.setProductId(1L);
        item.setProductName("牛肉盖饭");
        item.setUnitPrice(new BigDecimal("18.00"));
        item.setQuantity(2);
        item.setSubtotal(new BigDecimal("36.00"));
        orderItemMapper.insert(item);

        Order orderB = new Order();
        orderB.setOrderNo("ADMIN202609110002");
        orderB.setUserId(userAId);
        orderB.setShopId(shopId);
        orderB.setReceiverName("用户甲");
        orderB.setReceiverPhone("19900000001");
        orderB.setDeliveryAddress("测试校区1号宿舍楼");
        orderB.setProductAmount(new BigDecimal("6.50"));
        orderB.setDeliveryFee(new BigDecimal("3.00"));
        orderB.setTotalAmount(new BigDecimal("9.50"));
        orderB.setOrderStatus(2);
        orderMapper.insert(orderB);
    }

    private String adminToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "root_admin", "password": "admin_password_123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return response.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void listsUsersWithoutSensitiveFieldsAndSupportsFilters() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].username").exists())
                .andExpect(jsonPath("$.data[0].nickname").exists())
                .andExpect(jsonPath("$.data[0].status").exists())
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data[0].phone").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "user_a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].username").value("user_a"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].username").value("user_b"));
    }

    @Test
    void listsMerchantsWithoutSensitiveFieldsAndSupportsFilters() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/api/v1/admin/merchants")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].account").exists())
                .andExpect(jsonPath("$.data[0].merchantName").exists())
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data[0].contactPhone").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/merchants")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "商家乙"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].account").value("merchant_b"));

        mockMvc.perform(get("/api/v1/admin/merchants")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].account").value("merchant_a"));
    }

    @Test
    void listsShops() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/api/v1/admin/shops")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].shopName").value("校园美食店"))
                .andExpect(jsonPath("$.data[0].businessStatus").value(1));
    }

    @Test
    void listsOrdersWithFiltersAndRejectsInvalidStatus() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/api/v1/admin/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].orderStatus").exists())
                .andExpect(jsonPath("$.data[0].receiverPhone").doesNotExist())
                .andExpect(jsonPath("$.data[0].deliveryAddress").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/orders")
                        .header("Authorization", "Bearer " + token)
                        .param("orderStatus", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].orderNo").value("ADMIN202609110002"));

        mockMvc.perform(get("/api/v1/admin/orders")
                        .header("Authorization", "Bearer " + token)
                        .param("shopId", shopId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/v1/admin/orders")
                        .header("Authorization", "Bearer " + token)
                        .param("orderStatus", "9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void returnsOrderSnapshotAnd404ForUnknownOrder() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/api/v1/admin/orders/" + orderAId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").value("ADMIN202609110001"))
                .andExpect(jsonPath("$.data.receiverName").value("用户甲"))
                .andExpect(jsonPath("$.data.deliveryAddress").value("测试校区1号宿舍楼"))
                .andExpect(jsonPath("$.data.productAmountCent").value(3600))
                .andExpect(jsonPath("$.data.deliveryFeeCent").value(300))
                .andExpect(jsonPath("$.data.totalAmountCent").value(3900))
                .andExpect(jsonPath("$.data.remark").value("少辣"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].productName").value("牛肉盖饭"))
                .andExpect(jsonPath("$.data.items[0].unitPriceCent").value(1800))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].subtotalCent").value(3600));

        mockMvc.perform(get("/api/v1/admin/orders/999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
