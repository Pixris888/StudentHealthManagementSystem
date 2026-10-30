package com.health.management.vo.report;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 学院维度汇总。
 */
@Data
public class CollegeDistributionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String college;
    private Long studentCount;
    private Long checkinStudents;
    private BigDecimal checkinRate;
    private BigDecimal avgSleepDuration;
    private BigDecimal lateNightRate;
    private BigDecimal exerciseSufficientRate;
}
