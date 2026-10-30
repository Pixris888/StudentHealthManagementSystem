package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体测成绩展示对象，也用于个人健康指标趋势。
 */
@Data
public class HealthPhysicalTestVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String studentNo;
    private Integer testYear;
    private String testTerm;
    private BigDecimal height;
    private BigDecimal weight;
    private BigDecimal bmi;
    private Integer vitalCapacity;
    private BigDecimal run50m;
    private BigDecimal sitAndReach;
    private BigDecimal standingJump;
    private BigDecimal enduranceRun;
    private Integer strengthItem;
    private BigDecimal totalScore;
    private String gradeLevel;
    private LocalDate testDate;
    private LocalDateTime createTime;
}
