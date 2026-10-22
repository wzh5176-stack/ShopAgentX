-- 本机学习环境的项目账号；仅首次创建时执行。
-- RANDOM PASSWORD 的查询结果包含密码，不应保存到日志或提交 Git。
-- 执行器捕获密码并写入已被 Git 忽略的 backend/config/application-local.yml。
CREATE USER 'shopagent_app'@'127.0.0.1'
    IDENTIFIED BY RANDOM PASSWORD;

GRANT SELECT ON shopagent.products
    TO 'shopagent_app'@'127.0.0.1';
