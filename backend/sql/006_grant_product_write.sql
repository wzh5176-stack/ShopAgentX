-- ShopAgent stage 2b: grant only the table privileges needed by product CRUD.
-- Logical deletion updates deleted, so physical DELETE privilege is unnecessary.
-- This script does not change any password or grant privileges on other tables.
GRANT SELECT, INSERT, UPDATE
ON shopagent.products
TO 'shopagent_app'@'127.0.0.1';
