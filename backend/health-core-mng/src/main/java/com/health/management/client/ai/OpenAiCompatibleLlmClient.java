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
 * OpenAI 兼容接口客户端。
 * 百炼：https://dashscope.aliyuncs.com/compatible-mode/v1
 * Ollama：http://localhost:11434/v1
 *
 * 不记录 prompt/answer 内容，避免健康隐私落入日志。
 */
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final String provider;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final Double temperature;
    private final Integer maxTokens;
    private final Boolean enableThinking;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiCompatibleLlmClient(String provider, String baseUrl, String apiKey, String model,
                                     Double temperature, Integer maxTokens, Integer timeoutSeconds,
                                     Boolean enableThinking) {
        this.provider = provider;
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.temperature = temperature == null ? 0.4 : temperature;
        this.maxTokens = maxTokens == null ? 1200 : maxTokens;
        this.enableThinking = enableThinking;

        int timeout = timeoutSeconds == null ? 60 : timeoutSeconds;
        if (timeout < 1) {
            timeout = 1;
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(timeout * 1000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public LlmResponse chat(List<LlmMessage> messages) {
        ensureConfigured();

        long start = System.currentTimeMillis();
        Map<String, Object> body = buildRequestBody(messages, false);
        HttpHeaders headers = buildHeaders(false);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = baseUrl + "/chat/completions";

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JsonNode.class);
            JsonNode root = response.getBody();
            if (root == null) {
                throw new LlmException(provider + " 返回为空");
            }
            if (root.has("error")) {
                String message = root.path("error").path("message").asText("");
                throw new LlmException(provider + " 返回错误：" + message);
            }

            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.size() == 0) {
                throw new LlmException(provider + " 未返回答案");
            }
            String content = choices.get(0).path("message").path("content").asText("");
            if (!ValidateUtils.hasText(content)) {
                throw new LlmException(provider + " 返回空答案");
            }

            LlmResponse result = new LlmResponse();
            result.setContent(content.trim());
            result.setProvider(provider);
            result.setModel(root.path("model").asText(model));
            result.setPromptTokens(root.path("usage").path("prompt_tokens").asInt(0));
            result.setCompletionTokens(root.path("usage").path("completion_tokens").asInt(0));
            result.setLatencyMs((int) (System.currentTimeMillis() - start));
            return result;
        } catch (HttpStatusCodeException e) {
            throw new LlmException(provider + " 调用失败（HTTP " + e.getStatusCode().value() + "）", e);
        } catch (ResourceAccessException e) {
            throw new LlmException(provider + " 调用超时或网络不可达", e);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException(provider + " 调用异常", e);
        }
    }

    @Override
    public LlmResponse streamChat(List<LlmMessage> messages, Consumer<String> onDelta) {
        ensureConfigured();

        long start = System.currentTimeMillis();
        Map<String, Object> body = buildRequestBody(messages, true);
        HttpHeaders headers = buildHeaders(true);
        String url = baseUrl + "/chat/completions";

        try {
            return restTemplate.execute(url, HttpMethod.POST, request -> {
                request.getHeaders().putAll(headers);
                byte[] bytes = objectMapper.writeValueAsBytes(body);
                request.getHeaders().setContentLength(bytes.length);
                request.getBody().write(bytes);
            }, response -> {
                HttpStatus status = response.getStatusCode();
                if (!status.is2xxSuccessful()) {
                    String error = readLimited(response.getBody(), 500);
                    throw new LlmException(provider + " 调用失败（HTTP " + status.value() + "）"
                            + (ValidateUtils.hasText(error) ? "：" + error : ""));
                }
                return readSse(response.getBody(), onDelta, start);
            });
        } catch (LlmException e) {
            throw e;
        } catch (StreamCancelledException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new LlmException(provider + " 调用超时或网络不可达", e);
        } catch (Exception e) {
            throw new LlmException(provider + " 调用异常", e);
        }
    }

    @Override
    public String name() {
        return provider;
    }

    private void ensureConfigured() {
        if (!ValidateUtils.hasText(baseUrl)) {
            throw new LlmException(provider + " 接口地址未配置");
        }
        if (!ValidateUtils.hasText(model)) {
            throw new LlmException(provider + " 模型名称未配置");
        }
    }

    private Map<String, Object> buildRequestBody(List<LlmMessage> messages, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("stream", stream);
        if (stream) {
            // 百炼/OpenAI 兼容模式：让最后一个 chunk 携带 usage，便于落库 token 统计
            Map<String, Object> streamOptions = new HashMap<>();
            streamOptions.put("include_usage", Boolean.TRUE);
            body.put("stream_options", streamOptions);
        }
        if (enableThinking != null) {
            // 百炼思考模型：显式关闭思考，避免 reasoning_content 占满 token 导致 content 为空
            body.put("enable_thinking", enableThinking);
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
        return body;
    }

    private HttpHeaders buildHeaders(boolean stream) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 部分网关对 Accept 较严格，流式请求使用 */*，避免 406
        headers.setAccept(stream
                ? Collections.singletonList(MediaType.ALL)
                : Collections.singletonList(MediaType.APPLICATION_JSON));
        if (ValidateUtils.hasText(apiKey)) {
            headers.setBearerAuth(apiKey);
        }
        return headers;
    }

    /**
     * 解析 OpenAI 兼容的 SSE：每行一个 data: {json}，以 data: [DONE] 结束。
     */
    private LlmResponse readSse(InputStream inputStream, Consumer<String> onDelta, long start) {
        StringBuilder content = new StringBuilder();
        int promptTokens = 0;
        int completionTokens = 0;
        String responseModel = model;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith(":")) {
                    continue;
                }
                if (!trimmed.startsWith("data:")) {
                    continue;
                }
                String data = trimmed.substring(5).trim();
                if (data.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(data)) {
                    break;
                }

                JsonNode root;
                try {
                    root = objectMapper.readTree(data);
                } catch (JsonProcessingException e) {
                    throw new LlmException(provider + " 流式响应格式错误", e);
                }
                if (root.has("error")) {
                    throw new LlmException(provider + " 返回错误："
                            + root.path("error").path("message").asText(""));
                }

                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    String delta = choices.get(0).path("delta").path("content").asText("");
                    if (!delta.isEmpty()) {
                        content.append(delta);
                        if (onDelta != null) {
                            onDelta.accept(delta);
                        }
                    }
                }

                JsonNode usage = root.path("usage");
                if (usage.isObject()) {
                    promptTokens = usage.path("prompt_tokens").asInt(promptTokens);
                    completionTokens = usage.path("completion_tokens").asInt(completionTokens);
                }
                if (root.hasNonNull("model")) {
                    responseModel = root.get("model").asText(responseModel);
                }
            }
        } catch (StreamCancelledException e) {
            throw e;
        } catch (LlmException e) {
            throw e;
        } catch (IOException e) {
            throw new LlmException(provider + " 流式读取失败", e);
        }

        if (!ValidateUtils.hasText(content.toString())) {
            throw new LlmException(provider + " 返回空答案");
        }

        LlmResponse result = new LlmResponse();
        result.setContent(content.toString().trim());
        result.setProvider(provider);
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

    private String normalizeBaseUrl(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
