package com.health.management.service.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiSafetyGuardTest {

    private final AiSafetyGuard guard = new AiSafetyGuard();

    @Test
    void detectsEmergencyQuestion() {
        assertTrue(guard.isEmergency("我最近胸痛，喘不上气"));
        assertTrue(guard.isEmergency("我不想活了"));
    }

    @Test
    void doesNotTreatNormalQuestionAsEmergency() {
        assertFalse(guard.isEmergency("最近总是熬夜，怎么调整作息"));
    }

    @Test
    void addsDisclaimer() {
        String answer = guard.ensureDisclaimer("建议多喝水，早点休息。");
        assertTrue(answer.contains("不能替代医生诊断"));
    }

    @Test
    void disclaimerIsNotDuplicated() {
        String once = guard.ensureDisclaimer("建议多喝水。");
        assertEquals(once, guard.ensureDisclaimer(once));
    }

    @Test
    void sanitizesControlCharacters() {
        assertEquals("你好", guard.sanitizeQuestion("  你好\u0000  "));
    }
}
