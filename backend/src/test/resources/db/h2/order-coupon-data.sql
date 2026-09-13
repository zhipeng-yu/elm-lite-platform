INSERT INTO coupon(id,shop_id,name,threshold_amount,discount_amount,starts_at,expires_at,enabled) VALUES
 (1,1,'满20减5',20,5,'2026-01-01 00:00:00','2099-01-01 00:00:00',1),
 (2,2,'跨店券',0,2,'2026-01-01 00:00:00','2099-01-01 00:00:00',1);
INSERT INTO user_coupon(id,user_id,coupon_id,status) VALUES (1,1,1,0),(2,1,2,0);
