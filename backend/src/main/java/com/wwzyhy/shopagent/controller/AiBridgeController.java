package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.service.AiClient;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.wwzyhy.shopagent.dto.AiEchoData;
import com.wwzyhy.shopagent.dto.EchoRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.wwzyhy.shopagent.dto.AiChatData;
import com.wwzyhy.shopagent.dto.ChatRequest;


@RestController
public class AiBridgeController {

    private final AiClient aiClient;

    public AiBridgeController(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @GetMapping("/api/admin/ai/health")
    public Map<String, Object> health() {
        return Map.of("data", aiClient.health());
    }

    @PostMapping("/api/admin/ai/echo")
    public Map<String, Object> echo(@Valid @RequestBody EchoRequest request) {
        String message = request.message();
        AiEchoData data = aiClient.echo(message);
        return Map.of("data", data);
    }

    @PostMapping("/api/admin/ai/chat")
    public Map<String, Object> chat(@Valid @RequestBody ChatRequest request) {
        String message = request.message();
        AiChatData data = aiClient.chat(message);
        return Map.of("data", data);
    }
}
