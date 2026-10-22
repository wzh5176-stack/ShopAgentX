USE shopagent;

CREATE TABLE app_users (
                           id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户编号',
                           username VARCHAR(32) NOT NULL COMMENT '登录用户名',
                           password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希，非明文',
                           status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常，0禁用',
                           created_at DATETIME NOT NULL
                                                   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           updated_at DATETIME NOT NULL
                                                   DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

                           PRIMARY KEY (id),
                           UNIQUE KEY uk_app_users_username (username),

                           CONSTRAINT chk_app_users_username
                               CHECK (CHAR_LENGTH(TRIM(username)) > 0),

                           CONSTRAINT chk_app_users_status
                               CHECK (status IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
    COMMENT='ShopAgent 普通用户账号';

SHOW CREATE TABLE app_users;