USE shopagent;

GRANT SELECT, INSERT, UPDATE
    ON shopagent.payment_attempts
    TO 'shopagent_app'@'127.0.0.1';

SHOW GRANTS FOR 'shopagent_app'@'127.0.0.1';