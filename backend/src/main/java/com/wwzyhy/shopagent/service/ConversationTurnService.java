package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.CartToolPrepareRequest;
import com.wwzyhy.shopagent.dto.CartToolPrepareResponse;
import com.wwzyhy.shopagent.dto.ConversationContext;
import com.wwzyhy.shopagent.dto.ConversationTurnRequest;
import com.wwzyhy.shopagent.dto.ConversationTurnResponse;
import com.wwzyhy.shopagent.dto.MessageSendRequest;
import com.wwzyhy.shopagent.dto.MessageSendResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ConversationTurnService {

    private final ChatMessageSendService chatMessageSendService;
    private final CartToolPrepareService cartToolPrepareService;
    private final ChatMessageService chatMessageService;
    private final ToolMessageWriteService toolMessageWriteService;

    public ConversationTurnService(
            ChatMessageSendService chatMessageSendService,
            CartToolPrepareService cartToolPrepareService,
            ChatMessageService chatMessageService,
            ToolMessageWriteService toolMessageWriteService) {
        this.chatMessageSendService = chatMessageSendService;
        this.cartToolPrepareService = cartToolPrepareService;
        this.chatMessageService = chatMessageService;
        this.toolMessageWriteService = toolMessageWriteService;
    }

    public ConversationTurnResponse send(
            Authentication authentication,
            Long conversationId,
            ConversationTurnRequest request) {

        if (conversationId == null || conversationId <= 0) {
            throw new IllegalArgumentException("会话编号必须为正整数");
        }

        if (request == null
                || request.content() == null
                || request.content().isBlank()
                || request.content().length() > 1000
                || (request.sourceMessageId() != null
                && request.sourceMessageId() <= 0)) {
            throw new IllegalArgumentException("消息或推荐来源不符合要求");
        }

        String content = request.content();

        if (request.sourceMessageId() == null) {
            return sendGuide(authentication, conversationId, content);
        }

        ConversationContext context =
                chatMessageService.loadContext(authentication, conversationId);

        CartToolPrepareResponse intent = cartToolPrepareService.prepare(
                authentication,
                conversationId,
                new CartToolPrepareRequest(
                        request.sourceMessageId(), content));

        if ("none".equals(intent.action())) {
            return sendGuide(authentication, conversationId, content);
        }

        return toolMessageWriteService.saveToolTurn(
                authentication,
                conversationId,
                content,
                intent,
                context.lastMessageId());
    }

    private ConversationTurnResponse sendGuide(
            Authentication authentication,
            Long conversationId,
            String content) {

        MessageSendResponse guide = chatMessageSendService.send(
                authentication,
                conversationId,
                new MessageSendRequest(content));

        return new ConversationTurnResponse(
                "guide", guide, null, "已生成导购回复。", null, null);
    }
}