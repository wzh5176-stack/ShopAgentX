package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.dto.ConversationTurnRequest;
import com.wwzyhy.shopagent.dto.ConversationTurnResponse;
import com.wwzyhy.shopagent.service.ConversationTurnService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations/{conversationId}/turns")
public class ConversationTurnController {

    private final ConversationTurnService conversationTurnService;

    public ConversationTurnController(
            ConversationTurnService conversationTurnService) {
        this.conversationTurnService = conversationTurnService;
    }

    @PostMapping
    public Map<String, ConversationTurnResponse> send(
            @PathVariable("conversationId") Long conversationId,
            @Valid @RequestBody ConversationTurnRequest request,
            Authentication authentication) {

        ConversationTurnResponse data = conversationTurnService.send(
                authentication,
                conversationId,
                request);

        return Map.of("data", data);
    }
}