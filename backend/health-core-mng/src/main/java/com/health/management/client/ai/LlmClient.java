package com.health.management.client.ai;

import java.util.List;
import java.util.function.Consumer;

/**
 * 大模型客户端抽象。
 * 百炼、Ollama、Mock 共用该接口，业务层不感知具体供应商。
 */
public interface LlmClient {

    LlmResponse chat(List<LlmMessage> messages);

    /**
     * 流式对话：每收到一段增量内容就回调一次 onDelta，最终返回聚合后的完整结果。
     *
     * 默认实现会退化为同步调用，把完整答案一次性回调，保证旧客户端无需改动。
     * 如果前端断开连接，onDelta 会抛出 {@link StreamCancelledException}，
     * 具体客户端应停止读取并向上抛出，不要包装成普通模型故障。
     */
    default LlmResponse streamChat(List<LlmMessage> messages, Consumer<String> onDelta) {
        LlmResponse response = chat(messages);
        if (response != null && response.getContent() != null
                && !response.getContent().isEmpty() && onDelta != null) {
            onDelta.accept(response.getContent());
        }
        return response;
    }

    String name();
}
