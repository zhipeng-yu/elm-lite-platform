INSERT INTO users (id) VALUES (1);

INSERT INTO product (id) VALUES
    (1),
    (2);

INSERT INTO cart_item (
    id,
    user_id,
    product_id,
    quantity
) VALUES (
    1,
    1,
    2,
    1
);

ALTER TABLE cart_item ALTER COLUMN id RESTART WITH 2;
