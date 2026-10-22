package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.AiConversationIntentRequest;
import com.wwzyhy.shopagent.dto.AiIntentData;
import com.wwzyhy.shopagent.dto.AiStreamDelta;
import com.wwzyhy.shopagent.dto.AiStreamError;
import com.wwzyhy.shopagent.dto.AssistantResponse;
import com.wwzyhy.shopagent.dto.AssistantResult;
import com.wwzyhy.shopagent.dto.ConversationContext;
import com.wwzyhy.shopagent.dto.MessageSendRequest;
import com.wwzyhy.shopagent.dto.MessageSendResponse;
import com.wwzyhy.shopagent.dto.ProductSummaryResponse;
import com.wwzyhy.shopagent.exception.ConversationChangedException;
import com.wwzyhy.shopagent.exception.ConversationNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ChatMessageStreamService {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageStreamService.class);

    private final ChatMessageService chatMessageService;
    private final ChatMessageWriteService writeService;
    private final AssistantStreamService assistantStreamService;
    private final AiClient aiClient;
    private final ThreadPoolTaskExecutor executor;
    private final long timeoutMs;

    public ChatMessageStreamService(
            ChatMessageService chatMessageService,
            ChatMessageWriteService writeService,
            AssistantStreamService assistantStreamService,
            AiClient aiClient,
            @Qualifier("aiStreamExecutor") ThreadPoolTaskExecutor executor,
            @Value("${shopagent.ai.stream-timeout-ms:60000}") long timeoutMs) {
        this.chatMessageService = chatMessageService;
        this.writeService = writeService;
        this.assistantStreamService = assistantStreamService;
        this.aiClient = aiClient;
        this.executor = executor;
        this.timeoutMs = timeoutMs;
    }

    public SseEmitter stream(
            Authentication authentication, Long conversationId, MessageSendRequest request) {

        if (request == null || request.content() == null
                || request.content().isBlank() || request.content().length() > 1000) {
            throw new IllegalArgumentException("消息必须为1到1000个字符且不能全为空白");
        }

        // Verify ownership and capture the same history/version before queueing model work.
        ConversationContext context = chatMessageService.loadContext(authentication, conversationId);
        StreamJob job = new StreamJob(timeoutMs);
        try {
            Future<?> task = executor.submit(() -> runTurn(
                    authentication, conversationId, request.content(), context, job));
            job.attach(task);
        } catch (TaskRejectedException ex) {
            job.cancel();
            throw new RestClientException("流式服务繁忙，请稍后重试", ex);
        }
        return job.emitter;
    }

    private void runTurn(
            Authentication authentication,
            Long conversationId,
            String content,
            ConversationContext context,
            StreamJob job) {

        try {
            job.checkRunning();
            var intent = aiClient.parseConversationIntent(
                    new AiConversationIntentRequest(content, context.history()));
            job.checkRunning();

            AssistantResponse recommendation;
            if (intent.clarification() != null) {
                recommendation = new AssistantResponse(
                        intent.keyword(), intent.maxPrice(), intent.clarification(), List.of());
            } else {
                AssistantResult result = assistantStreamService.recommend(
                        content,
                        new AiIntentData(intent.keyword(), intent.maxPrice()),
                        job::delta,
                        job::opened);
                recommendation = toResponse(result);
            }

            // Clarification and empty product results do not open a Python explanation stream.
            job.ensurePreview(recommendation.answer());

            // Cancellation can prevent entering this boundary. Once entered, let the short
            // database transaction finish; losing the receiver cannot undo a committed turn.
            job.beginCommit();
            MessageSendResponse saved = writeService.saveTurn(
                    authentication, conversationId, content, recommendation, context.lastMessageId());
            job.success(saved);
        } catch (CancellationException ignored) {
            job.cancel();
            job.completeCancelled();
        } catch (RuntimeException ex) {
            log.warn("会话流式导购失败", ex);
            job.failure(toError(ex));
        } finally {
            job.closeInput();
        }
    }

    private AssistantResponse toResponse(AssistantResult result) {
        List<ProductSummaryResponse> products = new ArrayList<>();
        for (var product : result.products()) {
            products.add(new ProductSummaryResponse(
                    product.getId(), product.getTitle(), product.getBrand(),
                    product.getPrice().setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                    product.getImageUrl()));
        }
        return new AssistantResponse(
                result.intent().keyword(), result.intent().maxPrice(), result.answer(), products);
    }

    private AiStreamError toError(RuntimeException ex) {
        if (ex instanceof ConversationChangedException) {
            return new AiStreamError("CONVERSATION_CHANGED", "会话已有新消息，请刷新后重新发送");
        }
        if (ex instanceof ConversationNotFoundException) {
            return new AiStreamError("CONVERSATION_NOT_FOUND", "会话不存在");
        }
        if (ex instanceof AuthenticationException) {
            return new AiStreamError("UNAUTHORIZED", "登录状态无效，请重新登录");
        }
        if (ex instanceof DataAccessException || ex instanceof TransactionException) {
            return new AiStreamError("SERVICE_UNAVAILABLE", "数据库服务暂时不可用，请稍后再试");
        }
        if (ex instanceof RestClientException) {
            return new AiStreamError("AI_SERVICE_UNAVAILABLE", "模型服务暂时不可用，请稍后重试");
        }
        return new AiStreamError("INTERNAL_ERROR", "服务内部错误，请稍后再试");
    }

    private static final class StreamJob {

        private enum Phase { RUNNING, COMMITTING, FINISHED, CANCELLED }

        private final SseEmitter emitter;
        private final long deadlineNanos;
        private Phase phase = Phase.RUNNING;
        private boolean disconnected;
        private boolean previewSent;
        private Future<?> task;
        private InputStream input;

        private StreamJob(long timeoutMs) {
            emitter = new SseEmitter(timeoutMs);
            deadlineNanos = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(timeoutMs);
            emitter.onCompletion(this::cancel);
            emitter.onError(error -> cancel());
            emitter.onTimeout(() -> {
                cancel();
                emitter.complete();
            });
        }

        private void attach(Future<?> future) {
            boolean cancelled;
            synchronized (this) {
                task = future;
                cancelled = phase == Phase.CANCELLED;
            }
            if (cancelled) {
                future.cancel(true);
            }
        }

        private synchronized void checkRunning() {
            if (phase != Phase.RUNNING || disconnected
                    || Thread.currentThread().isInterrupted()
                    || System.nanoTime() >= deadlineNanos) {
                throw new CancellationException("会话导购已停止");
            }
        }

        private void opened(InputStream openedInput) {
            boolean rejected;
            synchronized (this) {
                rejected = phase != Phase.RUNNING || disconnected;
                if (!rejected) {
                    input = openedInput;
                }
            }
            if (rejected) {
                close(openedInput);
                throw new CancellationException("会话导购已停止");
            }
        }

        private synchronized void delta(AiStreamDelta data) {
            checkRunning();
            emit("delta", data);
            previewSent = true;
        }

        private synchronized void ensurePreview(String answer) {
            checkRunning();
            if (!previewSent) {
                delta(new AiStreamDelta(answer));
            }
        }

        private synchronized void beginCommit() {
            checkRunning();
            phase = Phase.COMMITTING;
        }

        private synchronized void success(MessageSendResponse saved) {
            phase = Phase.FINISHED;
            if (!disconnected) {
                try {
                    // This is a business completion event, sent only after saveTurn commits.
                    emit("done", saved);
                    emitter.complete();
                } catch (CancellationException ignored) {
                    // A committed turn is still available from the history endpoint.
                }
            }
        }

        private synchronized void failure(AiStreamError error) {
            if (phase == Phase.FINISHED || phase == Phase.CANCELLED || disconnected) {
                return;
            }
            phase = Phase.FINISHED;
            try {
                emit("error", error);
                emitter.complete();
            } catch (CancellationException ignored) {
            }
        }

        // All emit callers hold this job's monitor, so terminal and preview events cannot race.
        private void emit(String event, Object data) {
            try {
                emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException ex) {
                disconnected = true;
                if (phase == Phase.RUNNING) {
                    phase = Phase.CANCELLED;
                }
                throw new CancellationException("接收端连接已关闭");
            }
        }

        private void cancel() {
            Future<?> running = null;
            synchronized (this) {
                disconnected = true;
                if (phase == Phase.RUNNING) {
                    phase = Phase.CANCELLED;
                    running = task;
                }
            }
            closeInput();
            if (running != null) {
                running.cancel(true);
            }
        }

        private void closeInput() {
            InputStream opened;
            synchronized (this) {
                opened = input;
                input = null;
            }
            close(opened);
        }

        private static void close(InputStream opened) {
            if (opened != null) {
                try {
                    opened.close();
                } catch (IOException ignored) {
                }
            }
        }

        private void completeCancelled() {
            emitter.complete();
        }
    }
}
