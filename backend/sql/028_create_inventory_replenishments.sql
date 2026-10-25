CREATE TABLE inventory_replenishments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    actor VARCHAR(100) COLLATE utf8mb4_bin NOT NULL,
    request_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    quantity INT NOT NULL,
    reason VARCHAR(200) NOT NULL,
    available_quantity INT NOT NULL,
    reserved_quantity INT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_replenishment_request (actor, request_key),
    KEY idx_inventory_replenishment_product (product_id, id),
    CONSTRAINT fk_inventory_replenishment_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_inventory_replenishment_quantity CHECK (quantity BETWEEN 1 AND 1000000),
    CONSTRAINT chk_inventory_replenishment_snapshot CHECK (available_quantity >= 0 AND reserved_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
