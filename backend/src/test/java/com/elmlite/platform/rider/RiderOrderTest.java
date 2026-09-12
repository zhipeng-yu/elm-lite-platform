package com.elmlite.platform.rider;

import com.elmlite.platform.service.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:rider_orders;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@Sql({"/db/h2/order-api-schema.sql","/db/h2/order-api-data.sql","/db/h2/rider-order-schema.sql","/db/h2/rider-order-data.sql"})
class RiderOrderTest {
 @Autowired MockMvc mvc;
 @Autowired JwtTokenService tokens;
 private String rider(long id) { return "Bearer " + tokens.issue(id, JwtTokenService.AccountType.RIDER); }

 @Test void discoversAndClaimsWithoutLeakingDeliveryAddress() throws Exception {
  mvc.perform(get("/api/v1/rider/available-orders").header("Authorization",rider(1)))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data[*].id",contains(11)))
   .andExpect(jsonPath("$.data[0].shopName").value("测试店铺"))
   .andExpect(jsonPath("$.data[0].shopAddress").value("校内"))
   .andExpect(jsonPath("$.data[0].items[0].productName").value("测试饭"))
   .andExpect(jsonPath("$.data[0].receiverPhone").doesNotExist());
  mvc.perform(post("/api/v1/rider/orders/11/claim").header("Authorization",rider(1)))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(2));
  mvc.perform(post("/api/v1/rider/orders/11/claim").header("Authorization",rider(1))).andExpect(status().isOk());
  mvc.perform(post("/api/v1/rider/orders/11/claim").header("Authorization",rider(2))).andExpect(status().isConflict());
  mvc.perform(post("/api/v1/rider/orders/13/claim").header("Authorization",rider(1))).andExpect(status().isConflict());
 }

 @Test void listsOwnTasksAndCompletesDeliveryIdempotently() throws Exception {
  mvc.perform(get("/api/v1/rider/orders").header("Authorization",rider(1)))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data[*].id",contains(12,14,15)));
  mvc.perform(get("/api/v1/rider/orders/12").header("Authorization",rider(1)))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.receiverPhone").value("19900000002"));
  mvc.perform(post("/api/v1/rider/orders/12/dispatch").header("Authorization",rider(1)))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(3));
  mvc.perform(post("/api/v1/rider/orders/12/complete").header("Authorization",rider(1)))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(4));
  mvc.perform(post("/api/v1/rider/orders/12/complete").header("Authorization",rider(1))).andExpect(status().isOk());
  mvc.perform(post("/api/v1/rider/orders/14/complete").header("Authorization",rider(2))).andExpect(status().isForbidden());
 }

 @Test void rejectsMissingWrongIdentityAndDisabledRider() throws Exception {
  mvc.perform(get("/api/v1/rider/orders/999").header("Authorization",rider(1))).andExpect(status().isNotFound());
  mvc.perform(get("/api/v1/rider/orders")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/rider/orders").header("Authorization","Bearer "+tokens.issue(1,JwtTokenService.AccountType.USER)))
   .andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/rider/orders").header("Authorization",rider(3))).andExpect(status().isForbidden());
 }

 @Test void onlyOneRiderWinsConcurrentClaim() throws Exception {
  try (var pool=Executors.newFixedThreadPool(2)) {
   var first=pool.submit(()->mvc.perform(post("/api/v1/rider/orders/11/claim").header("Authorization",rider(1))).andReturn().getResponse().getStatus());
   var second=pool.submit(()->mvc.perform(post("/api/v1/rider/orders/11/claim").header("Authorization",rider(2))).andReturn().getResponse().getStatus());
   assert java.util.Set.of(first.get(),second.get()).equals(java.util.Set.of(200,409));
  }
 }
}
