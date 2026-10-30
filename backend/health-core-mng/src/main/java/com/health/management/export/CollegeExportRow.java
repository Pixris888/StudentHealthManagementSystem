package com.health.management.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 学院分布导出行。
 */
@Data
public class CollegeExportRow {

    @ExcelProperty("学院")
    private String college;

    @ExcelProperty("学生数")
    private Long studentCount;

    @ExcelProperty("打卡学生数")
    private Long checkinStudents;

    @ExcelProperty("打卡率(%)")
    private BigDecimal checkinRate;

    @ExcelProperty("平均睡眠(小时)")
    private BigDecimal avgSleepDuration;

    @ExcelProperty("熬夜率(%)")
    private BigDecimal lateNightRate;

    @ExcelProperty("运动达标率(%)")
    private BigDecimal exerciseSufficientRate;
}
