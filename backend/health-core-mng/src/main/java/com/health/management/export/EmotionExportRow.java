package com.health.management.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 情绪分布导出行。
 */
@Data
public class EmotionExportRow {

    @ExcelProperty("情绪")
    private String emotionName;

    @ExcelProperty("人数")
    private Long count;

    @ExcelProperty("占比(%)")
    private BigDecimal rate;
}
