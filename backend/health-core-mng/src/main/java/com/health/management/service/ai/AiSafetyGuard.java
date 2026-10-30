package com.health.management.service.ai;

import com.health.management.common.util.ValidateUtils;
import org.springframework.stereotype.Component;

/**
 * AI 医疗安全边界：
 * 1. 紧急问题优先拦截，不经过大模型；
 * 2. 所有回答统一追加免责声明；
 * 3. 清理输入中的控制字符。
 */
@Component
public class AiSafetyGuard {

    private static final String DISCLAIMER =
            "\n\n> 以上内容仅供参考，不能替代医生诊断。如有持续或加重的不适，请及时到校医院就诊。";

    private static final String EMERGENCY_ANSWER =
            "你描述的情况可能属于紧急情况。请立即拨打 120，并联系校医院、辅导员或身边同学陪同就医，"
                    + "不要独自等待。";

    /**
     * 保守的紧急关键词。宁可误拦截，也不要把紧急情况交给模型自由发挥。
     */
    private static final String[] EMERGENCY_KEYWORDS = new String[]{
            "自杀", "自残", "不想活", "轻生", "结束生命", "活着没意思",
            "胸痛", "胸口剧痛", "呼吸困难", "喘不上气", "窒息",
            "昏迷", "意识不清", "意识模糊", "大出血", "抽搐",
            "过敏休克", "休克", "中毒", "服毒", "误服", "高烧惊厥"
    };

    public boolean isEmergency(String question) {
        if (!ValidateUtils.hasText(question)) {
            return false;
        }
        String text = question.toLowerCase();
        for (String keyword : EMERGENCY_KEYWORDS) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public String emergencyAnswer() {
        return EMERGENCY_ANSWER + DISCLAIMER;
    }

    public String ensureDisclaimer(String answer) {
        if (!ValidateUtils.hasText(answer)) {
            return "暂时没有生成有效建议，请稍后重试或直接咨询校医。" + DISCLAIMER;
        }
        if (answer.contains("不能替代医生诊断") || answer.contains("不能替代医生")) {
            return answer;
        }
        return answer.trim() + DISCLAIMER;
    }

    public String sanitizeQuestion(String question) {
        if (question == null) {
            return "";
        }
        return question
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", " ")
                .trim();
    }
}
