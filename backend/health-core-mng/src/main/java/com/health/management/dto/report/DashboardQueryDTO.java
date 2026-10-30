package com.health.management.dto.report;

import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 全校健康看板查询条件。
 */
@Data
public class DashboardQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "月份格式应为 yyyy-MM")
    private String month;

    @Size(max = 100, message = "学院名称长度不能超过100个字符")
    private String college;

    @Size(max = 20, message = "年级长度不能超过20个字符")
    private String grade;
}
