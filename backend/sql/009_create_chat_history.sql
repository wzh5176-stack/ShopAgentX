USE shopagent;

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

SHOW CREATE TABLE chat_conversations;
SHOW CREATE TABLE chat_messages;