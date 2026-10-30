package com.health.management.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 报表概览的“指标/数值/说明”行。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportMetricRow {

    @ExcelProperty("指标")
    private String metric;

    @ExcelProperty("数值")
    private String value;

    @ExcelProperty("说明")
    private String note;
}
