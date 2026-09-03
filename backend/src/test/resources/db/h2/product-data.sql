INSERT INTO shop (id) VALUES (1);

INSERT INTO product_category (
    id,
    shop_id
) VALUES
    (1, 1),
    (2, 1);

INSERT INTO product (
    id,
    shop_id,
    category_id,
    product_name,
    description,
    price,
    stock,
    status
) VALUES
    (1, 1, 1, 'Beef Rice', 'Main food test product', 18.00, 100, 1),
    (2, 1, 2, 'Lemon Water', 'Drink test product', 6.50, 100, 1);

ALTER TABLE product ALTER COLUMN id RESTART WITH 3;
