USE shopagent;

CREATE TABLE user_addresses (
                                id BIGINT NOT NULL AUTO_INCREMENT COMMENT '地址编号',
                                user_id BIGINT NOT NULL COMMENT '所属用户编号',
                                recipient_name VARCHAR(50) NOT NULL COMMENT '收货人',
                                phone VARCHAR(20) NOT NULL COMMENT '联系电话',
                                province VARCHAR(50) NOT NULL COMMENT '省或直辖市',
                                city VARCHAR(50) NOT NULL COMMENT '城市',
                                district VARCHAR(50) NOT NULL COMMENT '区县',
                                detail_address VARCHAR(200) NOT NULL COMMENT '详细地址',
                                is_default TINYINT NOT NULL DEFAULT 0 COMMENT '1默认，0非默认',

                                default_user_id BIGINT GENERATED ALWAYS AS (
                                    CASE WHEN is_default = 1 THEN user_id ELSE NULL END
                                    ) STORED,

                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP,

                                PRIMARY KEY (id),

                                KEY idx_user_addresses_user (user_id),

                                UNIQUE KEY uk_user_addresses_default (default_user_id),

                                CONSTRAINT fk_user_addresses_user
                                    FOREIGN KEY (user_id) REFERENCES app_users (id),

                                CONSTRAINT chk_user_addresses_default
                                    CHECK (is_default IN (0, 1)),

                                CONSTRAINT chk_user_addresses_text
                                    CHECK (
                                        CHAR_LENGTH(TRIM(recipient_name)) > 0
                                            AND CHAR_LENGTH(TRIM(phone)) > 0
                                            AND CHAR_LENGTH(TRIM(province)) > 0
                                            AND CHAR_LENGTH(TRIM(city)) > 0
                                            AND CHAR_LENGTH(TRIM(district)) > 0
                                            AND CHAR_LENGTH(TRIM(detail_address)) > 0
                                        )
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
    COMMENT='用户收货地址';

SHOW CREATE TABLE user_addresses;

SELECT COUNT(*) AS address_count FROM user_addresses;