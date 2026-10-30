package com.health.management.service.ai;

import com.health.management.config.AiProperties;
import com.health.management.entity.HealthKnowledgeMaterial;
import com.health.management.service.HealthKnowledgeMaterialService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiKnowledgeRetrieverTest {

    @Test
    void retrievesSleepMaterial() {
        AiKnowledgeRetriever retriever = buildRetriever();

        List<AiRetrievedMaterial> result = retriever.retrieve("最近总是熬夜，睡眠不足怎么办", 4);

        assertFalse(result.isEmpty());
        assertEquals(Long.valueOf(1L), result.get(0).getMaterial().getId());
    }

    @Test
    void returnsEmptyForUnrelatedQuestion() {
        AiKnowledgeRetriever retriever = buildRetriever();

        List<AiRetrievedMaterial> result = retriever.retrieve("今天天气怎么样", 4);

        assertTrue(result.isEmpty());
    }

    private AiKnowledgeRetriever buildRetriever() {
        HealthKnowledgeMaterial material = new HealthKnowledgeMaterial();
        material.setId(1L);
        material.setTitle("成年人推荐睡眠时长与熬夜危害");
        material.setKeyword("睡眠,熬夜,作息");
        material.setContent("成年人建议每晚睡眠7到9小时，长期睡眠不足会影响免疫力和情绪。");
        material.setSource("中国居民膳食指南（2022）");

        HealthKnowledgeMaterialService materialService = mock(HealthKnowledgeMaterialService.class);
        when(materialService.findUsableMaterials(null, 50))
                .thenReturn(Collections.singletonList(material));

        AiProperties properties = new AiProperties();
        properties.setRetrieveLimit(4);

        AiKnowledgeRetriever retriever = new AiKnowledgeRetriever();
        ReflectionTestUtils.setField(retriever, "healthKnowledgeMaterialService", materialService);
        ReflectionTestUtils.setField(retriever, "aiProperties", properties);
        return retriever;
    }
}
