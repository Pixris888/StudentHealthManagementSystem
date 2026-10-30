package com.health.management.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 个人报告每日打卡明细。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyReportRow {

    @ExcelProperty("日期")
    private String date;

    @ExcelProperty("入睡时间")
    private String sleepTime;

    @ExcelProperty("睡眠时长(小时)")
    private String sleepDuration;

    @ExcelProperty("是否熬夜")
    private String stayUpLate;

    @ExcelProperty("运动时长(分钟)")
    private String exerciseDuration;

    @ExcelProperty("运动是否达标")
    private String exerciseSufficient;

    @ExcelProperty("心情评分")
    private String moodScore;

    @ExcelProperty("是否吃早餐")
    private String breakfast;

    @ExcelProperty("饮水量(ml)")
    private String waterIntake;

    @ExcelProperty("数据质量")
    private String dataQuality;
}
