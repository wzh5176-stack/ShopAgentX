-- ShopAgent schema-only baseline, derived from reviewed 001-027 history.
-- EMPTY DATABASE ONLY. No CREATE DATABASE, USE, grants, accounts or seed rows.
-- Existing shopagent databases must NOT replay this file. See backend/db/README.md.

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

ALTER TABLE products
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0
        COMMENT '0=not deleted, 1=deleted',
    ADD CONSTRAINT chk_products_deleted CHECK (deleted IN (0, 1));

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

CREATE TABLE chat_conversations (
                                    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '聊天会话编号',
                                    user_id BIGINT NOT NULL COMMENT '所属普通用户编号',
                                    title VARCHAR(100) NOT NULL DEFAULT '新对话' COMMENT '聊天标题',
                                    created_at DATETIME NOT NULL
                                                                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    updated_at DATETIME NOT NULL
                                                                DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

                                    PRIMARY KEY (id),
                                    KEY idx_chat_conversations_user_updated (user_id, updated_at, id),

                                    CONSTRAINT fk_chat_conversations_user
                                        FOREIGN KEY (user_id) REFERENCES app_users (id),
                                    CONSTRAINT chk_chat_conversations_title
                                        CHECK (CHAR_LENGTH(TRIM(title)) > 0)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
    COMMENT='用户的聊天会话';

CREATE TABLE chat_messages (
                               id BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息编号',
                               conversation_id BIGINT NOT NULL COMMENT '所属聊天会话编号',
                               role VARCHAR(16) NOT NULL COMMENT 'user用户消息，assistant模型回复',
                               content TEXT NOT NULL COMMENT '消息正文',
                               created_at DATETIME NOT NULL
                                   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                               PRIMARY KEY (id),
                               KEY idx_chat_messages_conversation_id (conversation_id, id),

                               CONSTRAINT fk_chat_messages_conversation
                                   FOREIGN KEY (conversation_id) REFERENCES chat_conversations (id),
                               CONSTRAINT chk_chat_messages_role
                                   CHECK (role IN ('user', 'assistant')),
                               CONSTRAINT chk_chat_messages_content
                                   CHECK (CHAR_LENGTH(TRIM(content)) > 0)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
    COMMENT='聊天会话中的消息';

ALTER TABLE chat_messages
    ADD COLUMN recommendation_json JSON NULL
        COMMENT '当次导购结果快照，普通文字消息为空';

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

CREATE TABLE cart_actions (
                              operation_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin
                                                       NOT NULL COMMENT 'Java生成的操作编号',
                              user_id BIGINT NOT NULL COMMENT '操作所属用户',
                              conversation_id BIGINT NOT NULL COMMENT '操作所属会话',
                              source_message_id BIGINT NOT NULL COMMENT '来源推荐消息',
                              product_id BIGINT NOT NULL COMMENT '真实商品编号',
                              title VARCHAR(200) NOT NULL COMMENT '创建操作时的商品名称',
                              base_quantity INT NOT NULL COMMENT '创建操作时的购物车数量，无条目为0',
                              quantity INT NOT NULL COMMENT '确认后要设置的最终数量',
                              status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin
                                                       NOT NULL DEFAULT 'PENDING' COMMENT '操作状态',
                              expires_at DATETIME(3) NOT NULL COMMENT '操作有效期截止时间',
                              executed_quantity INT NULL COMMENT '成功执行时保存的历史结果数量',
                              created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                              updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                  ON UPDATE CURRENT_TIMESTAMP(3),

                              PRIMARY KEY (operation_id),
                              KEY idx_cart_actions_user_conversation (user_id, conversation_id),

                              CONSTRAINT fk_cart_actions_user
                                  FOREIGN KEY (user_id) REFERENCES app_users (id),
                              CONSTRAINT fk_cart_actions_conversation
                                  FOREIGN KEY (conversation_id) REFERENCES chat_conversations (id),
                              CONSTRAINT fk_cart_actions_source
                                  FOREIGN KEY (source_message_id) REFERENCES chat_messages (id),
                              CONSTRAINT fk_cart_actions_product
                                  FOREIGN KEY (product_id) REFERENCES products (id),

                              CONSTRAINT chk_cart_actions_base_quantity
                                  CHECK (base_quantity BETWEEN 0 AND 99),
                              CONSTRAINT chk_cart_actions_quantity
                                  CHECK (quantity BETWEEN 1 AND 99),
                              CONSTRAINT chk_cart_actions_title
                                  CHECK (CHAR_LENGTH(TRIM(title)) > 0),
                              CONSTRAINT chk_cart_actions_status
                                  CHECK (status IN (
                                                    'PENDING', 'SUCCEEDED', 'CANCELLED',
                                                    'EXPIRED', 'CONFLICTED', 'REJECTED'
                                      )),
                              CONSTRAINT chk_cart_actions_result
                                  CHECK (
                                      (status = 'SUCCEEDED'
                                          AND executed_quantity IS NOT NULL
                                          AND executed_quantity = quantity)
                                          OR
                                      (status <> 'SUCCEEDED' AND executed_quantity IS NULL)
                                      )
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
    COMMENT='待确认的购物车操作与执行结果';

ALTER TABLE chat_messages
    ADD COLUMN message_type VARCHAR(16)
                                CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL DEFAULT 'TEXT'
        COMMENT 'TEXT普通消息，CART_ACTION购物车确认提示',

    ADD COLUMN cart_operation_id VARCHAR(36)
                                CHARACTER SET ascii COLLATE ascii_bin
        NULL COMMENT '确认提示关联的购物车操作编号',

    ADD UNIQUE KEY uk_chat_messages_cart_operation (cart_operation_id),

    ADD CONSTRAINT fk_chat_messages_cart_operation
        FOREIGN KEY (cart_operation_id)
            REFERENCES cart_actions (operation_id),

    ADD CONSTRAINT chk_chat_messages_tool_payload
        CHECK (
            (message_type = 'TEXT' AND cart_operation_id IS NULL)
                OR
            (message_type = 'CART_ACTION'
                AND role = 'assistant'
                AND recommendation_json IS NULL
                AND cart_operation_id IS NOT NULL)
            );

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

CREATE TABLE product_inventory (
                                   product_id BIGINT NOT NULL,
                                   available_quantity INT NOT NULL DEFAULT 0,
                                   reserved_quantity INT NOT NULL DEFAULT 0,
                                   updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP,

                                   PRIMARY KEY (product_id),

                                   CONSTRAINT fk_product_inventory_product
                                       FOREIGN KEY (product_id) REFERENCES products (id),

                                   CONSTRAINT chk_product_inventory_available
                                       CHECK (available_quantity >= 0),

                                   CONSTRAINT chk_product_inventory_reserved
                                       CHECK (reserved_quantity >= 0)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE TABLE shop_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    request_key VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_hash VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount DECIMAL(14, 2) NOT NULL,

    recipient_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    province VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(50) NOT NULL,
    detail_address VARCHAR(200) NOT NULL,

    expires_at DATETIME(3) NOT NULL,
    paid_at DATETIME(3) NULL,
    closed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_shop_orders_user_request (user_id, request_key),
    KEY idx_shop_orders_user_id (user_id, id),
    KEY idx_shop_orders_expiry (status, expires_at, id),

    CONSTRAINT fk_shop_orders_user
        FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT chk_shop_orders_request
        CHECK (CHAR_LENGTH(request_key) = 36
            AND CHAR_LENGTH(request_hash) = 64),
    CONSTRAINT chk_shop_orders_amount
        CHECK (total_amount > 0),
    CONSTRAINT chk_shop_orders_status
        CHECK (status IN (
            'PENDING_PAYMENT', 'PAID', 'CANCELLED', 'EXPIRED'
        )),
    CONSTRAINT chk_shop_orders_address
        CHECK (
            CHAR_LENGTH(TRIM(recipient_name)) > 0
            AND CHAR_LENGTH(TRIM(phone)) > 0
            AND CHAR_LENGTH(TRIM(province)) > 0
            AND CHAR_LENGTH(TRIM(city)) > 0
            AND CHAR_LENGTH(TRIM(district)) > 0
            AND CHAR_LENGTH(TRIM(detail_address)) > 0
        ),
    CONSTRAINT chk_shop_orders_expiry
        CHECK (expires_at > created_at),
    CONSTRAINT chk_shop_orders_state_time
        CHECK (
            (status = 'PENDING_PAYMENT'
                AND paid_at IS NULL AND closed_at IS NULL)
            OR
            (status = 'PAID'
                AND paid_at IS NOT NULL AND paid_at >= created_at
                AND closed_at IS NULL)
            OR
            (status IN ('CANCELLED', 'EXPIRED')
                AND paid_at IS NULL
                AND closed_at IS NOT NULL AND closed_at >= created_at)
        )
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    image_url VARCHAR(500) NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    line_amount DECIMAL(14, 2) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_order_items_order_product (order_id, product_id),
    KEY idx_order_items_product (product_id),

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES shop_orders (id),
    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_order_items_text
        CHECK (CHAR_LENGTH(TRIM(title)) > 0
            AND CHAR_LENGTH(TRIM(brand)) > 0),
    CONSTRAINT chk_order_items_price
        CHECK (unit_price >= 0),
    CONSTRAINT chk_order_items_quantity
        CHECK (quantity BETWEEN 1 AND 99),
    CONSTRAINT chk_order_items_amount
        CHECK (line_amount = unit_price * quantity)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE TABLE payment_attempts (
                                  id BIGINT NOT NULL AUTO_INCREMENT,
                                  order_id BIGINT NOT NULL,

                                  request_key VARCHAR(36)
                                         CHARACTER SET ascii COLLATE ascii_bin NOT NULL,

                                  channel VARCHAR(32)
                                         CHARACTER SET ascii COLLATE ascii_bin
                                      NOT NULL DEFAULT 'MOCK',

                                  status VARCHAR(16)
                                         CHARACTER SET ascii COLLATE ascii_bin
                                      NOT NULL DEFAULT 'PENDING',

                                  amount DECIMAL(14, 2) NOT NULL,

                                  expires_at DATETIME(3) NOT NULL,
                                  finished_at DATETIME(3) NULL,

                                  created_at DATETIME(3) NOT NULL
                                               DEFAULT CURRENT_TIMESTAMP(3),

                                  updated_at DATETIME(3) NOT NULL
                                               DEFAULT CURRENT_TIMESTAMP(3)
                                      ON UPDATE CURRENT_TIMESTAMP(3),

                                  active_order_id BIGINT GENERATED ALWAYS AS (
                                      CASE WHEN status = 'PENDING'
                                               THEN order_id ELSE NULL END
                                      ) STORED,

                                  successful_order_id BIGINT GENERATED ALWAYS AS (
                                      CASE WHEN status = 'SUCCEEDED'
                                               THEN order_id ELSE NULL END
                                      ) STORED,

                                  PRIMARY KEY (id),

                                  UNIQUE KEY uk_payment_attempts_order_request (
                                                                                order_id, request_key
                                      ),

                                  UNIQUE KEY uk_payment_attempts_active (
                                                                         active_order_id
                                      ),

                                  UNIQUE KEY uk_payment_attempts_success (
                                                                          successful_order_id
                                      ),

                                  KEY idx_payment_attempts_order_id (order_id, id),

                                  KEY idx_payment_attempts_expiry (
                                                                   status, expires_at, id
                                      ),

                                  CONSTRAINT fk_payment_attempts_order
                                      FOREIGN KEY (order_id)
                                          REFERENCES shop_orders (id),

                                  CONSTRAINT chk_payment_attempts_request
                                      CHECK (CHAR_LENGTH(request_key) = 36),

                                  CONSTRAINT chk_payment_attempts_channel
                                      CHECK (channel = 'MOCK'),

                                  CONSTRAINT chk_payment_attempts_status
                                      CHECK (
                                          status IN (
                                                     'PENDING', 'SUCCEEDED', 'FAILED', 'CLOSED'
                                              )
                                          ),

                                  CONSTRAINT chk_payment_attempts_amount
                                      CHECK (amount > 0),

                                  CONSTRAINT chk_payment_attempts_expiry
                                      CHECK (expires_at > created_at),

                                  CONSTRAINT chk_payment_attempts_state_time
                                      CHECK (
                                          (status = 'PENDING' AND finished_at IS NULL)
                                              OR
                                          (
                                              status IN ('SUCCEEDED', 'FAILED', 'CLOSED')
                                                  AND finished_at IS NOT NULL
                                                  AND finished_at >= created_at
                                              )
                                          )
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE TABLE mock_payment_receipts (
    payment_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    outcome VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (payment_id),
    CONSTRAINT fk_mock_payment_receipts_payment
        FOREIGN KEY (payment_id) REFERENCES payment_attempts (id),
    CONSTRAINT fk_mock_payment_receipts_order
        FOREIGN KEY (order_id) REFERENCES shop_orders (id),
    CONSTRAINT chk_mock_payment_receipts_amount CHECK (amount > 0),
    CONSTRAINT chk_mock_payment_receipts_outcome CHECK (outcome IN ('SUCCEEDED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE payment_attempts
    ADD COLUMN out_trade_no VARCHAR(64)
                                CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD COLUMN provider_app_id VARCHAR(32)
                                CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD COLUMN provider_seller_id VARCHAR(32)
                                CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD COLUMN provider_trade_no VARCHAR(64)
                                CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD COLUMN provider_paid_at DATETIME(3) NULL,
    DROP CHECK chk_payment_attempts_channel,
    ADD CONSTRAINT chk_payment_attempts_channel
        CHECK (channel IN ('MOCK', 'ALIPAY_SANDBOX')),
    ADD UNIQUE KEY uk_payment_attempts_out_trade (out_trade_no),
    ADD UNIQUE KEY uk_payment_attempts_provider_trade (
                                                       channel, provider_trade_no
        ),
    ADD CONSTRAINT chk_payment_attempts_provider_identity
        CHECK (
            (channel = 'MOCK'
                AND out_trade_no IS NULL
                AND provider_app_id IS NULL
                AND provider_seller_id IS NULL
                AND provider_trade_no IS NULL
                AND provider_paid_at IS NULL)
                OR
            (channel = 'ALIPAY_SANDBOX'
                AND out_trade_no IS NOT NULL
                AND CHAR_LENGTH(TRIM(out_trade_no)) > 0
                AND provider_app_id IS NOT NULL
                AND CHAR_LENGTH(TRIM(provider_app_id)) > 0
                AND provider_seller_id IS NOT NULL
                AND CHAR_LENGTH(TRIM(provider_seller_id)) > 0)
            ),
    ADD CONSTRAINT chk_payment_attempts_provider_success
        CHECK (
            channel = 'MOCK' OR status <> 'SUCCEEDED'
                OR (provider_trade_no IS NOT NULL
                AND CHAR_LENGTH(TRIM(provider_trade_no)) > 0
                AND provider_paid_at IS NOT NULL)
            );

CREATE TABLE inventory_replenishments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    actor VARCHAR(100) COLLATE utf8mb4_bin NOT NULL,
    request_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    quantity INT NOT NULL,
    reason VARCHAR(200) NOT NULL,
    available_quantity INT NOT NULL,
    reserved_quantity INT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_replenishment_request (actor, request_key),
    KEY idx_inventory_replenishment_product (product_id, id),
    CONSTRAINT fk_inventory_replenishment_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_inventory_replenishment_quantity CHECK (quantity BETWEEN 1 AND 1000000),
    CONSTRAINT chk_inventory_replenishment_snapshot CHECK (available_quantity >= 0 AND reserved_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
