package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.MessageSendRequest;
import com.wwzyhy.shopagent.service.ChatMessageStreamService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class ChatMessageStreamController {

    private final ChatMessageStreamService streamService;

    public ChatMessageStreamController(ChatMessageStreamService streamService) {
        this.streamService = streamService;
    }

    @PostMapping("/api/conversations/{conversationId}/messages/stream")
    public ResponseEntity<SseEmitter> stream(
            @PathVariable("conversationId") Long conversationId,
            @Valid @RequestBody MessageSendRequest request,
            Authentication authentication) {

        SseEmitter emitter = streamService.stream(authentication, conversationId, request);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }
}
