USE shopagent;

GRANT SELECT, INSERT, UPDATE
    ON shopagent.product_inventory
    TO 'shopagent_app'@'127.0.0.1';

SHOW GRANTS FOR 'shopagent_app'@'127.0.0.1';