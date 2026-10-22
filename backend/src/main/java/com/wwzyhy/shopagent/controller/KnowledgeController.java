package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.AiKnowledgeData;
import com.wwzyhy.shopagent.dto.IntentRequest;
import com.wwzyhy.shopagent.service.AiClient;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KnowledgeController {

    private final AiClient aiClient;

    public KnowledgeController(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @PostMapping("/api/assistant/knowledge")
    public Map<String, AiKnowledgeData> askKnowledge(
            @Valid @RequestBody IntentRequest request) {

        AiKnowledgeData data =
                aiClient.askKnowledge(request.question().trim());

        return Map.of("data", data);
    }
}