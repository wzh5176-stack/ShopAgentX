package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.ChatRequest;
import com.wwzyhy.shopagent.service.AiModelStreamService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class AdminAiStreamController {

    private final AiModelStreamService streamService;

    public AdminAiStreamController(AiModelStreamService streamService) {
        this.streamService = streamService;
    }

    @PostMapping("/api/admin/ai/chat/stream")
    public ResponseEntity<SseEmitter> stream(
            @Valid @RequestBody ChatRequest request) {

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(streamService.stream(request.message()));
    }
}