package com.health.management.controller.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.management.client.ai.StreamCancelledException;
import com.health.management.service.ai.AiStreamSink;
import com.health.management.vo.ai.AiAnswerVO;
import com.health.management.vo.ai.AiStreamDeltaVO;
import com.health.management.vo.ai.AiStreamErrorVO;
import com.health.management.vo.ai.AiStreamMetaVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 把业务层的流式事件写成 SSE。
 *
 * 事件约定：
 * - meta：会话 ID、问答记录 ID、知识来源，首字前先返回；
 * - delta：增量文本；
 * - done：完整答案与模型/统计信息，终止事件；
 * - error：错误信息，终止事件；
 * - 心跳使用 SSE 注释 ": ping"，不产生业务事件。
 */
public class AiSseStreamSink implements AiStreamSink {

    private static final Logger log = LoggerFactory.getLogger(AiSseStreamSink.class);

    private final SseEmitter emitter;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService heartbeatScheduler;
    private final long heartbeatMillis;
    private final Object sendLock = new Object();
    private final Object heartbeatLock = new Object();
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private volatile ScheduledFuture<?> heartbeatFuture;

    public AiSseStreamSink(long timeoutMillis, long heartbeatMillis,
                           ScheduledExecutorService heartbeatScheduler, ObjectMapper objectMapper) {
        this.emitter = new SseEmitter(Math.max(10_000L, timeoutMillis));
        this.heartbeatMillis = heartbeatMillis;
        this.heartbeatScheduler = heartbeatScheduler;
        this.objectMapper = objectMapper;

        this.emitter.onCompletion(() -> {
            completed.set(true);
            cancelHeartbeat();
        });
        this.emitter.onTimeout(() -> {
            cancelled.set(true);
            completed.set(true);
            cancelHeartbeat();
        });
        this.emitter.onError(e -> {
            cancelled.set(true);
            completed.set(true);
            cancelHeartbeat();
            log.debug("AI 流式连接异常：{}", e.getMessage());
        });
        startHeartbeat();
    }

    public SseEmitter getEmitter() {
        return emitter;
    }

    @Override
    public void sendMeta(AiStreamMetaVO meta) {
        send("meta", meta);
    }

    @Override
    public void sendDelta(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        send("delta", new AiStreamDeltaVO(content));
    }

    @Override
    public void sendDone(AiAnswerVO answer) {
        try {
            send("done", answer);
        } catch (StreamCancelledException e) {
            // 客户端已断开，答案已完整落库，无需再报错
            log.debug("AI 流式完成事件发送失败：{}", e.getMessage());
        } finally {
            complete();
        }
    }

    @Override
    public void sendError(Integer code, String message, Long consultationId, boolean partial) {
        AiStreamErrorVO error = new AiStreamErrorVO();
        error.setCode(code);
        error.setMessage(message);
        error.setConsultationId(consultationId);
        error.setPartial(partial);
        try {
            send("error", error);
        } catch (StreamCancelledException e) {
            log.debug("AI 流式错误事件发送失败：{}", e.getMessage());
        } finally {
            complete();
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    private void send(String event, Object data) {
        if (completed.get() || cancelled.get()) {
            throw new StreamCancelledException("客户端已断开连接");
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("SSE 数据序列化失败", e);
        }

        synchronized (sendLock) {
            if (completed.get() || cancelled.get()) {
                throw new StreamCancelledException("客户端已断开连接");
            }
            try {
                emitter.send(SseEmitter.event().name(event).data(json));
            } catch (IOException | IllegalStateException e) {
                cancelled.set(true);
                throw new StreamCancelledException("SSE 发送失败", e);
            }
        }
    }

    /**
     * 幂等结束响应。业务层正常发送 done/error 时会调用；Controller 的 finally 再兜底调用一次。
     */
    public void complete() {
        if (!completed.compareAndSet(false, true)) {
            return;
        }
        cancelHeartbeat();
        try {
            emitter.complete();
        } catch (Exception e) {
            log.debug("AI 流式响应完成失败：{}", e.getMessage());
        }
    }

    private void startHeartbeat() {
        if (heartbeatMillis <= 0) {
            return;
        }
        synchronized (heartbeatLock) {
            if (completed.get() || cancelled.get()) {
                return;
            }
            try {
                heartbeatFuture = heartbeatScheduler.scheduleAtFixedRate(
                        this::heartbeat, heartbeatMillis, heartbeatMillis, TimeUnit.MILLISECONDS);
            } catch (RuntimeException e) {
                log.warn("AI 流式心跳启动失败：{}", e.getMessage());
            }
        }
    }

    private void heartbeat() {
        if (completed.get() || cancelled.get()) {
            cancelHeartbeat();
            return;
        }
        synchronized (sendLock) {
            if (completed.get() || cancelled.get()) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (Exception e) {
                cancelled.set(true);
                cancelHeartbeat();
            }
        }
    }

    private void cancelHeartbeat() {
        synchronized (heartbeatLock) {
            ScheduledFuture<?> future = heartbeatFuture;
            if (future != null) {
                future.cancel(false);
                heartbeatFuture = null;
            }
        }
    }
}
