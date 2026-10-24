USE shopagent;

SELECT DATABASE(), VERSION(), CURRENT_USER(), @@hostname, @@port;
SHOW TABLES LIKE 'shop_orders';
SHOW TABLES LIKE 'order_items';

CREATE TABLE shop_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    request_key VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_hash VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount DECIMAL(14, 2) NOT NULL,

    recipient_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    province VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(50) NOT NULL,
    detail_address VARCHAR(200) NOT NULL,

    expires_at DATETIME(3) NOT NULL,
    paid_at DATETIME(3) NULL,
    closed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_shop_orders_user_request (user_id, request_key),
    KEY idx_shop_orders_user_id (user_id, id),
    KEY idx_shop_orders_expiry (status, expires_at, id),

    CONSTRAINT fk_shop_orders_user
        FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT chk_shop_orders_request
        CHECK (CHAR_LENGTH(request_key) = 36
            AND CHAR_LENGTH(request_hash) = 64),
    CONSTRAINT chk_shop_orders_amount
        CHECK (total_amount > 0),
    CONSTRAINT chk_shop_orders_status
        CHECK (status IN (
            'PENDING_PAYMENT', 'PAID', 'CANCELLED', 'EXPIRED'
        )),
    CONSTRAINT chk_shop_orders_address
        CHECK (
            CHAR_LENGTH(TRIM(recipient_name)) > 0
            AND CHAR_LENGTH(TRIM(phone)) > 0
            AND CHAR_LENGTH(TRIM(province)) > 0
            AND CHAR_LENGTH(TRIM(city)) > 0
            AND CHAR_LENGTH(TRIM(district)) > 0
            AND CHAR_LENGTH(TRIM(detail_address)) > 0
        ),
    CONSTRAINT chk_shop_orders_expiry
        CHECK (expires_at > created_at),
    CONSTRAINT chk_shop_orders_state_time
        CHECK (
            (status = 'PENDING_PAYMENT'
                AND paid_at IS NULL AND closed_at IS NULL)
            OR
            (status = 'PAID'
                AND paid_at IS NOT NULL AND paid_at >= created_at
                AND closed_at IS NULL)
            OR
            (status IN ('CANCELLED', 'EXPIRED')
                AND paid_at IS NULL
                AND closed_at IS NOT NULL AND closed_at >= created_at)
        )
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    image_url VARCHAR(500) NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    line_amount DECIMAL(14, 2) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_order_items_order_product (order_id, product_id),
    KEY idx_order_items_product (product_id),

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES shop_orders (id),
    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_order_items_text
        CHECK (CHAR_LENGTH(TRIM(title)) > 0
            AND CHAR_LENGTH(TRIM(brand)) > 0),
    CONSTRAINT chk_order_items_price
        CHECK (unit_price >= 0),
    CONSTRAINT chk_order_items_quantity
        CHECK (quantity BETWEEN 1 AND 99),
    CONSTRAINT chk_order_items_amount
        CHECK (line_amount = unit_price * quantity)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

SHOW CREATE TABLE shop_orders;
SHOW CREATE TABLE order_items;
