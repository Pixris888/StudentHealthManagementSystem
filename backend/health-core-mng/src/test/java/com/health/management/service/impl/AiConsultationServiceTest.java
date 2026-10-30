package com.health.management.service.impl;

import com.health.management.client.ai.LlmClient;
import com.health.management.client.ai.LlmResponse;
import com.health.management.config.AiProperties;
import com.health.management.dto.ai.AiAskDTO;
import com.health.management.entity.AiConsultation;
import com.health.management.entity.HealthKnowledgeMaterial;
import com.health.management.mapper.AiConsultationMapper;
import com.health.management.security.LoginUser;
import com.health.management.service.ai.AiContextBuilder;
import com.health.management.service.ai.AiFallbackService;
import com.health.management.service.ai.AiHealthContext;
import com.health.management.service.ai.AiKnowledgeRetriever;
import com.health.management.service.ai.AiPromptBuilder;
import com.health.management.service.ai.AiRateLimiter;
import com.health.management.service.ai.AiRetrievedMaterial;
import com.health.management.service.ai.AiSafetyGuard;
import com.health.management.service.ai.AiUserInfoSupport;
import com.health.management.vo.ai.AiAnswerVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiConsultationServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void askUsesLlmAndPersistsProviderAndSources() {
        loginAsStudent(1L);

        AiProperties properties = new AiProperties();
        properties.setProvider("bailian");

        AiHealthContext context = new AiHealthContext();
        context.setUserId(1L);
        context.setProfileText("年龄 20；性别 男；BMI 22.1。");
        context.setCheckinText("打卡 6 天；平均睡眠 6.8 小时；熬夜 3 次。");
        context.setPhysicalTestText("暂无体测记录。");
        context.setCheckinDays(6);

        HealthKnowledgeMaterial material = new HealthKnowledgeMaterial();
        material.setId(1L);
        material.setTitle("成年人推荐睡眠时长与熬夜危害");
        material.setSource("中国居民膳食指南（2022）");
        material.setContent("成年人建议每晚睡眠7到9小时。");
        material.setKeyword("睡眠,熬夜,作息");

        AiContextBuilder contextBuilder = mock(AiContextBuilder.class);
        when(contextBuilder.build(1L)).thenReturn(context);

        AiKnowledgeRetriever retriever = mock(AiKnowledgeRetriever.class);
        when(retriever.retrieve(anyString(), anyInt()))
                .thenReturn(Collections.singletonList(new AiRetrievedMaterial(material, 12)));

        LlmResponse llmResponse = new LlmResponse();
        llmResponse.setContent("建议固定起床时间，逐步把入睡时间提前到23:30前。");
        llmResponse.setProvider("bailian");
        llmResponse.setModel("qwen-plus");
        llmResponse.setPromptTokens(320);
        llmResponse.setCompletionTokens(88);

        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.chat(anyList())).thenReturn(llmResponse);

        AiSafetyGuard safetyGuard = new AiSafetyGuard();
        AiFallbackService fallbackService = new AiFallbackService();
        ReflectionTestUtils.setField(fallbackService, "aiSafetyGuard", safetyGuard);

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
        doReturn(true).when(service).save(any(AiConsultation.class));

        AiAskDTO dto = new AiAskDTO();
        dto.setQuestion("最近总是熬夜，怎么调整作息");

        AiAnswerVO answer = service.ask(dto);

        assertNotNull(answer);
        assertEquals("bailian", answer.getProvider());
        assertEquals(Integer.valueOf(1), answer.getAnswerStatus());
        assertFalse(answer.getFallback());
        assertTrue(answer.getAnswer().contains("不能替代医生诊断"));
        assertTrue(answer.getSources().get(0).contains("成年人推荐睡眠时长与熬夜危害"));

        ArgumentCaptor<AiConsultation> captor = ArgumentCaptor.forClass(AiConsultation.class);
        verify(service).save(captor.capture());
        AiConsultation saved = captor.getValue();
        assertEquals(Long.valueOf(1L), saved.getUserId());
        assertNotNull(saved.getSessionId());
        assertEquals("bailian", saved.getProvider());
        assertEquals("qwen-plus", saved.getModelName());
        assertEquals("1", saved.getRetrievedIds());
        assertTrue(saved.getKnowledgeSource().contains("成年人推荐睡眠时长与熬夜危害"));
        assertEquals(Integer.valueOf(320), saved.getPromptTokens());
        assertEquals(Integer.valueOf(88), saved.getCompletionTokens());
    }

    private void loginAsStudent(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setUsername("student01");
        loginUser.setRoles(Collections.singletonList("STUDENT"));
        loginUser.buildAuthorities();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
