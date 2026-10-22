USE shopagent;

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

SHOW CREATE TABLE chat_messages;