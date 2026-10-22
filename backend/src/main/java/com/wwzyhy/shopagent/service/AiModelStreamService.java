package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.AiStreamError;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AiModelStreamService {

    private static final Logger log =
            LoggerFactory.getLogger(AiModelStreamService.class);

    private final AiClient aiClient;
    private final AiStreamJsonCodec codec;
    private final ThreadPoolTaskExecutor executor;
    private final long timeoutMs;

    public AiModelStreamService(
            AiClient aiClient,
            AiStreamJsonCodec codec,
            @Qualifier("aiStreamExecutor") ThreadPoolTaskExecutor executor,
            @Value("${shopagent.ai.stream-timeout-ms:60000}") long timeoutMs) {
        this.aiClient = aiClient;
        this.codec = codec;
        this.executor = executor;
        this.timeoutMs = timeoutMs;
    }

    public SseEmitter stream(String message) {
        if (message == null || message.isBlank() || message.length() > 1000) {
            throw new IllegalArgumentException("消息必须为1到1000个字符且不能全为空白");
        }

        SseEmitter emitter = new SseEmitter(timeoutMs);
        AtomicBoolean stopped = new AtomicBoolean();
        AtomicReference<Future<?>> taskRef = new AtomicReference<>();
        AtomicReference<InputStream> inputRef = new AtomicReference<>();

        Runnable cancel = () -> {
            stopped.set(true);
            closeInput(inputRef);
            Future<?> task = taskRef.get();
            if (task != null) {
                task.cancel(true);
            }
        };

        emitter.onCompletion(cancel);
        emitter.onError(error -> cancel.run());
        emitter.onTimeout(() -> {
            cancel.run();
            emitter.complete();
        });

        try {
            Future<?> task = executor.submit(
                    () -> relay(message, emitter, stopped, inputRef));
            taskRef.set(task);
            if (stopped.get()) {
                task.cancel(true);
            }
        } catch (TaskRejectedException ex) {
            stopped.set(true);
            throw new RestClientException("流式服务繁忙，请稍后重试", ex);
        }

        return emitter;
    }

    private void relay(
            String message,
            SseEmitter emitter,
            AtomicBoolean stopped,
            AtomicReference<InputStream> inputRef) {

        StringBuilder answer = new StringBuilder();

        try {
            aiClient.streamChat(message, event -> {
                if (stopped.get() || Thread.currentThread().isInterrupted()) {
                    throw new CancellationException();
                }

                switch (event.event()) {
                    case "delta" -> {
                        var data = codec.readDelta(event.data());
                        if (answer.length() + data.content().length() > 2000) {
                            throw new RestClientException("累计回答超过长度限制");
                        }
                        answer.append(data.content());
                        send(emitter, stopped, "delta", data, false);
                    }
                    case "done" -> send(
                            emitter, stopped, "done",
                            codec.readDone(event.data(), answer.toString()), true);
                    case "error" -> send(
                            emitter, stopped, "error",
                            codec.readError(event.data()), true);
                    default -> throw new RestClientException("未知模型事件");
                }
            }, input -> {
                inputRef.set(input);
                if (stopped.get()) {
                    closeInput(inputRef);
                    throw new CancellationException();
                }
            });

            if (!stopped.get()) {
                throw new RestClientException("模型流缺少结束事件");
            }
        } catch (CancellationException ignored) {
        } catch (RuntimeException ex) {
            if (!stopped.get()) {
                log.warn("模型流式转发失败", ex);
                try {
                    send(emitter, stopped, "error", new AiStreamError(
                            "AI_SERVICE_UNAVAILABLE",
                            "模型服务暂时不可用，请稍后重试"), true);
                } catch (CancellationException ignored) {
                }
            }
        } finally {
            closeInput(inputRef);
        }
    }

    private void closeInput(AtomicReference<InputStream> inputRef) {
        InputStream input = inputRef.getAndSet(null);
        if (input != null) {
            try {
                input.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void send(
            SseEmitter emitter,
            AtomicBoolean stopped,
            String event,
            Object data,
            boolean terminal) {

        synchronized (emitter) {
            if (stopped.get()) {
                throw new CancellationException();
            }
            try {
                emitter.send(SseEmitter.event().name(event)
                        .data(data, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException ex) {
                stopped.set(true);
                throw new CancellationException("接收端连接已关闭");
            }

            if (terminal) {
                stopped.set(true);
                emitter.complete();
            }
        }
    }
}