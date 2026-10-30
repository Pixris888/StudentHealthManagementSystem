package com.health.management.vo.report;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人月度健康统计报告。
 */
@Data
public class MonthlyReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String month;
    private Long userId;
    private String studentNo;
    private String realName;
    private String college;
    private String major;
    private String grade;
    private String className;

    private Integer checkinDays;
    private Integer validDays;
    private Integer abnormalDays;
    private BigDecimal avgSleepDuration;
    private Integer stayUpLateCount;
    private BigDecimal lateNightRate;
    private BigDecimal avgExerciseDuration;
    private Integer exerciseInsufficientCount;
    private BigDecimal exerciseSufficientRate;
    private BigDecimal avgMoodScore;
    private BigDecimal breakfastRate;
    private BigDecimal avgWaterIntake;

    private BigDecimal bmi;
    private String bmiLevel;
    private BigDecimal latestPhysicalScore;
    private String latestPhysicalGradeLevel;

    private List<DailyHealthPointVO> daily;
    private List<String> suggestions;
    private String summary;
    private LocalDateTime generateTime;
}
