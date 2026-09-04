INSERT INTO shop (id) VALUES (1);

INSERT INTO product_category (
    id,
    shop_id,
    category_name,
    sort_order,
    status
) VALUES
    (1, 1, 'Main Food', 1, 1),
    (2, 1, 'Drinks', 2, 1);

ALTER TABLE product_category ALTER COLUMN id RESTART WITH 3;
