USE shopagent;

ALTER TABLE chat_messages
    ADD COLUMN recommendation_json JSON NULL
        COMMENT '当次导购结果快照，普通文字消息为空';

SHOW COLUMNS FROM chat_messages LIKE 'recommendation_json';