-- One-time migration for the existing ShopAgent products table.
-- Run only after confirming that the deleted column does not exist.
-- Do not rerun the initial schema or seed scripts.
ALTER TABLE shopagent.products
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0
        COMMENT '0=not deleted, 1=deleted',
    ADD CONSTRAINT chk_products_deleted CHECK (deleted IN (0, 1));
