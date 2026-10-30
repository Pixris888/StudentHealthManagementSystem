package com.health.management.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 需关注学生导出行。
 */
@Data
public class RiskExportRow {

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String realName;

    @ExcelProperty("学院")
    private String college;

    @ExcelProperty("年级")
    private String grade;

    @ExcelProperty("班级")
    private String className;

    @ExcelProperty("打卡天数")
    private Integer checkinDays;

    @ExcelProperty("熬夜次数")
    private Integer stayUpLateCount;

    @ExcelProperty("运动不足次数")
    private Integer exerciseInsufficientCount;

    @ExcelProperty("异常天数")
    private Integer abnormalDays;

    @ExcelProperty("风险等级")
    private String riskLevel;
}
