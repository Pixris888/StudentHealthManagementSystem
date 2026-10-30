package com.health.management.client.ai;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FailoverLlmClientTest {

    @Test
    void fallsBackToSecondClient() {
        LlmClient primary = new LlmClient() {
            @Override
            public LlmResponse chat(List<LlmMessage> messages) {
                throw new LlmException("primary down");
            }

            @Override
            public String name() {
                return "bailian";
            }
        };
        LlmClient fallback = new LlmClient() {
            @Override
            public LlmResponse chat(List<LlmMessage> messages) {
                LlmResponse response = new LlmResponse();
                response.setContent("备用模型回答");
                response.setProvider("ollama");
                response.setModel("qwen3:0.6b");
                return response;
            }

            @Override
            public String name() {
                return "ollama";
            }
        };

        FailoverLlmClient client = new FailoverLlmClient(Arrays.asList(primary, fallback));
        LlmResponse response = client.chat(Collections.singletonList(LlmMessage.user("你好")));

        assertEquals("备用模型回答", response.getContent());
        assertEquals("ollama", response.getProvider());
        assertEquals("bailian：primary down；", response.getFallbackReason());
    }

    @Test
    void throwsWhenAllClientsFail() {
        LlmClient primary = new LlmClient() {
            @Override
            public LlmResponse chat(List<LlmMessage> messages) {
                throw new LlmException("down");
            }

            @Override
            public String name() {
                return "bailian";
            }
        };

        FailoverLlmClient client = new FailoverLlmClient(Collections.singletonList(primary));

        assertThrows(LlmException.class,
                () -> client.chat(Collections.singletonList(LlmMessage.user("你好"))));
    }
}
