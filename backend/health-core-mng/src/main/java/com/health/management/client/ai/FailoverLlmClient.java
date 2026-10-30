package com.health.management.client.ai;

import com.health.management.common.util.ValidateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 按顺序尝试多个模型客户端：百炼 → Ollama。
 * 全部失败时抛出异常，由业务层使用规则兜底。
 */
public class FailoverLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(FailoverLlmClient.class);

    private final List<LlmClient> clients;

    public FailoverLlmClient(List<LlmClient> clients) {
        this.clients = clients == null
                ? Collections.emptyList()
                : new ArrayList<>(clients);
    }

    @Override
    public LlmResponse chat(List<LlmMessage> messages) {
        if (clients.isEmpty()) {
            throw new LlmException("未配置可用的大模型");
        }
        LlmException lastException = null;
        StringBuilder errors = new StringBuilder();
        for (LlmClient client : clients) {
            try {
                LlmResponse response = client.chat(messages);
                if (response != null && ValidateUtils.hasText(response.getContent())) {
                    if (errors.length() > 0) {
                        response.setFallbackReason(errors.toString());
                    }
                    return response;
                }
                lastException = new LlmException(client.name() + " 返回空答案");
                errors.append(client.name()).append("：返回空答案；");
            } catch (Exception e) {
                lastException = e instanceof LlmException
                        ? (LlmException) e
                        : new LlmException(client.name() + " 调用失败", e);
                errors.append(client.name()).append("：").append(e.getMessage()).append("；");
                // 只记录提供方和错误摘要，不记录 prompt
                log.warn("AI 模型 {} 调用失败：{}", client.name(), e.getMessage());
            }
        }
        throw new LlmException(errors.length() == 0 ? "未配置可用的大模型" : errors.toString(),
                lastException);
    }

    /**
     * 流式降级规则：
     * 1. 只有在前一个模型还没有输出任何内容时，才切换到下一个模型；
     * 2. 已经输出部分内容后失败，直接抛出，避免两个模型的回答拼在一起；
     * 3. 前端取消（StreamCancelledException）立即向上抛出，不触发降级。
     */
    @Override
    public LlmResponse streamChat(List<LlmMessage> messages, Consumer<String> onDelta) {
        if (clients.isEmpty()) {
            throw new LlmException("未配置可用的大模型");
        }
        LlmException lastException = null;
        StringBuilder errors = new StringBuilder();
        for (LlmClient client : clients) {
            AtomicBoolean committed = new AtomicBoolean(false);
            try {
                LlmResponse response = client.streamChat(messages, delta -> {
                    if (ValidateUtils.hasText(delta)) {
                        committed.set(true);
                        if (onDelta != null) {
                            onDelta.accept(delta);
                        }
                    }
                });
                if (response != null && ValidateUtils.hasText(response.getContent())) {
                    if (!committed.get() && onDelta != null) {
                        onDelta.accept(response.getContent());
                    }
                    if (errors.length() > 0) {
                        response.setFallbackReason(errors.toString());
                    }
                    return response;
                }
                if (committed.get()) {
                    throw new LlmException(client.name() + " 流式返回中断");
                }
                lastException = new LlmException(client.name() + " 返回空答案");
                errors.append(client.name()).append("：返回空答案；");
            } catch (StreamCancelledException e) {
                throw e;
            } catch (Exception e) {
                if (committed.get()) {
                    // 已经向前端输出了部分内容，不能再换模型拼接
                    throw e instanceof LlmException
                            ? (LlmException) e
                            : new LlmException(client.name() + " 流式调用中断", e);
                }
                lastException = e instanceof LlmException
                        ? (LlmException) e
                        : new LlmException(client.name() + " 调用失败", e);
                errors.append(client.name()).append("：").append(e.getMessage()).append("；");
                // 只记录提供方和错误摘要，不记录 prompt
                log.warn("AI 模型 {} 流式调用失败：{}", client.name(), e.getMessage());
            }
        }
        throw new LlmException(errors.length() == 0 ? "未配置可用的大模型" : errors.toString(),
                lastException);
    }

    @Override
    public String name() {
        return "failover";
    }
}
