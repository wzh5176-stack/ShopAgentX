USE shopagent;
GRANT SELECT, INSERT ON shopagent.mock_payment_receipts
    TO 'shopagent_app'@'127.0.0.1';
SHOW GRANTS FOR 'shopagent_app'@'127.0.0.1';
