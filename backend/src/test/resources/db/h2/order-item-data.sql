INSERT INTO orders (id) VALUES (1);

INSERT INTO product (id) VALUES
    (1),
    (2);

INSERT INTO order_item (
    id,
    order_id,
    product_id,
    product_name,
    unit_price,
    quantity,
    subtotal
) VALUES (
    1,
    1,
    1,
    'Beef Rice',
    18.00,
    2,
    36.00
);

ALTER TABLE order_item ALTER COLUMN id RESTART WITH 2;
