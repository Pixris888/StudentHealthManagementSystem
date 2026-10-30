package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 个人打卡概览（按月）。
 */
@Data
public class HealthCheckinSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** yyyy-MM */
    private String month;

    private Integer checkinDays;

    private Integer monthDays;

    /** 打卡率，百分比，保留 1 位小数 */
    private BigDecimal checkinRate;

    /** 截至今天的连续打卡天数 */
    private Integer consecutiveDays;

    private Integer lateCount;

    private Integer exerciseDays;

    private Integer totalExerciseMinutes;

    private BigDecimal avgSleepDuration;

    private Integer avgWaterIntake;

    private BigDecimal avgMoodScore;

    private Integer breakfastDays;

    /** 数据异常待复核的记录数 */
    private Integer abnormalCount;
}
