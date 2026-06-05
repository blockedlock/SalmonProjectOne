CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    name VARCHAR(500),
    current_price NUMERIC(10,2),
    last_checked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE TABLE price_history (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    price NUMERIC(10, 2) NOT NULL,
    recorded_at TIMESTAMP DEFAULT NOW()
);