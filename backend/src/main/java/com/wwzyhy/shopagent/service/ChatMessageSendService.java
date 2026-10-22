package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.AiConversationIntentData;
import com.wwzyhy.shopagent.dto.AiConversationIntentRequest;
import com.wwzyhy.shopagent.dto.AiIntentData;
import com.wwzyhy.shopagent.dto.AssistantResponse;
import com.wwzyhy.shopagent.dto.AssistantResult;
import com.wwzyhy.shopagent.dto.ConversationContext;
import com.wwzyhy.shopagent.dto.MessageSendRequest;
import com.wwzyhy.shopagent.dto.MessageSendResponse;
import com.wwzyhy.shopagent.dto.ProductSummaryResponse;
import com.wwzyhy.shopagent.entity.Product;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageSendService {

    private final ChatMessageService chatMessageService;
    private final AssistantService assistantService;
    private final ChatMessageWriteService chatMessageWriteService;
    private final AiClient aiClient;

    public ChatMessageSendService(
            ChatMessageService chatMessageService,
            AssistantService assistantService,
            ChatMessageWriteService chatMessageWriteService,
            AiClient aiClient) {
        this.chatMessageService = chatMessageService;
        this.assistantService = assistantService;
        this.chatMessageWriteService = chatMessageWriteService;
        this.aiClient = aiClient;
    }

    public MessageSendResponse send(
            Authentication authentication,
            Long conversationId,
            MessageSendRequest request) {

        ConversationContext context =
                chatMessageService.loadContext(authentication, conversationId);

        if (request == null
                || request.content() == null
                || request.content().isBlank()
                || request.content().length() > 1000) {
            throw new IllegalArgumentException(
                    "消息必须为1到1000个字符且不能全为空白");
        }

        String content = request.content();

        AiConversationIntentData intent =
                aiClient.parseConversationIntent(
                        new AiConversationIntentRequest(
                                content, context.history())
                );

        AssistantResponse recommendation;

        if (intent.clarification() != null) {
            recommendation = new AssistantResponse(
                    intent.keyword(),
                    intent.maxPrice(),
                    intent.clarification(),
                    List.of()
            );
        } else {
            AssistantResult result = assistantService.askWithIntent(
                    content,
                    new AiIntentData(intent.keyword(), intent.maxPrice())
            );

            List<ProductSummaryResponse> products = new ArrayList<>();
            for (Product product : result.products()) {
                products.add(new ProductSummaryResponse(
                        product.getId(),
                        product.getTitle(),
                        product.getBrand(),
                        product.getPrice()
                                .setScale(2, RoundingMode.UNNECESSARY)
                                .toPlainString(),
                        product.getImageUrl()
                ));
            }

            recommendation = new AssistantResponse(
                    result.intent().keyword(),
                    result.intent().maxPrice(),
                    result.answer(),
                    products
            );
        }

        return chatMessageWriteService.saveTurn(
                authentication,
                conversationId,
                content,
                recommendation,
                context.lastMessageId()
        );
    }
}