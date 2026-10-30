package com.health.management.vo.report;

import lombok.Data;

import java.io.Serializable;

/**
 * 看板需关注学生（校医脱敏，超管完整）。
 */
@Data
public class RiskStudentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String studentNo;
    private String realName;
    private String college;
    private String grade;
    private String className;
    private Integer checkinDays;
    private Integer stayUpLateCount;
    private Integer exerciseInsufficientCount;
    private Integer abnormalDays;
    private String riskLevel;
}
