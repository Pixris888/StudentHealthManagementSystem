package com.health.management.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 每日趋势导出行。
 */
@Data
public class TrendExportRow {

    @ExcelProperty("日期")
    private String date;

    @ExcelProperty("打卡学生数")
    private Long checkinStudents;

    @ExcelProperty("打卡记录数")
    private Long checkinRecords;

    @ExcelProperty("平均睡眠(小时)")
    private BigDecimal avgSleepDuration;

    @ExcelProperty("熬夜次数")
    private Long stayUpLateCount;

    @ExcelProperty("平均运动(分钟)")
    private BigDecimal avgExerciseDuration;

    @ExcelProperty("平均心情")
    private BigDecimal avgMoodScore;
}
