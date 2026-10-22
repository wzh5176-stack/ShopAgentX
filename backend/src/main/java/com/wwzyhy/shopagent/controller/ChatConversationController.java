package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.ConversationCreateRequest;
import com.wwzyhy.shopagent.dto.ConversationResponse;
import com.wwzyhy.shopagent.service.ChatConversationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.wwzyhy.shopagent.dto.ConversationPageResponse;
import org.springframework.web.bind.annotation.RequestParam;
import com.wwzyhy.shopagent.dto.MessagePageResponse;
import com.wwzyhy.shopagent.service.ChatMessageService;
import com.wwzyhy.shopagent.dto.MessageSendRequest;
import com.wwzyhy.shopagent.dto.MessageSendResponse;
import com.wwzyhy.shopagent.service.ChatMessageSendService;


@RestController
@RequestMapping("/api/conversations")
public class ChatConversationController {

    private final ChatConversationService chatConversationService;
    private final ChatMessageService chatMessageService;
    private final ChatMessageSendService chatMessageSendService;

    public ChatConversationController(
            ChatConversationService chatConversationService,
            ChatMessageService chatMessageService,
            ChatMessageSendService chatMessageSendService) {
        this.chatConversationService = chatConversationService;
        this.chatMessageService = chatMessageService;
        this.chatMessageSendService = chatMessageSendService;
    }

    @PostMapping
    public ResponseEntity<Map<String, ConversationResponse>> create(
            @Valid @RequestBody ConversationCreateRequest request,
            Authentication authentication) {

        ConversationResponse data =
                chatConversationService.create(authentication, request);

        URI location = URI.create("/api/conversations/" + data.id());

        return ResponseEntity.created(location)
                .body(Map.of("data", data));
    }

    @GetMapping("/{id}")
    public Map<String, ConversationResponse> getOwned(
            @PathVariable("id") Long id,
            Authentication authentication) {

        ConversationResponse data =
                chatConversationService.getOwned(authentication, id);

        return Map.of("data", data);
    }

    @GetMapping
    public Map<String, ConversationPageResponse> listOwned(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        ConversationPageResponse data =
                chatConversationService.listOwned(authentication, page, size);

        return Map.of("data", data);
    }


    @GetMapping("/{conversationId}/messages")
    public Map<String, MessagePageResponse> readHistory(
            @PathVariable("conversationId") Long conversationId,
            Authentication authentication,
            @RequestParam(name = "beforeId", required = false) Long beforeId,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        MessagePageResponse data = chatMessageService.readHistory(
                authentication, conversationId, beforeId, size);

        return Map.of("data", data);
    }

    @PostMapping("/{conversationId}/messages")
    public Map<String, MessageSendResponse> sendMessage(
            @PathVariable("conversationId") Long conversationId,
            @Valid @RequestBody MessageSendRequest request,
            Authentication authentication) {

        MessageSendResponse data = chatMessageSendService.send(
                authentication, conversationId, request);

        return Map.of("data", data);
    }

}