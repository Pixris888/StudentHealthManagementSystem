package com.health.management.vo.report;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 看板 KPI 聚合 SQL 的原始结果。
 */
@Data
public class DashboardOverviewRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long studentTotal;
    private Long checkinStudents;
    private Long checkinRecords;
    private BigDecimal avgSleepDuration;
    private Long stayUpLateCount;
    private Long sleepFilledCount;
    private BigDecimal avgExerciseDuration;
    private Long exerciseSufficientCount;
    private Long exerciseFilledCount;
    private BigDecimal avgMoodScore;
    private Long breakfastCount;
    private Long breakfastFilledCount;
    private Long abnormalRecords;
}
