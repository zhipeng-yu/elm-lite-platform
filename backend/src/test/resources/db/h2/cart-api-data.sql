INSERT INTO users (
    id, username, password_hash, nickname, status
) VALUES
    (1, 'cart_user_1', 'unused_test_hash', 'User One', 1),
    (2, 'cart_user_2', 'unused_test_hash', 'User Two', 1);

INSERT INTO product (
    id, shop_id, category_id, product_name,
    description, image_url, price, stock, status
) VALUES
    (1, 10, 1, 'Beef Rice',
     'Beef rice', 'https://example.com/beef.jpg',
     18.00, 10, 1),

    (2, 10, 1, 'Cola',
     'Cola', 'https://example.com/cola.jpg',
     6.50, 5, 1);

INSERT INTO cart_item (
    id, user_id, product_id, quantity
) VALUES
    (1, 1, 1, 2),
    (2, 2, 2, 1);

ALTER TABLE cart_item ALTER COLUMN id RESTART WITH 3;