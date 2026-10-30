package com.health.management.client.ai;

import com.health.management.common.util.ValidateUtils;

import java.util.List;
import java.util.function.Consumer;

/**
 * Mock 模型：未配置 API Key 时用于打通前后端链路。
 * 不生成任何医疗建议，避免演示时误把 Mock 内容当成真实 AI 回答。
 */
public class MockLlmClient implements LlmClient {

    private final String model;

    public MockLlmClient(String model) {
        this.model = ValidateUtils.hasText(model) ? model : "mock";
    }

    @Override
    public LlmResponse chat(List<LlmMessage> messages) {
        LlmResponse response = new LlmResponse();
        response.setContent("当前为 AI Mock 模式，尚未配置真实大模型。"
                + "你可以先完善健康档案、打卡记录和知识库素材，再切换到百炼或本地 Ollama。");
        response.setProvider("mock");
        response.setModel(model);
        response.setPromptTokens(0);
        response.setCompletionTokens(0);
        response.setLatencyMs(0);
        return response;
    }

    @Override
    public LlmResponse streamChat(List<LlmMessage> messages, Consumer<String> onDelta) {
        LlmResponse response = chat(messages);
        String content = response.getContent();
        if (content == null || onDelta == null) {
            return response;
        }
        // 按固定长度切块，方便本地联调时观察前端逐字渲染效果
        int chunkSize = 12;
        for (int i = 0; i < content.length(); i += chunkSize) {
            String chunk = content.substring(i, Math.min(i + chunkSize, content.length()));
            onDelta.accept(chunk);
            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return response;
    }

    @Override
    public String name() {
        return "mock";
    }
}
