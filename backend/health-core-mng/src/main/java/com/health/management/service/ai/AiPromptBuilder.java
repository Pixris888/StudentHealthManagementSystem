package com.health.management.service.ai;

import com.health.management.client.ai.LlmMessage;
import com.health.management.common.util.ValidateUtils;
import com.health.management.entity.AiConsultation;
import com.health.management.entity.HealthKnowledgeMaterial;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Prompt 组装。
 *
 * 关键约束：
 * 1. 只把健康指标和知识素材交给模型，不传姓名/学号/电话；
 * 2. 素材没有覆盖的问题要求模型明确说“知识库暂无直接依据”；
 * 3. 紧急情况由 AiSafetyGuard 提前拦截，不进入模型。
 */
@Component
public class AiPromptBuilder {

    private static final String SYSTEM_PROMPT = String.join("\n",
            "你是“校园健康助手”，面向大学生提供健康科普和生活方式建议。",
            "必须遵守：",
            "1. 你不是医生，不做诊断、不开药、不给出具体药物剂量，不替代校医或医院就诊。",
            "2. 只依据【学生健康画像】和【权威知识素材】回答；素材没有覆盖的内容，",
            "   要明确说“知识库暂无直接依据”，再给保守的一般性建议，不要编造。",
            "3. 遇到自伤、自杀、胸痛、呼吸困难、意识障碍、大出血等紧急情况，",
            "   先建议立即拨打 120 并联系校医/辅导员。",
            "4. 建议要具体、可执行，结合学生的睡眠、运动、饮食、情绪数据。",
            "5. 回答用 Markdown 分点；末尾列出“参考来源”。",
            "6. 不泄露学生姓名、学号、电话等身份信息。",
            "7. 不要输出你的思考过程。");

    private static final String PLAN_SYSTEM_PROMPT = String.join("\n",
            "你是校园健康助手。请根据学生健康画像，生成未来7天的个性化健康计划。",
            "要求：",
            "1. 输出 Markdown，包含目标、7天安排表、注意事项。",
            "2. 结合最近7天睡眠、熬夜、运动、早餐、情绪数据；没有数据时给通用建议。",
            "3. 不安排极端节食、超高强度运动、药物建议。",
            "4. 有慢性病或过敏史时，明确避开相关运动/食物，并建议咨询校医。",
            "5. 不要输出你的思考过程。");

    public List<LlmMessage> buildQaMessages(String question, AiHealthContext context,
                                            List<AiRetrievedMaterial> materials,
                                            List<AiConsultation> history) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.system(SYSTEM_PROMPT));

        if (history != null) {
            for (AiConsultation item : history) {
                if (item == null) {
                    continue;
                }
                if (ValidateUtils.hasText(item.getQuestion())) {
                    messages.add(LlmMessage.user(truncate(item.getQuestion(), 300)));
                }
                if (ValidateUtils.hasText(item.getAnswer())) {
                    messages.add(LlmMessage.assistant(truncate(item.getAnswer(), 500)));
                }
            }
        }

        StringBuilder userContent = new StringBuilder();
        userContent.append(context == null ? "【学生健康画像】\n暂无健康数据。" : context.toPromptText());
        userContent.append("\n\n【权威知识素材】\n");
        userContent.append(formatMaterials(materials));
        userContent.append("\n\n【当前问题】\n").append(question);
        messages.add(LlmMessage.user(userContent.toString()));
        return messages;
    }

    public List<LlmMessage> buildPlanMessages(Integer planType, AiHealthContext context) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.system(PLAN_SYSTEM_PROMPT));
        StringBuilder userContent = new StringBuilder();
        userContent.append(context == null ? "【学生健康画像】\n暂无健康数据。" : context.toPromptText());
        userContent.append("\n\n请生成未来7天的“").append(planTypeName(planType)).append("”计划。");
        messages.add(LlmMessage.user(userContent.toString()));
        return messages;
    }

    private String formatMaterials(List<AiRetrievedMaterial> materials) {
        if (materials == null || materials.isEmpty()) {
            return "知识库暂无匹配素材。";
        }
        StringBuilder text = new StringBuilder();
        int index = 1;
        for (AiRetrievedMaterial item : materials) {
            if (item == null || item.getMaterial() == null) {
                continue;
            }
            HealthKnowledgeMaterial material = item.getMaterial();
            text.append("[").append(index++).append("] 《")
                    .append(ValidateUtils.hasText(material.getTitle()) ? material.getTitle() : "未命名素材")
                    .append("》");
            if (ValidateUtils.hasText(material.getSource())) {
                text.append("（").append(material.getSource()).append("）");
            }
            text.append("\n").append(truncate(material.getContent(), 800)).append("\n\n");
        }
        return text.length() == 0 ? "知识库暂无匹配素材。" : text.toString();
    }

    private String planTypeName(Integer planType) {
        if (Integer.valueOf(1).equals(planType)) {
            return "作息";
        }
        if (Integer.valueOf(2).equals(planType)) {
            return "运动";
        }
        return "综合健康";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
