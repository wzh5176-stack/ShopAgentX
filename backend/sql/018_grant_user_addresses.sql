USE shopagent;

GRANT SELECT, INSERT, UPDATE, DELETE
    ON shopagent.user_addresses
    TO 'shopagent_app'@'127.0.0.1';

SHOW GRANTS FOR 'shopagent_app'@'127.0.0.1';