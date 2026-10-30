package com.health.management.service.impl;

import com.health.management.client.ai.LlmClient;
import com.health.management.client.ai.LlmException;
import com.health.management.client.ai.LlmResponse;
import com.health.management.client.ai.StreamCancelledException;
import com.health.management.config.AiProperties;
import com.health.management.dto.ai.AiAskDTO;
import com.health.management.entity.AiConsultation;
import com.health.management.entity.HealthKnowledgeMaterial;
import com.health.management.mapper.AiConsultationMapper;
import com.health.management.service.ai.AiContextBuilder;
import com.health.management.service.ai.AiFallbackService;
import com.health.management.service.ai.AiHealthContext;
import com.health.management.service.ai.AiKnowledgeRetriever;
import com.health.management.service.ai.AiPromptBuilder;
import com.health.management.service.ai.AiRateLimiter;
import com.health.management.service.ai.AiRetrievedMaterial;
import com.health.management.service.ai.AiSafetyGuard;
import com.health.management.service.ai.AiStreamSink;
import com.health.management.service.ai.AiUserInfoSupport;
import com.health.management.vo.ai.AiAnswerVO;
import com.health.management.vo.ai.AiStreamMetaVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiConsultationStreamServiceTest {

    @Test
    void askStreamEmitsMetaDeltaDoneAndPersists() {
        AiProperties properties = new AiProperties();
        properties.setProvider("bailian");

        AiContextBuilder contextBuilder = mock(AiContextBuilder.class);
        when(contextBuilder.build(1L)).thenReturn(context());
        AiKnowledgeRetriever retriever = mock(AiKnowledgeRetriever.class);
        when(retriever.retrieve(anyString(), anyInt()))
                .thenReturn(Collections.singletonList(new AiRetrievedMaterial(material(), 12)));

        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.streamChat(anyList(), any())).thenAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(1);
            onDelta.accept("第一段");
            onDelta.accept("第二段");
            LlmResponse response = new LlmResponse();
            response.setContent("第一段第二段");
            response.setProvider("bailian");
            response.setModel("test-model");
            response.setPromptTokens(10);
            response.setCompletionTokens(5);
            return response;
        });

        AiSafetyGuard safetyGuard = new AiSafetyGuard();
        AiFallbackService fallbackService = new AiFallbackService();
        ReflectionTestUtils.setField(fallbackService, "aiSafetyGuard", safetyGuard);

        AiConsultationServiceImpl service = service(llmClient, contextBuilder, retriever,
                properties, safetyGuard, fallbackService);
        stubPersistence(service);

        CapturingSink sink = new CapturingSink();
        service.askStream(dto(), 1L, sink);

        assertNotNull(sink.meta);
        assertEquals(Long.valueOf(100L), sink.meta.getConsultationId());
        assertEquals(3, sink.deltas.size());
        assertEquals("第一段", sink.deltas.get(0));
        assertEquals("第二段", sink.deltas.get(1));
        assertTrue(sink.deltas.get(2).contains("不能替代医生诊断"));

        assertNotNull(sink.done);
        assertEquals(Integer.valueOf(1), sink.done.getAnswerStatus());
        assertFalse(sink.done.getFallback());
        assertEquals("bailian", sink.done.getProvider());
        assertTrue(sink.done.getAnswer().contains("第一段第二段"));
        assertTrue(sink.done.getAnswer().contains("不能替代医生诊断"));
        assertEquals("《成年人推荐睡眠时长与熬夜危害》（中国居民膳食指南（2022））",
                sink.done.getSources().get(0));

        ArgumentCaptor<AiConsultation> captor = ArgumentCaptor.forClass(AiConsultation.class);
        verify(service).updateById(captor.capture());
        AiConsultation updated = captor.getValue();
        assertEquals(Long.valueOf(100L), updated.getId());
        assertEquals(Integer.valueOf(1), updated.getAnswerStatus());
        assertEquals("bailian", updated.getProvider());
        assertEquals("test-model", updated.getModelName());
        assertEquals(Integer.valueOf(10), updated.getPromptTokens());
        assertEquals(Integer.valueOf(5), updated.getCompletionTokens());
        assertEquals("1", updated.getRetrievedIds());
        assertEquals(Long.valueOf(1L), updated.getUserId());
        assertEquals(Long.valueOf(1L), updated.getUpdateBy());
    }

    @Test
    void askStreamFallsBackToRuleWhenModelFailsBeforeFirstToken() {
        AiProperties properties = new AiProperties();
        properties.setProvider("bailian");

        AiContextBuilder contextBuilder = mock(AiContextBuilder.class);
        when(contextBuilder.build(1L)).thenReturn(context());
        AiKnowledgeRetriever retriever = mock(AiKnowledgeRetriever.class);
        when(retriever.retrieve(anyString(), anyInt()))
                .thenReturn(Collections.singletonList(new AiRetrievedMaterial(material(), 12)));

        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.streamChat(anyList(), any())).thenThrow(new LlmException("model down"));

        AiSafetyGuard safetyGuard = new AiSafetyGuard();
        AiFallbackService fallbackService = new AiFallbackService();
        ReflectionTestUtils.setField(fallbackService, "aiSafetyGuard", safetyGuard);

        AiConsultationServiceImpl service = service(llmClient, contextBuilder, retriever,
                properties, safetyGuard, fallbackService);
        stubPersistence(service);

        CapturingSink sink = new CapturingSink();
        service.askStream(dto(), 1L, sink);

        assertNotNull(sink.done);
        assertEquals(Integer.valueOf(3), sink.done.getAnswerStatus());
        assertTrue(sink.done.getFallback());
        assertEquals("rule", sink.done.getProvider());
        assertEquals(1, sink.deltas.size());
        assertTrue(sink.deltas.get(0).contains("当前大模型服务暂时不可用"));

        ArgumentCaptor<AiConsultation> captor = ArgumentCaptor.forClass(AiConsultation.class);
        verify(service).updateById(captor.capture());
        AiConsultation updated = captor.getValue();
        assertEquals(Integer.valueOf(3), updated.getAnswerStatus());
        assertEquals("rule", updated.getProvider());
        assertEquals("rule", updated.getModelName());
        assertTrue(updated.getErrorMsg().contains("模型调用失败"));
    }

    @Test
    void askStreamKeepsPartialAnswerAndSendsErrorWhenModelBreaksMidway() {
        AiProperties properties = new AiProperties();
        properties.setProvider("bailian");

        AiContextBuilder contextBuilder = mock(AiContextBuilder.class);
        when(contextBuilder.build(1L)).thenReturn(context());
        AiKnowledgeRetriever retriever = mock(AiKnowledgeRetriever.class);
        when(retriever.retrieve(anyString(), anyInt()))
                .thenReturn(Collections.singletonList(new AiRetrievedMaterial(material(), 12)));

        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.streamChat(anyList(), any())).thenAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(1);
            onDelta.accept("已经生成的部分");
            throw new LlmException("stream broken");
        });

        AiSafetyGuard safetyGuard = new AiSafetyGuard();
        AiFallbackService fallbackService = new AiFallbackService();
        ReflectionTestUtils.setField(fallbackService, "aiSafetyGuard", safetyGuard);

        AiConsultationServiceImpl service = service(llmClient, contextBuilder, retriever,
                properties, safetyGuard, fallbackService);
        stubPersistence(service);

        CapturingSink sink = new CapturingSink();
        service.askStream(dto(), 1L, sink);

        assertNull(sink.done);
        assertNotNull(sink.errorCode);
        assertTrue(sink.partial);
        assertEquals(1, sink.deltas.size());
        assertEquals("已经生成的部分", sink.deltas.get(0));
        assertNotNull(sink.errorConsultationId);

        ArgumentCaptor<AiConsultation> captor = ArgumentCaptor.forClass(AiConsultation.class);
        verify(service).updateById(captor.capture());
        AiConsultation updated = captor.getValue();
        assertEquals(Integer.valueOf(2), updated.getAnswerStatus());
        assertEquals("已经生成的部分", updated.getAnswer());
        assertTrue(updated.getErrorMsg().contains("回答生成中断"));
    }

    @Test
    void askStreamMarksInterruptedWhenClientDisconnects() {
        AiProperties properties = new AiProperties();
        properties.setProvider("bailian");

        AiContextBuilder contextBuilder = mock(AiContextBuilder.class);
        when(contextBuilder.build(1L)).thenReturn(context());
        AiKnowledgeRetriever retriever = mock(AiKnowledgeRetriever.class);
        when(retriever.retrieve(anyString(), anyInt()))
                .thenReturn(Collections.singletonList(new AiRetrievedMaterial(material(), 12)));

        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.streamChat(anyList(), any())).thenAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(1);
            onDelta.accept("第一段");
            LlmResponse response = new LlmResponse();
            response.setContent("第一段");
            response.setProvider("bailian");
            response.setModel("test-model");
            return response;
        });

        AiSafetyGuard safetyGuard = new AiSafetyGuard();
        AiFallbackService fallbackService = new AiFallbackService();
        ReflectionTestUtils.setField(fallbackService, "aiSafetyGuard", safetyGuard);

        AiConsultationServiceImpl service = service(llmClient, contextBuilder, retriever,
                properties, safetyGuard, fallbackService);
        stubPersistence(service);

        CapturingSink sink = new CapturingSink();
        sink.cancelOnDelta = true;
        service.askStream(dto(), 1L, sink);

        assertNull(sink.done);
        assertNull(sink.errorCode);
        ArgumentCaptor<AiConsultation> captor = ArgumentCaptor.forClass(AiConsultation.class);
        verify(service).updateById(captor.capture());
        AiConsultation updated = captor.getValue();
        assertEquals(Integer.valueOf(2), updated.getAnswerStatus());
        assertEquals("客户端断开连接", updated.getErrorMsg());
    }

    private AiConsultationServiceImpl service(LlmClient llmClient, AiContextBuilder contextBuilder,
                                              AiKnowledgeRetriever retriever, AiProperties properties,
                                              AiSafetyGuard safetyGuard, AiFallbackService fallbackService) {
        AiConsultationMapper mapper = mock(AiConsultationMapper.class);
        when(mapper.selectList(any())).thenReturn(Collections.emptyList());

        AiConsultationServiceImpl service = spy(new AiConsultationServiceImpl());
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "llmClient", llmClient);
        ReflectionTestUtils.setField(service, "aiProperties", properties);
        ReflectionTestUtils.setField(service, "aiContextBuilder", contextBuilder);
        ReflectionTestUtils.setField(service, "aiKnowledgeRetriever", retriever);
        ReflectionTestUtils.setField(service, "aiPromptBuilder", new AiPromptBuilder());
        ReflectionTestUtils.setField(service, "aiSafetyGuard", safetyGuard);
        ReflectionTestUtils.setField(service, "aiFallbackService", fallbackService);
        ReflectionTestUtils.setField(service, "aiRateLimiter", mock(AiRateLimiter.class));
        ReflectionTestUtils.setField(service, "aiUserInfoSupport", mock(AiUserInfoSupport.class));
        return service;
    }

    private void stubPersistence(AiConsultationServiceImpl service) {
        doAnswer(invocation -> {
            AiConsultation entity = invocation.getArgument(0);
            entity.setId(100L);
            return true;
        }).when(service).save(any(AiConsultation.class));
        doReturn(true).when(service).updateById(any(AiConsultation.class));
    }

    private AiAskDTO dto() {
        AiAskDTO dto = new AiAskDTO();
        dto.setQuestion("最近总是熬夜，怎么调整作息");
        return dto;
    }

    private AiHealthContext context() {
        AiHealthContext context = new AiHealthContext();
        context.setUserId(1L);
        context.setProfileText("年龄 20；性别 男；BMI 22.1。");
        context.setCheckinText("打卡 6 天；平均睡眠 6.8 小时；熬夜 3 次。");
        context.setPhysicalTestText("暂无体测记录。");
        context.setCheckinDays(6);
        return context;
    }

    private HealthKnowledgeMaterial material() {
        HealthKnowledgeMaterial material = new HealthKnowledgeMaterial();
        material.setId(1L);
        material.setTitle("成年人推荐睡眠时长与熬夜危害");
        material.setSource("中国居民膳食指南（2022）");
        material.setContent("成年人建议每晚睡眠7到9小时。");
        material.setKeyword("睡眠,熬夜,作息");
        return material;
    }

    private static class CapturingSink implements AiStreamSink {

        private AiStreamMetaVO meta;
        private final List<String> deltas = new ArrayList<>();
        private AiAnswerVO done;
        private Integer errorCode;
        private String errorMessage;
        private Long errorConsultationId;
        private boolean partial;
        private boolean cancelOnDelta;

        @Override
        public void sendMeta(AiStreamMetaVO meta) {
            this.meta = meta;
        }

        @Override
        public void sendDelta(String content) {
            deltas.add(content);
            if (cancelOnDelta) {
                throw new StreamCancelledException("客户端断开");
            }
        }

        @Override
        public void sendDone(AiAnswerVO answer) {
            this.done = answer;
        }

        @Override
        public void sendError(Integer code, String message, Long consultationId, boolean partial) {
            this.errorCode = code;
            this.errorMessage = message;
            this.errorConsultationId = consultationId;
            this.partial = partial;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}
