package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.AiChatData;
import com.wwzyhy.shopagent.dto.AiStreamDelta;
import com.wwzyhy.shopagent.dto.AiStreamError;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Component
public class AiStreamJsonCodec {

    private final JsonMapper jsonMapper;

    public AiStreamJsonCodec(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper.rebuild()
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build();
    }

    public AiStreamDelta readDelta(String json) {
        AiStreamDelta data = read(json, AiStreamDelta.class);
        if (data == null || data.content() == null
                || data.content().isEmpty() || data.content().length() > 2000) {
            throw new RestClientException("Python文字片段不符合约定");
        }
        return data;
    }

    public AiChatData readDone(String json, String receivedAnswer) {
        AiChatData data = read(json, AiChatData.class);
        if (data == null || data.answer() == null || data.answer().isBlank()
                || data.answer().length() > 2000
                || !"shopagent-ai".equals(data.service())
                || !data.answer().equals(receivedAnswer)) {
            throw new RestClientException("Python结束事件不符合约定");
        }
        return data;
    }

    public AiStreamError readError(String json) {
        AiStreamError data = read(json, AiStreamError.class);
        if (data == null || !"AI_SERVICE_UNAVAILABLE".equals(data.code())
                || data.message() == null || data.message().isBlank()
                || data.message().length() > 200) {
            throw new RestClientException("Python错误事件不符合约定");
        }
        return new AiStreamError(
                "AI_SERVICE_UNAVAILABLE",
                "模型服务暂时不可用，请稍后重试"
        );
    }

    private <T> T read(String json, Class<T> type) {
        if (json == null || json.isBlank() || json.length() > 16000) {
            throw new RestClientException("Python事件JSON不符合约定");
        }
        try {
            return jsonMapper.readValue(json, type);
        } catch (JacksonException ex) {
            throw new RestClientException("Python事件JSON解析失败", ex);
        }
    }
}