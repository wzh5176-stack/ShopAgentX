USE shopagent;

GRANT SELECT, INSERT, UPDATE
    ON shopagent.shop_orders
    TO 'shopagent_app'@'127.0.0.1';

GRANT SELECT, INSERT
    ON shopagent.order_items
    TO 'shopagent_app'@'127.0.0.1';

SHOW GRANTS FOR 'shopagent_app'@'127.0.0.1';
