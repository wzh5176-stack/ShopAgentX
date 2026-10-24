USE shopagent;

SELECT DATABASE(), VERSION(), CURRENT_USER(), @@hostname, @@port;
SHOW TABLES LIKE 'product_inventory';

CREATE TABLE product_inventory (
                                   product_id BIGINT NOT NULL,
                                   available_quantity INT NOT NULL DEFAULT 0,
                                   reserved_quantity INT NOT NULL DEFAULT 0,
                                   updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP,

                                   PRIMARY KEY (product_id),

                                   CONSTRAINT fk_product_inventory_product
                                       FOREIGN KEY (product_id) REFERENCES products (id),

                                   CONSTRAINT chk_product_inventory_available
                                       CHECK (available_quantity >= 0),

                                   CONSTRAINT chk_product_inventory_reserved
                                       CHECK (reserved_quantity >= 0)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

INSERT INTO product_inventory (
    product_id, available_quantity, reserved_quantity
)
SELECT id,
       CASE WHEN status = 1 AND deleted = 0 THEN 20 ELSE 0 END,
       0
FROM products;

SELECT i.product_id, p.title, p.status, p.deleted,
       i.available_quantity, i.reserved_quantity
FROM product_inventory i
         JOIN products p ON p.id = i.product_id
ORDER BY i.product_id;