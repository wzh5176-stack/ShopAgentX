package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.wwzyhy.shopagent.dto.CartActionCreateRequest;
import com.wwzyhy.shopagent.dto.CartActionResponse;
import com.wwzyhy.shopagent.dto.CartToolPrepareResponse;
import com.wwzyhy.shopagent.dto.ConversationTurnResponse;
import com.wwzyhy.shopagent.dto.MessageResponse;
import com.wwzyhy.shopagent.entity.ChatConversation;
import com.wwzyhy.shopagent.entity.ChatMessage;
import com.wwzyhy.shopagent.exception.ConversationChangedException;
import com.wwzyhy.shopagent.exception.ConversationNotFoundException;
import com.wwzyhy.shopagent.mapper.ChatConversationMapper;
import com.wwzyhy.shopagent.mapper.ChatMessageMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ToolMessageWriteService {

    private final CurrentUserService currentUserService;
    private final ChatConversationMapper chatConversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final CartActionService cartActionService;
    private final RecommendationSourceService recommendationSourceService;

    public ToolMessageWriteService(
            CurrentUserService currentUserService,
            ChatConversationMapper chatConversationMapper,
            ChatMessageMapper chatMessageMapper,
            CartActionService cartActionService,
            RecommendationSourceService recommendationSourceService) {
        this.currentUserService = currentUserService;
        this.chatConversationMapper = chatConversationMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.cartActionService = cartActionService;
        this.recommendationSourceService = recommendationSourceService;
    }

    @Transactional
    public ConversationTurnResponse saveToolTurn(
            Authentication authentication,
            Long conversationId,
            String content,
            CartToolPrepareResponse intent,
            long expectedLastMessageId) {

        Long userId = currentUserService.requireUserId(authentication);

        if (conversationId == null || conversationId <= 0
                || expectedLastMessageId < 0
                || content == null || content.isBlank()
                || content.length() > 1000) {
            throw new IllegalArgumentException("会话或消息参数不符合要求");
        }

        if (intent == null
                || !conversationId.equals(intent.conversationId())
                || intent.sourceMessageId() == null
                || intent.sourceMessageId() <= 0) {
            throw new IllegalStateException("工具意图的会话或来源不完整");
        }

        boolean needsAction = "set_quantity".equals(intent.action());
        boolean needsClarification = "clarify".equals(intent.action());

        if ((!needsAction && !needsClarification)
                || (needsAction && (intent.product() == null
                || intent.product().id() == null
                || intent.product().id() <= 0
                || intent.quantity() == null
                || intent.quantity() < 1 || intent.quantity() > 99))
                || (needsClarification && (intent.product() != null
                || intent.quantity() != null
                || intent.message() == null
                || intent.message().isBlank()
                || intent.message().length() > 1000))) {
            throw new IllegalStateException("工具意图结果不符合约定");
        }

        QueryWrapper<ChatConversation> conversationQuery = new QueryWrapper<>();
        conversationQuery.eq("id", conversationId);
        conversationQuery.eq("user_id", userId);
        conversationQuery.last("FOR UPDATE");

        ChatConversation conversation =
                chatConversationMapper.selectOne(conversationQuery);

        if (conversation == null) {
            throw new ConversationNotFoundException();
        }

        QueryWrapper<ChatMessage> latestQuery = new QueryWrapper<>();
        latestQuery.select("id");
        latestQuery.eq("conversation_id", conversationId);
        latestQuery.orderByDesc("id");
        latestQuery.last("LIMIT 1 FOR SHARE");

        ChatMessage latest = chatMessageMapper.selectOne(latestQuery);
        long actualLastMessageId = latest == null ? 0L : latest.getId();

        if (actualLastMessageId != expectedLastMessageId) {
            throw new ConversationChangedException();
        }

        CartActionResponse action = null;
        String answer;
        String messageType;
        String operationId = null;

        if (needsAction) {
            action = cartActionService.create(
                    authentication,
                    conversationId,
                    new CartActionCreateRequest(
                            intent.sourceMessageId(),
                            intent.product().id(),
                            intent.quantity()));

            answer = "请确认将「" + action.title()
                    + "」在购物车中的数量设为" + action.quantity() + "。";
            messageType = "CART_ACTION";
            operationId = action.operationId();
        } else {
            recommendationSourceService.loadProducts(
                    authentication, conversationId, intent.sourceMessageId());

            answer = intent.message();
            messageType = "TEXT";
        }

        ChatMessage userMessage = insertMessage(
                conversationId, "user", content, "TEXT", null);

        ChatMessage assistantMessage = insertMessage(
                conversationId, "assistant", answer, messageType, operationId);

        UpdateWrapper<ChatConversation> update = new UpdateWrapper<>();
        update.eq("id", conversationId);
        update.eq("user_id", userId);
        update.setSql("updated_at = CURRENT_TIMESTAMP");
        chatConversationMapper.update(null, update);

        return new ConversationTurnResponse(
                needsAction ? "cart_action" : "clarify",
                null,
                action,
                answer,
                toResponse(userMessage),
                toResponse(assistantMessage));
    }

    private ChatMessage insertMessage(
            Long conversationId,
            String role,
            String content,
            String messageType,
            String operationId) {

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setMessageType(messageType);
        message.setCartOperationId(operationId);

        if (chatMessageMapper.insert(message) != 1 || message.getId() == null) {
            throw new IllegalStateException("保存工具消息失败");
        }

        ChatMessage saved = chatMessageMapper.selectById(message.getId());

        if (saved == null || saved.getCreatedAt() == null) {
            throw new IllegalStateException("读取工具消息失败");
        }

        return saved;
    }

    private MessageResponse toResponse(ChatMessage message) {
        return new MessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt(),
                null,
                message.getMessageType(),
                message.getCartOperationId());
    }
}