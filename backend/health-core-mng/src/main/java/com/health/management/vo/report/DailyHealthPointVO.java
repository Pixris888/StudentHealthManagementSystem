package com.health.management.vo.report;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 个人报告中的每日打卡点。
 */
@Data
public class DailyHealthPointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate date;
    private LocalTime sleepTime;
    private BigDecimal sleepDuration;
    private Integer stayUpLate;
    private Integer exerciseDuration;
    private Integer exerciseSufficient;
    private Integer dietBreakfast;
    private Integer waterIntake;
    private Integer emotionType;
    private Integer moodScore;
    private Integer dataQuality;
}
