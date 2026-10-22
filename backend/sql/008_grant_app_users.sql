SELECT VERSION() AS mysql_version;

GRANT SELECT, INSERT
    ON shopagent.app_users
    TO 'shopagent_app'@'127.0.0.1';

SHOW GRANTS FOR 'shopagent_app'@'127.0.0.1';