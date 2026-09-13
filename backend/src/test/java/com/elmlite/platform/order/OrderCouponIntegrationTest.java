package com.elmlite.platform.order;

import com.elmlite.platform.service.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:order_coupon;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@Sql({"/db/h2/order-api-schema.sql","/db/h2/order-api-data.sql","/db/h2/order-coupon-schema.sql","/db/h2/order-coupon-data.sql"})
class OrderCouponIntegrationTest {
 @Autowired MockMvc mvc; @Autowired JwtTokenService tokens; @Autowired JdbcTemplate jdbc;

 @Test void consumesCouponAndPersistsDiscountSnapshot() throws Exception {
  mvc.perform(post("/api/v1/orders").header("Authorization",user()).contentType(MediaType.APPLICATION_JSON)
   .content("{\"addressId\":1,\"cartItemIds\":[1],\"userCouponId\":1}"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.data.productAmountCent").value(3600))
   .andExpect(jsonPath("$.data.discountAmountCent").value(500)).andExpect(jsonPath("$.data.totalAmountCent").value(3400));
  assertEquals(1,jdbc.queryForObject("SELECT status FROM user_coupon WHERE id=1",Integer.class));
  assertEquals(1L,jdbc.queryForObject("SELECT user_coupon_id FROM orders",Long.class));
  assertEquals("5.00",jdbc.queryForObject("SELECT discount_amount FROM orders",java.math.BigDecimal.class).toPlainString());
 }

 @Test void invalidCouponRollsBackStockCartAndCoupon() throws Exception {
  mvc.perform(post("/api/v1/orders").header("Authorization",user()).contentType(MediaType.APPLICATION_JSON)
   .content("{\"addressId\":1,\"cartItemIds\":[1],\"userCouponId\":2}"))
   .andExpect(status().isConflict());
  assertEquals(10,jdbc.queryForObject("SELECT stock FROM product WHERE id=1",Integer.class));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM cart_item WHERE id=1",Integer.class));
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM orders",Integer.class));
 }

 @Test void cancellationReturnsCouponOnlyOnce() throws Exception {
  var result=mvc.perform(post("/api/v1/orders").header("Authorization",user()).contentType(MediaType.APPLICATION_JSON)
   .content("{\"addressId\":1,\"cartItemIds\":[1],\"userCouponId\":1}")).andReturn();
  long id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
  for(int i=0;i<2;i++) mvc.perform(post("/api/v1/orders/"+id+"/cancel").header("Authorization",user())).andExpect(status().isOk());
  assertEquals(0,jdbc.queryForObject("SELECT status FROM user_coupon WHERE id=1",Integer.class));
 }
 private String user(){return "Bearer "+tokens.issue(1,JwtTokenService.AccountType.USER);}
}
