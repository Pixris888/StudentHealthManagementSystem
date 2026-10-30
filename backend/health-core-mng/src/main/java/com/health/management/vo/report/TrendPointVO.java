package com.health.management.vo.report;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 看板每日趋势点。
 */
@Data
public class TrendPointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate date;
    private Long checkinStudents;
    private Long checkinRecords;
    private BigDecimal avgSleepDuration;
    private Long stayUpLateCount;
    private BigDecimal avgExerciseDuration;
    private BigDecimal avgMoodScore;
}
