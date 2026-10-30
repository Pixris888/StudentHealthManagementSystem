package com.health.management.config;

import com.health.management.client.ai.FailoverLlmClient;
import com.health.management.client.ai.LlmClient;
import com.health.management.client.ai.MockLlmClient;
import com.health.management.client.ai.OllamaLlmClient;
import com.health.management.client.ai.OpenAiCompatibleLlmClient;
import com.health.management.common.util.ValidateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 客户端装配。
 *
 * 主模型：health.ai.provider = bailian / ollama / mock
 * 备用模型：health.ai.fallback（默认 Ollama）
 *
 * 百炼 Key 为空时不会启动失败，会自动跳过百炼；如果 Ollama 也不可用，
 * 业务层会使用知识素材 + 规则模板兜底。
 */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    public LlmClient llmClient(AiProperties properties) {
        List<LlmClient> clients = new ArrayList<>();

        if (!properties.isEnabled() || "mock".equalsIgnoreCase(properties.getProvider())) {
            clients.add(new MockLlmClient(properties.getModel()));
            log.warn("AI 已启用 Mock 模式，不会调用真实大模型");
            return new FailoverLlmClient(clients);
        }

        if ("ollama".equalsIgnoreCase(properties.getProvider())) {
            // provider=ollama 时使用 fallback 节点作为本地模型配置
            LlmClient ollama = buildOllamaClient(properties.getFallback(), properties.getTemperature());
            addIfPresent(clients, ollama);
        } else {
            if (ValidateUtils.hasText(properties.getApiKey())) {
                LlmClient bailian = buildClient("bailian",
                        properties.getBaseUrl(), properties.getApiKey(), properties.getModel(),
                        properties.getTemperature(), properties.getMaxTokens(),
                        properties.getTimeoutSeconds(), properties.getEnableThinking());
                addIfPresent(clients, bailian);
            } else {
                log.warn("未配置 DASHSCOPE_API_KEY，百炼主模型将被跳过");
            }
        }

        AiProperties.Fallback fallback = properties.getFallback();
        if (fallback != null && fallback.isEnabled()
                && !"ollama".equalsIgnoreCase(properties.getProvider())) {
            LlmClient ollama = buildOllamaClient(fallback, properties.getTemperature());
            addIfPresent(clients, ollama);
        }

        if (clients.isEmpty()) {
            log.warn("未配置任何可用大模型，AI 问答将使用规则兜底");
        }
        return new FailoverLlmClient(clients);
    }

    private void addIfPresent(List<LlmClient> clients, LlmClient client) {
        if (client != null) {
            clients.add(client);
        }
    }

    private LlmClient buildClient(String provider, String baseUrl, String apiKey, String model,
                                  Double temperature, Integer maxTokens, Integer timeoutSeconds,
                                  Boolean enableThinking) {
        if (!ValidateUtils.hasText(baseUrl) || !ValidateUtils.hasText(model)) {
            log.warn("AI 模型 {} 配置不完整，已跳过", provider);
            return null;
        }
        return new OpenAiCompatibleLlmClient(
                provider, baseUrl, apiKey, model, temperature, maxTokens, timeoutSeconds, enableThinking);
    }

    private LlmClient buildOllamaClient(AiProperties.Fallback fallback, Double temperature) {
        if (fallback == null
                || !ValidateUtils.hasText(fallback.getBaseUrl())
                || !ValidateUtils.hasText(fallback.getModel())) {
            log.warn("Ollama 配置不完整，已跳过");
            return null;
        }
        return new OllamaLlmClient(
                fallback.getBaseUrl(), fallback.getModel(), temperature,
                fallback.getMaxTokens(), fallback.getTimeoutSeconds());
    }
}
