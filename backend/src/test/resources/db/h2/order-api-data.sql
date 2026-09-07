INSERT INTO users(id,username,password_hash,nickname,status) VALUES
    (1,'order_owner','unused_test_hash','订单用户',1),
    (2,'order_other','unused_test_hash','其他用户',1),
    (3,'order_disabled','unused_test_hash','禁用用户',0);
INSERT INTO merchant(id,account,password_hash,merchant_name,contact_name,contact_phone) VALUES
    (1,'order_merchant','unused_test_hash','测试商家','测试联系人','19900000000');
INSERT INTO shop(id,merchant_id,shop_name,address,start_price,delivery_price,business_status) VALUES
    (1,1,'测试店铺','校内',15.00,3.00,1), (2,1,'另一店铺','校内',0,0,1);
INSERT INTO product_category(id,shop_id,category_name) VALUES (1,1,'主食'),(2,2,'饮品');
INSERT INTO product(id,shop_id,category_id,product_name,price,stock,status) VALUES
    (1,1,1,'测试饭',18.00,10,1), (2,1,1,'测试面',12.50,5,1), (3,2,2,'测试饮品',5.00,10,1);
INSERT INTO delivery_address(id,user_id,receiver_name,receiver_phone,address_detail) VALUES
    (1,1,'测试收货人','19900000001','测试一号楼'), (2,2,'其他收货人','19900000002','测试二号楼');
INSERT INTO cart_item(id,user_id,product_id,quantity) VALUES (1,1,1,2),(2,1,2,1),(3,2,1,1);
