package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.AssistantResponse;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class RecommendationJsonCodec {

    private final JsonMapper jsonMapper;

    public RecommendationJsonCodec(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public String toJson(AssistantResponse recommendation) {
        if (recommendation == null) {
            return null;
        }

        try {
            return jsonMapper.writeValueAsString(recommendation);
        } catch (JacksonException exception) {
            throw new IllegalStateException("导购结果转换失败", exception);
        }
    }

    public AssistantResponse fromJson(String json) {
        if (json == null) {
            return null;
        }

        try {
            AssistantResponse recommendation =
                    jsonMapper.readValue(json, AssistantResponse.class);

            if (recommendation == null) {
                throw new IllegalStateException(
                        "已保存的导购结果不是有效对象");
            }

            return recommendation;
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "已保存的导购结果读取失败", exception);
        }
    }
}