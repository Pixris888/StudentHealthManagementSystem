package com.health.management.service.ai;

import com.health.management.entity.HealthKnowledgeMaterial;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiFallbackServiceTest {

    @Test
    void buildsPlanWithDisclaimer() {
        AiFallbackService service = new AiFallbackService();
        ReflectionTestUtils.setField(service, "aiSafetyGuard", new AiSafetyGuard());

        AiHealthContext context = new AiHealthContext();
        context.setCheckinDays(5);
        context.setAvgSleepDuration(new BigDecimal("6.5"));
        context.setStayUpLateCount(3);
        context.setAvgExerciseMinutes(20);
        context.setExerciseInsufficientCount(4);
        context.setBreakfastDays(2);
        context.setAvgMoodScore(new BigDecimal("3.0"));

        String plan = service.buildPlan(3, context);

        assertTrue(plan.contains("未来7天"));
        assertTrue(plan.contains("不能替代医生诊断"));
    }

    @Test
    void buildsAnswerFromRetrievedMaterials() {
        AiFallbackService service = new AiFallbackService();
        ReflectionTestUtils.setField(service, "aiSafetyGuard", new AiSafetyGuard());

        HealthKnowledgeMaterial material = new HealthKnowledgeMaterial();
        material.setTitle("成年人推荐睡眠时长与熬夜危害");
        material.setContent("成年人建议每晚睡眠7到9小时。");
        material.setSource("中国居民膳食指南（2022）");

        String answer = service.buildAnswer("最近熬夜怎么办",
                Collections.singletonList(new AiRetrievedMaterial(material, 10)), null);

        assertTrue(answer.contains("成年人推荐睡眠时长与熬夜危害"));
        assertTrue(answer.contains("不能替代医生诊断"));
    }
}
