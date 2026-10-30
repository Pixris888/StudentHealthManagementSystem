package com.health.management.vo.report;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 全校健康汇总看板。
 */
@Data
public class DashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String month;
    private String college;
    private String grade;
    private DashboardOverviewVO overview;
    private List<TrendPointVO> trend;
    private List<CollegeDistributionVO> collegeDistribution;
    private List<EmotionDistributionVO> emotionDistribution;
    private List<RiskStudentVO> riskStudents;
}
