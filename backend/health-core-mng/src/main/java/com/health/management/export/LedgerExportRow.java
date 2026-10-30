package com.health.management.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 校园健康台账导出行。
 * 校医导出的姓名/学号已脱敏，超管为完整身份。
 */
@Data
public class LedgerExportRow {

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String realName;

    @ExcelProperty("学院")
    private String college;

    @ExcelProperty("专业")
    private String major;

    @ExcelProperty("年级")
    private String grade;

    @ExcelProperty("班级")
    private String className;

    @ExcelProperty("打卡天数")
    private Integer checkinDays;

    @ExcelProperty("有效天数")
    private Integer validDays;

    @ExcelProperty("异常天数")
    private Integer abnormalDays;

    @ExcelProperty("平均睡眠(小时)")
    private BigDecimal avgSleepDuration;

    @ExcelProperty("熬夜次数")
    private Integer stayUpLateCount;

    @ExcelProperty("熬夜率(%)")
    private BigDecimal lateNightRate;

    @ExcelProperty("平均运动(分钟)")
    private BigDecimal avgExerciseDuration;

    @ExcelProperty("运动不足次数")
    private Integer exerciseInsufficientCount;

    @ExcelProperty("运动达标率(%)")
    private BigDecimal exerciseSufficientRate;

    @ExcelProperty("平均心情")
    private BigDecimal avgMoodScore;

    @ExcelProperty("早餐率(%)")
    private BigDecimal breakfastRate;

    @ExcelProperty("平均饮水(ml)")
    private BigDecimal avgWaterIntake;

    @ExcelProperty("BMI")
    private BigDecimal bmi;

    @ExcelProperty("BMI等级")
    private String bmiLevel;

    @ExcelProperty("风险等级")
    private String riskLevel;
}
