package com.health.management.dto.report;

import lombok.Data;

import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 个人月度报告查询条件。
 */
@Data
public class ReportMonthQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** yyyy-MM；不传默认当前月份 */
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "月份格式应为 yyyy-MM")
    private String month;
}
