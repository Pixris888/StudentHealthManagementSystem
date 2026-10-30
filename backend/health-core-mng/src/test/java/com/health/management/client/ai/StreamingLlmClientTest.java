package com.health.management.client.ai;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamingLlmClientTest {

    private final List<HttpServer> servers = new ArrayList<>();

    @AfterEach
    void stopServers() {
        for (HttpServer server : servers) {
            server.stop(0);
        }
        servers.clear();
    }

    @Test
    void openAiStreamParsesSseAndUsage() throws IOException {
        String sse = "data: {\"model\":\"test-model\",\"choices\":[{\"delta\":{\"role\":\"assistant\"}}],\"usage\":null}\n\n"
                + "data: {\"model\":\"test-model\",\"choices\":[{\"delta\":{\"content\":\"你好\"}}],\"usage\":null}\n\n"
                + "data: {\"model\":\"test-model\",\"choices\":[{\"delta\":{\"content\":\"，世界\"}}],\"usage\":null}\n\n"
                + "data: {\"model\":\"test-model\",\"choices\":[{\"delta\":{\"content\":\"\"},\"finish_reason\":\"stop\"}],\"usage\":null}\n\n"
                + "data: {\"model\":\"test-model\",\"choices\":[],\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":5}}\n\n"
                + "data: [DONE]\n\n";
        String baseUrl = startServer("/chat/completions", "text/event-stream", sse);

        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(
                "bailian", baseUrl, "test-key", "test-model",
                0.4, 100, 10, false);
        List<String> deltas = new ArrayList<>();
        LlmResponse response = client.streamChat(
                Collections.singletonList(LlmMessage.user("你好")), deltas::add);

        assertEquals("你好，世界", response.getContent());
        assertEquals(Arrays.asList("你好", "，世界"), deltas);
        assertEquals(Integer.valueOf(12), response.getPromptTokens());
        assertEquals(Integer.valueOf(5), response.getCompletionTokens());
        assertEquals("bailian", response.getProvider());
        assertEquals("test-model", response.getModel());
    }

    @Test
    void ollamaStreamParsesNdjson() throws IOException {
        String ndjson = "{\"model\":\"qwen3:0.6b\",\"message\":{\"role\":\"assistant\",\"content\":\"第一段\"},\"done\":false}\n"
                + "{\"model\":\"qwen3:0.6b\",\"message\":{\"role\":\"assistant\",\"content\":\"第二段\"},\"done\":false}\n"
                + "{\"model\":\"qwen3:0.6b\",\"message\":{\"role\":\"assistant\",\"content\":\"\"},\"done\":true,"
                + "\"prompt_eval_count\":8,\"eval_count\":4}\n";
        String baseUrl = startServer("/api/chat", "application/x-ndjson", ndjson);

        OllamaLlmClient client = new OllamaLlmClient(
                baseUrl, "qwen3:0.6b", 0.4, 100, 10);
        List<String> deltas = new ArrayList<>();
        LlmResponse response = client.streamChat(
                Collections.singletonList(LlmMessage.user("你好")), deltas::add);

        assertEquals("第一段第二段", response.getContent());
        assertEquals(Arrays.asList("第一段", "第二段"), deltas);
        assertEquals(Integer.valueOf(8), response.getPromptTokens());
        assertEquals(Integer.valueOf(4), response.getCompletionTokens());
        assertEquals("ollama", response.getProvider());
    }

    @Test
    void failoverStreamsFromFallbackWhenPrimaryFailsBeforeFirstToken() {
        LlmClient primary = fakeClient("bailian", (messages, onDelta) -> {
            throw new LlmException("primary down");
        });
        LlmClient fallback = fakeClient("ollama", (messages, onDelta) -> {
            onDelta.accept("备用模型回答");
            return response("备用模型回答", "ollama", "qwen3:0.6b");
        });

        FailoverLlmClient client = new FailoverLlmClient(Arrays.asList(primary, fallback));
        List<String> deltas = new ArrayList<>();
        LlmResponse response = client.streamChat(
                Collections.singletonList(LlmMessage.user("你好")), deltas::add);

        assertEquals("备用模型回答", response.getContent());
        assertEquals(Collections.singletonList("备用模型回答"), deltas);
        assertEquals("ollama", response.getProvider());
        assertTrue(response.getFallbackReason().contains("primary down"));
    }

    @Test
    void failoverDoesNotSwitchAfterPartialOutput() {
        LlmClient primary = fakeClient("bailian", (messages, onDelta) -> {
            onDelta.accept("已经输出的部分");
            throw new LlmException("stream broken");
        });
        AtomicInteger fallbackCalls = new AtomicInteger();
        LlmClient fallback = fakeClient("ollama", (messages, onDelta) -> {
            fallbackCalls.incrementAndGet();
            onDelta.accept("不应该出现");
            return response("不应该出现", "ollama", "qwen3:0.6b");
        });

        FailoverLlmClient client = new FailoverLlmClient(Arrays.asList(primary, fallback));
        List<String> deltas = new ArrayList<>();

        assertThrows(LlmException.class,
                () -> client.streamChat(Collections.singletonList(LlmMessage.user("你好")), deltas::add));
        assertEquals(Collections.singletonList("已经输出的部分"), deltas);
        assertEquals(0, fallbackCalls.get());
    }

    @Test
    void cancellationPropagatesWithoutFailover() {
        LlmClient primary = fakeClient("bailian", (messages, onDelta) -> {
            onDelta.accept("开始输出");
            return response("开始输出", "bailian", "test-model");
        });
        AtomicInteger fallbackCalls = new AtomicInteger();
        LlmClient fallback = fakeClient("ollama", (messages, onDelta) -> {
            fallbackCalls.incrementAndGet();
            return response("备用", "ollama", "qwen3:0.6b");
        });

        FailoverLlmClient client = new FailoverLlmClient(Arrays.asList(primary, fallback));
        assertThrows(StreamCancelledException.class,
                () -> client.streamChat(Collections.singletonList(LlmMessage.user("你好")), delta -> {
                    throw new StreamCancelledException("客户端断开");
                }));
        assertEquals(0, fallbackCalls.get());
    }

    private LlmClient fakeClient(String name, FakeStream stream) {
        return new LlmClient() {
            @Override
            public LlmResponse chat(List<LlmMessage> messages) {
                return stream.stream(messages, delta -> {
                });
            }

            @Override
            public LlmResponse streamChat(List<LlmMessage> messages, Consumer<String> onDelta) {
                return stream.stream(messages, onDelta);
            }

            @Override
            public String name() {
                return name;
            }
        };
    }

    private LlmResponse response(String content, String provider, String model) {
        LlmResponse response = new LlmResponse();
        response.setContent(content);
        response.setProvider(provider);
        response.setModel(model);
        return response;
    }

    private String startServer(String path, String contentType, String body) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(path, exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
                output.flush();
            }
        });
        server.start();
        servers.add(server);
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @FunctionalInterface
    private interface FakeStream {
        LlmResponse stream(List<LlmMessage> messages, Consumer<String> onDelta);
    }
}
