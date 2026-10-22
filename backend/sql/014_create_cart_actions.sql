USE shopagent;

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

SHOW CREATE TABLE cart_actions;