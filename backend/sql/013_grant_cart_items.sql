-- 在012建表成功后，由迁移/管理员连接执行；助手尚未执行。
-- 权限仅作用于新购物车表，不扩大商品/用户/聊天表权限。
GRANT SELECT, INSERT, UPDATE, DELETE
    ON shopagent.cart_items
    TO 'shopagent_app'@'127.0.0.1';

SHOW GRANTS FOR 'shopagent_app'@'127.0.0.1';
