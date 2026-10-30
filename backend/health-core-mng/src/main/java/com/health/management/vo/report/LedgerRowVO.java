package com.health.management.vo.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 校园健康台账行（校医脱敏，超管完整）。
 */
@Data
public class LedgerRowVO implements Serializable {

    private static final long serialVersionUID = 1L;

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
    private String riskLevel;

    /** 以下为计算比率用的原始计数，不返回给前端 */
    @JsonIgnore
    private Long sleepFilledCount;
    @JsonIgnore
    private Long exerciseSufficientCount;
    @JsonIgnore
    private Long exerciseFilledCount;
    @JsonIgnore
    private Long breakfastCount;
    @JsonIgnore
    private Long breakfastFilledCount;
}
