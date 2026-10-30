package com.health.management.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 体测成绩 Excel 导入行。
 * 列顺序：学号、测试年份、学期、身高、体重、肺活量、50米跑、坐位体前屈、
 * 立定跳远、耐力跑、力量项目、总分、等级、测试日期。
 */
@Data
public class PhysicalTestImportRow {

    @ExcelProperty(index = 0)
    private String studentNo;

    @ExcelProperty(index = 1)
    private Integer testYear;

    @ExcelProperty(index = 2)
    private String testTerm;

    @ExcelProperty(index = 3)
    private BigDecimal height;

    @ExcelProperty(index = 4)
    private BigDecimal weight;

    @ExcelProperty(index = 5)
    private Integer vitalCapacity;

    @ExcelProperty(index = 6)
    private BigDecimal run50m;

    @ExcelProperty(index = 7)
    private BigDecimal sitAndReach;

    @ExcelProperty(index = 8)
    private BigDecimal standingJump;

    @ExcelProperty(index = 9)
    private BigDecimal enduranceRun;

    @ExcelProperty(index = 10)
    private Integer strengthItem;

    @ExcelProperty(index = 11)
    private BigDecimal totalScore;

    @ExcelProperty(index = 12)
    private String gradeLevel;

    @ExcelProperty(index = 13)
    private String testDate;
}
