package com.health.management.dto.report;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 校园健康台账查询条件。
 */
@Data
public class LedgerQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 10;

    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "月份格式应为 yyyy-MM")
    private String month;

    @Size(max = 100, message = "学院名称长度不能超过100个字符")
    private String college;

    @Size(max = 20, message = "年级长度不能超过20个字符")
    private String grade;

    @Size(max = 50, message = "班级名称长度不能超过50个字符")
    private String className;

    @Size(max = 50, message = "搜索关键词长度不能超过50个字符")
    private String keyword;
}
