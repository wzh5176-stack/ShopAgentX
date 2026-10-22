-- 阶段7购物车基础。由迁移/管理员连接执行；助手尚未执行。
-- MySQL 8.0.34，父表 app_users/products 的 id 均为有符号 BIGINT。
-- 仅创建新表，不修改已有商品、用户或聊天记录。
USE shopagent;

CREATE TABLE cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车条目编号',
    user_id BIGINT NOT NULL COMMENT '所属普通用户编号',
    product_id BIGINT NOT NULL COMMENT '商品编号',
    quantity INT NOT NULL COMMENT '该商品在购物车中的目标总数量，1到99',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_items_user_product (user_id, product_id),
    KEY idx_cart_items_product (product_id),
    CONSTRAINT fk_cart_items_user
        FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT fk_cart_items_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_cart_items_quantity CHECK (quantity BETWEEN 1 AND 99)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT='用户购物车商品条目';

SHOW CREATE TABLE cart_items;
