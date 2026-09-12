INSERT INTO rider(id,username,password_hash,display_name,status) VALUES
 (1,'rider_one','unused','骑手一',1),(2,'rider_two','unused','骑手二',1),(3,'rider_off','unused','停用骑手',0);
INSERT INTO orders(id,order_no,user_id,shop_id,address_id,receiver_name,receiver_phone,delivery_address,
 product_amount,delivery_fee,total_amount,order_status,created_at,rider_id) VALUES
 (11,'RIDER11',1,1,1,'张同学','19900000001','一号宿舍',18,3,21,2,'2026-09-12 10:00:00',NULL),
 (12,'RIDER12',1,1,1,'李同学','19900000002','二号宿舍',18,3,21,2,'2026-09-12 09:00:00',1),
 (13,'RIDER13',1,1,1,'王同学','19900000003','三号宿舍',18,3,21,1,'2026-09-12 08:00:00',NULL),
 (14,'RIDER14',1,1,1,'赵同学','19900000004','四号宿舍',18,3,21,3,'2026-09-12 07:00:00',1),
 (15,'RIDER15',1,1,1,'钱同学','19900000005','五号宿舍',18,3,21,4,'2026-09-12 06:00:00',1);
INSERT INTO order_item(order_id,product_id,product_name,unit_price,quantity,subtotal)
 SELECT id,1,'测试饭',18,1,18 FROM orders WHERE id BETWEEN 11 AND 15;
