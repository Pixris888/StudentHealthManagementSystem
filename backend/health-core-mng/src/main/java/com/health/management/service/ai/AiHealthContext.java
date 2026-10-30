package com.health.management.service.ai;

import com.health.management.common.util.ValidateUtils;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 学生健康画像：只包含生成建议所需的指标，不包含姓名、学号、电话等身份信息。
 */
@Data
public class AiHealthContext {

    private Long userId;

    private String profileText;

    private String checkinText;

    private String physicalTestText;

    private LocalDate latestCheckinDate;

    private BigDecimal avgSleepDuration;

    private Integer stayUpLateCount;

    private Integer avgExerciseMinutes;

    private Integer exerciseInsufficientCount;

    private Integer breakfastDays;

    private BigDecimal avgMoodScore;

    private Integer checkinDays;

    public boolean hasCheckinData() {
        return checkinDays != null && checkinDays > 0;
    }

    public String toPromptText() {
        StringBuilder text = new StringBuilder();
        text.append("【学生健康画像】\n");
        text.append("档案信息：").append(hasText(profileText) ? profileText : "未完善健康档案。").append("\n");
        text.append("最近7天：").append(hasText(checkinText) ? checkinText : "暂无打卡记录。").append("\n");
        text.append("体测信息：").append(hasText(physicalTestText) ? physicalTestText : "暂无体测记录。");
        return text.toString();
    }

    private boolean hasText(String value) {
        return ValidateUtils.hasText(value);
    }
}
