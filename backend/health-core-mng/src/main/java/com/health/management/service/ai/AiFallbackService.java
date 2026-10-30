package com.health.management.service.ai;

import com.health.management.common.util.ValidateUtils;
import com.health.management.entity.HealthKnowledgeMaterial;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 规则兜底。
 * 大模型不可用时，用知识素材和确定性模板保证系统仍可回答/生成计划。
 */
@Component
public class AiFallbackService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd");

    @Autowired
    private AiSafetyGuard aiSafetyGuard;

    public String buildAnswer(String question, List<AiRetrievedMaterial> materials, AiHealthContext context) {
        StringBuilder answer = new StringBuilder();
        if (materials != null && !materials.isEmpty()) {
            answer.append("当前大模型服务暂时不可用，先根据校园健康知识库为你整理以下信息：\n\n");
            int index = 1;
            for (AiRetrievedMaterial item : materials) {
                if (item == null || item.getMaterial() == null) {
                    continue;
                }
                HealthKnowledgeMaterial material = item.getMaterial();
                answer.append(index++).append(". 《")
                        .append(ValidateUtils.hasText(material.getTitle()) ? material.getTitle() : "未命名素材")
                        .append("》");
                if (ValidateUtils.hasText(material.getSource())) {
                    answer.append("（").append(material.getSource()).append("）");
                }
                answer.append("\n").append(truncate(material.getContent(), 300)).append("\n\n");
            }
            answer.append("建议结合自身情况调整。");
        } else {
            answer.append("当前 AI 服务暂时不可用，知识库也没有检索到与你的问题直接相关的素材。\n\n");
            answer.append("建议：\n");
            answer.append("1. 先完善健康档案和最近打卡记录；\n");
            answer.append("2. 保持规律作息、均衡饮食和适量运动；\n");
            answer.append("3. 如有明显不适或持续症状，请及时到校医院就诊。");
        }
        return aiSafetyGuard.ensureDisclaimer(answer.toString());
    }

    public String buildPlan(Integer planType, AiHealthContext context) {
        int type = planType == null ? 3 : planType;
        LocalDate start = LocalDate.now(ZONE);
        StringBuilder plan = new StringBuilder();
        plan.append("# 未来7天").append(planTypeName(type)).append("计划\n\n");
        plan.append("## 数据依据\n").append(buildBasis(context)).append("\n\n");
        plan.append("## 每日安排\n");
        plan.append("| 日期 | 睡眠目标 | 运动安排 | 饮食提醒 | 注意事项 |\n");
        plan.append("| --- | --- | --- | --- | --- |\n");

        String sleepTarget = sleepTarget(context);
        String exercise = exercisePlan(type, context);
        String diet = "三餐规律；早餐加鸡蛋/牛奶；少油少糖";
        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            String tip;
            if (i % 3 == 0) {
                tip = "睡前1小时不看手机";
            } else if (i % 3 == 1) {
                tip = "运动前热身5分钟";
            } else {
                tip = "如有不适立即停止";
            }
            plan.append("| ").append(DATE_FORMAT.format(date))
                    .append(" | ").append(sleepTarget)
                    .append(" | ").append(exercise)
                    .append(" | ").append(diet)
                    .append(" | ").append(tip)
                    .append(" |\n");
        }

        plan.append("\n## 注意事项\n");
        plan.append("- 运动循序渐进，不要突然加量；\n");
        plan.append("- 有慢性病或过敏史时，请先咨询校医再调整运动/饮食；\n");
        plan.append("- 本计划由规则模板生成，不能替代医生诊断。");
        return aiSafetyGuard.ensureDisclaimer(plan.toString());
    }

    private String buildBasis(AiHealthContext context) {
        if (context == null || !context.hasCheckinData()) {
            return "暂无最近7天打卡数据，以下为通用计划。建议先完善健康档案和每日打卡，计划会更准确。";
        }
        StringBuilder basis = new StringBuilder();
        basis.append("最近7天打卡 ").append(context.getCheckinDays()).append(" 天；");
        basis.append("平均睡眠 ").append(context.getAvgSleepDuration() == null
                ? "未知" : context.getAvgSleepDuration().toPlainString()).append(" 小时；");
        basis.append("熬夜 ").append(context.getStayUpLateCount()).append(" 次；");
        basis.append("平均运动 ").append(context.getAvgExerciseMinutes() == null
                ? "未知" : context.getAvgExerciseMinutes()).append(" 分钟；");
        basis.append("运动不足 ").append(context.getExerciseInsufficientCount()).append(" 次；");
        basis.append("早餐 ").append(context.getBreakfastDays()).append(" 天；");
        basis.append("平均心情 ").append(context.getAvgMoodScore() == null
                ? "未知" : context.getAvgMoodScore().toPlainString()).append("/5。");
        return basis.toString();
    }

    private String sleepTarget(AiHealthContext context) {
        if (context != null && context.getAvgSleepDuration() != null
                && context.getAvgSleepDuration().doubleValue() < 7) {
            return "23:30-07:30（保证7-8小时）";
        }
        if (context != null && context.getStayUpLateCount() != null
                && context.getStayUpLateCount() > 0) {
            return "23:30-07:30（尽量提前入睡）";
        }
        return "23:00-07:00（保证7-8小时）";
    }

    private String exercisePlan(int type, AiHealthContext context) {
        if (type == 1) {
            return "散步/拉伸20分钟";
        }
        if (context != null && context.getExerciseInsufficientCount() != null
                && context.getExerciseInsufficientCount() > 0) {
            return "快走30分钟（循序渐进）";
        }
        if (context != null && context.getAvgExerciseMinutes() != null
                && context.getAvgExerciseMinutes() < 30) {
            return "快走30分钟（逐步增加）";
        }
        if (type == 2) {
            return "快走/慢跑30-40分钟";
        }
        return "快走30分钟 + 拉伸10分钟";
    }

    private String planTypeName(int type) {
        if (type == 1) {
            return "作息";
        }
        if (type == 2) {
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
