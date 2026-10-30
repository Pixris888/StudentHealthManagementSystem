package com.health.management.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 健康建议行。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdviceRow {

    @ExcelProperty("健康建议")
    private String advice;
}
