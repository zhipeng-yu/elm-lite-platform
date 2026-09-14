INSERT INTO shop (id) VALUES (1);

INSERT INTO product_category (
    id, shop_id, category_name, sort_order, status
) VALUES
    (1, 1, 'Main Food', 1, 1),
    (2, 1, 'Drinks', 2, 1),
    (3, 1, 'Disabled', 3, 0);

INSERT INTO product (
    id, shop_id, category_id, product_name,
    description, image_url, price, stock, status
) VALUES
    (1, 1, 1, 'Beef Rice',
     'Main food product', NULL, 18.00, 100, 1),

    (2, 1, 2, 'Lemon Water',
     'Drink product', NULL, 6.50, 0, 1),

    (3, 1, 1, 'Hidden Product',
     'Off shelf product', NULL, 10.00, 20, 0);

INSERT INTO product_detail_image (
    id, product_id, image_url, sort_order
) VALUES
    (1, 1, 'https://example.com/beef-detail-2.jpg', 1),
    (2, 1, '/images/products/beef-detail-1.jpg', 0);

INSERT INTO cart_item (id, user_id, product_id, quantity)
VALUES (1, 10, 1, 4), (2, 20, 1, 3);
