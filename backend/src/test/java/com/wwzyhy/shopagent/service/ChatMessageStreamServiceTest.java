package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClientException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatMessageStreamServiceTest {
    private final ChatMessageService messages = mock(ChatMessageService.class);
    private final ChatMessageWriteService writes = mock(ChatMessageWriteService.class);
    private final AssistantStreamService guide = mock(AssistantStreamService.class);
    private final AiClient ai = mock(AiClient.class);
    private final ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
    private final ChatMessageStreamService service = new ChatMessageStreamService(messages, writes, guide, ai, executor, 60000);

    @BeforeEach void setup() {
        when(messages.loadContext(null, 1L)).thenReturn(new ConversationContext(List.of(), 0L));
        when(ai.parseConversationIntent(any())).thenReturn(new AiConversationIntentData("耳机", null, null));
        when(executor.submit(any(Runnable.class))).thenAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return CompletableFuture.completedFuture(null);
        });
    }

    @Test void upstreamFailureCannotSavePartialAnswer() {
        when(guide.recommend(anyString(), any(), any(), any())).thenThrow(new RestClientException("truncated stream"));
        service.stream(null, 1L, new MessageSendRequest("推荐耳机"));
        verifyNoInteractions(writes);
    }

    @Test void clarificationIsSavedWithoutCallingRecommendation() {
        when(ai.parseConversationIntent(any())).thenReturn(new AiConversationIntentData("", null, "想买什么商品？"));
        service.stream(null, 1L, new MessageSendRequest("帮我选"));
        verify(writes).saveTurn(isNull(), eq(1L), eq("帮我选"), argThat(reply -> reply.answer().equals("想买什么商品？")), eq(0L));
        verifyNoInteractions(guide);
    }
}
