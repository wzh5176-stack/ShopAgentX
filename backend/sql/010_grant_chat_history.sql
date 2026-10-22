GRANT SELECT, INSERT, UPDATE
    ON shopagent.chat_conversations
    TO 'shopagent_app'@'127.0.0.1';

GRANT SELECT, INSERT
    ON shopagent.chat_messages
    TO 'shopagent_app'@'127.0.0.1';

SHOW GRANTS FOR 'shopagent_app'@'127.0.0.1';