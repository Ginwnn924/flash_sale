CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    storage VARCHAR(20),
    color VARCHAR(30),
    price BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS flash_sale_items (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    flash_price BIGINT NOT NULL,
    total_stock INT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    flash_sale_item_id BIGINT NOT NULL REFERENCES flash_sale_items(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    product_name VARCHAR(150) NOT NULL,
    price BIGINT NOT NULL,
    status SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);


INSERT INTO products (brand, model, storage, color, price)
VALUES
    ('Apple', 'iPhone 15', '128GB', 'Black', 22990000),
    ('Apple', 'iPhone 15 Pro', '256GB', 'Natural Titanium', 28990000),
    ('Samsung', 'Galaxy S24 Ultra', '256GB', 'Titanium Gray', 25990000),
    ('Samsung', 'Galaxy Z Flip5', '256GB', 'Lavender', 19990000),
    ('Xiaomi', 'Redmi Note 13 Pro', '128GB', 'Midnight Black', 6990000)
ON CONFLICT DO NOTHING;

INSERT INTO flash_sale_items (product_id, flash_price, total_stock, start_time, end_time)
VALUES
    (1, 15990000, 100, NOW(), NOW() + INTERVAL '24 hours'),
    (2, 21990000, 50, NOW(), NOW() + INTERVAL '24 hours'),
    (3, 18990000, 80, NOW(), NOW() + INTERVAL '24 hours'),
    (4, 13990000, 60, NOW(), NOW() + INTERVAL '24 hours'),
    (5, 4490000, 200, NOW(), NOW() + INTERVAL '24 hours')
ON CONFLICT DO NOTHING;


CREATE INDEX IF NOT EXISTS idx_orders_user ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_flashsale ON orders(flash_sale_item_id);
CREATE UNIQUE INDEX IF NOT EXISTS uniq_user_flashsale ON orders(user_id, flash_sale_item_id);


