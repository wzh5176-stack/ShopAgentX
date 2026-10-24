-- Windows development only; verify target before execution. No real funds.
USE shopagent;
SELECT DATABASE(), VERSION(), CURRENT_USER(), @@hostname, @@port;
SHOW TABLES LIKE 'mock_payment_receipts';

CREATE TABLE mock_payment_receipts (
    payment_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    outcome VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (payment_id),
    CONSTRAINT fk_mock_payment_receipts_payment
        FOREIGN KEY (payment_id) REFERENCES payment_attempts (id),
    CONSTRAINT fk_mock_payment_receipts_order
        FOREIGN KEY (order_id) REFERENCES shop_orders (id),
    CONSTRAINT chk_mock_payment_receipts_amount CHECK (amount > 0),
    CONSTRAINT chk_mock_payment_receipts_outcome CHECK (outcome IN ('SUCCEEDED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

SHOW CREATE TABLE mock_payment_receipts;
