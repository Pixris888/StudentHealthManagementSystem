package com.health.management.vo.report;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 历史月度报告摘要。
 */
@Data
public class HealthReportHistoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String month;
    private String reportTitle;
    private Integer checkinDays;
    private BigDecimal avgSleepDuration;
    private Integer stayUpLateCount;
    private Integer avgExerciseDuration;
    private Integer exerciseInsufficientCount;
    private BigDecimal avgMoodScore;
    private LocalDateTime generateTime;
}
