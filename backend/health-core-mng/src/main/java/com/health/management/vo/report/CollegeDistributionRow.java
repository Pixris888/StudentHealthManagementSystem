package com.health.management.vo.report;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 学院维度聚合 SQL 的原始结果。
 */
@Data
public class CollegeDistributionRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private String college;
    private Long studentCount;
    private Long checkinStudents;
    private BigDecimal avgSleepDuration;
    private Long stayUpLateCount;
    private Long sleepFilledCount;
    private Long exerciseSufficientCount;
    private Long exerciseFilledCount;
}
