package com.health.management.vo.report;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 看板顶部 KPI。
 */
@Data
public class DashboardOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long studentTotal;
    private Long checkinStudents;
    private BigDecimal checkinRate;
    private Long checkinRecords;
    private BigDecimal avgSleepDuration;
    private Long stayUpLateCount;
    private BigDecimal lateNightRate;
    private BigDecimal avgExerciseDuration;
    private BigDecimal exerciseSufficientRate;
    private BigDecimal avgMoodScore;
    private BigDecimal breakfastRate;
    private Long abnormalRecords;
}
