package com.health.management.client.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.management.common.util.ValidateUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Ollama 原生 /api/chat 客户端。
 *
 * 为什么不走 OpenAI 兼容接口：
 * Qwen3 等思考模型在 Ollama 的 OpenAI 兼容模式下会把内容放进 message.reasoning，
 * message.content 可能为空；原生接口支持 think=false，可以关闭思考，直接拿正文。
 */
public class OllamaLlmClient implements LlmClient {

    private final String baseUrl;
    private final String model;
    private final Double temperature;
    private final Integer maxTokens;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaLlmClient(String baseUrl, String model, Double temperature,
                           Integer maxTokens, Integer timeoutSeconds) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.model = model;
        this.temperature = temperature == null ? 0.4 : temperature;
        this.maxTokens = maxTokens == null ? 800 : maxTokens;

        int timeout = timeoutSeconds == null ? 120 : timeoutSeconds;
        if (timeout < 1) {
            timeout = 1;
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(timeout * 1000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public LlmResponse chat(List<LlmMessage> messages) {
        ensureConfigured();

        long start = System.currentTimeMillis();
        Map<String, Object> body = buildRequestBody(messages, false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + "/api/chat", HttpMethod.POST, entity, JsonNode.class);
            JsonNode root = response.getBody();
            if (root == null) {
                throw new LlmException("Ollama 返回为空");
            }
            if (root.has("error")) {
                throw new LlmException("Ollama 返回错误：" + root.path("error").asText(""));
            }
            JsonNode message = root.path("message");
            String content = message.path("content").asText("");
            if (!ValidateUtils.hasText(content)) {
                throw new LlmException("Ollama 返回空答案");
            }

            LlmResponse result = new LlmResponse();
            result.setContent(content.trim());
            result.setProvider("ollama");
            result.setModel(root.path("model").asText(model));
            result.setPromptTokens(root.path("prompt_eval_count").asInt(0));
            result.setCompletionTokens(root.path("eval_count").asInt(0));
            result.setLatencyMs((int) (System.currentTimeMillis() - start));
            return result;
        } catch (HttpStatusCodeException e) {
            throw new LlmException("Ollama 调用失败（HTTP " + e.getStatusCode().value() + "）", e);
        } catch (ResourceAccessException e) {
            throw new LlmException("Ollama 调用超时或未启动", e);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("Ollama 调用异常", e);
        }
    }

    @Override
    public LlmResponse streamChat(List<LlmMessage> messages, Consumer<String> onDelta) {
        ensureConfigured();

        long start = System.currentTimeMillis();
        Map<String, Object> body = buildRequestBody(messages, true);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.ALL));

        try {
            return restTemplate.execute(baseUrl + "/api/chat", HttpMethod.POST, request -> {
                request.getHeaders().putAll(headers);
                byte[] bytes = objectMapper.writeValueAsBytes(body);
                request.getHeaders().setContentLength(bytes.length);
                request.getBody().write(bytes);
            }, response -> {
                HttpStatus status = response.getStatusCode();
                if (!status.is2xxSuccessful()) {
                    String error = readLimited(response.getBody(), 500);
                    throw new LlmException("Ollama 调用失败（HTTP " + status.value() + "）"
                            + (ValidateUtils.hasText(error) ? "：" + error : ""));
                }
                return readNdjson(response.getBody(), onDelta, start);
            });
        } catch (LlmException e) {
            throw e;
        } catch (StreamCancelledException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new LlmException("Ollama 调用超时或未启动", e);
        } catch (Exception e) {
            throw new LlmException("Ollama 调用异常", e);
        }
    }

    @Override
    public String name() {
        return "ollama";
    }

    private void ensureConfigured() {
        if (!ValidateUtils.hasText(baseUrl)) {
            throw new LlmException("Ollama 接口地址未配置");
        }
        if (!ValidateUtils.hasText(model)) {
            throw new LlmException("Ollama 模型名称未配置");
        }
    }

    private Map<String, Object> buildRequestBody(List<LlmMessage> messages, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("stream", stream);
        if (supportsThinkingSwitch(model)) {
            // 健康问答不需要展示思考过程，关闭后响应更快、正文更稳定
            body.put("think", Boolean.FALSE);
        }

        List<Map<String, String>> messageList = new ArrayList<>();
        if (messages != null) {
            for (LlmMessage message : messages) {
                if (message == null) {
                    continue;
                }
                Map<String, String> item = new HashMap<>();
                item.put("role", message.getRole());
                item.put("content", message.getContent());
                messageList.add(item);
            }
        }
        body.put("messages", messageList);

        Map<String, Object> options = new HashMap<>();
        options.put("temperature", temperature);
        options.put("num_predict", maxTokens);
        // RAG 上下文可能超过 Ollama 默认 4K，显式给 8K 避免 Prompt 被截断
        options.put("num_ctx", 8192);
        body.put("options", options);
        return body;
    }

    /**
     * 解析 Ollama 原生 NDJSON：每行一个 JSON，最后一行 done=true，并带 token 统计。
     */
    private LlmResponse readNdjson(InputStream inputStream, Consumer<String> onDelta, long start) {
        StringBuilder content = new StringBuilder();
        int promptTokens = 0;
        int completionTokens = 0;
        String responseModel = model;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                JsonNode root;
                try {
                    root = objectMapper.readTree(trimmed);
                } catch (JsonProcessingException e) {
                    throw new LlmException("Ollama 流式响应格式错误", e);
                }
                if (root.has("error")) {
                    throw new LlmException("Ollama 返回错误：" + root.path("error").asText(""));
                }

                String delta = root.path("message").path("content").asText("");
                if (!delta.isEmpty()) {
                    content.append(delta);
                    if (onDelta != null) {
                        onDelta.accept(delta);
                    }
                }
                if (root.hasNonNull("model")) {
                    responseModel = root.get("model").asText(responseModel);
                }
                if (root.path("done").asBoolean(false)) {
                    promptTokens = root.path("prompt_eval_count").asInt(promptTokens);
                    completionTokens = root.path("eval_count").asInt(completionTokens);
                    break;
                }
            }
        } catch (StreamCancelledException e) {
            throw e;
        } catch (LlmException e) {
            throw e;
        } catch (IOException e) {
            throw new LlmException("Ollama 流式读取失败", e);
        }

        if (!ValidateUtils.hasText(content.toString())) {
            throw new LlmException("Ollama 返回空答案");
        }

        LlmResponse result = new LlmResponse();
        result.setContent(content.toString().trim());
        result.setProvider("ollama");
        result.setModel(responseModel);
        result.setPromptTokens(promptTokens);
        result.setCompletionTokens(completionTokens);
        result.setLatencyMs((int) (System.currentTimeMillis() - start));
        return result;
    }

    private String readLimited(InputStream inputStream, int maxLength) {
        if (inputStream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[maxLength];
            int read = reader.read(buffer);
            return read <= 0 ? "" : new String(buffer, 0, read);
        } catch (IOException e) {
            return "";
        }
    }

    private boolean supportsThinkingSwitch(String modelName) {
        if (modelName == null) {
            return false;
        }
        String lower = modelName.toLowerCase();
        return lower.contains("qwen3") || lower.contains("qwq")
                || lower.contains("deepseek-r1") || lower.contains("thinking");
    }

    private String normalizeBaseUrl(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        // 兼容用户误填 OpenAI 兼容地址 http://localhost:11434/v1
        if (trimmed.endsWith("/v1")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed;
    }
}
