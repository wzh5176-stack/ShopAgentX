-- 要求 MySQL 8.0.16+，已在本机 8.0.34 执行并核对结构。
-- 仅用于首次创建；同名库或表已存在时会报错。
-- 本脚本只创建库和表，不包含凭据或样例数据。

CREATE DATABASE shopagent
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE shopagent;

CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品编号',
    title VARCHAR(200) NOT NULL COMMENT '商品名称',
    brand VARCHAR(100) NOT NULL COMMENT '品牌',
    price DECIMAL(10, 2) NOT NULL COMMENT '价格，人民币元',
    image_url VARCHAR(500) NULL COMMENT '主图地址，无图时为 NULL',
    description TEXT NOT NULL COMMENT '商品介绍，可为空字符串',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1 上架，0 下架',
    PRIMARY KEY (id),
    CONSTRAINT chk_products_title CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT chk_products_brand CHECK (CHAR_LENGTH(TRIM(brand)) > 0),
    CONSTRAINT chk_products_price CHECK (price >= 0),
    CONSTRAINT chk_products_status CHECK (status IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT='ShopAgent 商品表';
