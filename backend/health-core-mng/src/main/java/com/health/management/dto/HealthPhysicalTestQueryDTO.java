package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 体测成绩分页查询条件（个人查询与校医查询共用）。
 */
@Data
public class HealthPhysicalTestQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 10;

    @Min(value = 2000, message = "测试年份应在2000-2100之间")
    @Max(value = 2100, message = "测试年份应在2000-2100之间")
    private Integer testYear;

    @Size(max = 20, message = "测试学期长度不能超过20个字符")
    private String testTerm;

    /**
     * 仅校医/超级管理员查询时生效
     */
    @Size(max = 30, message = "学号长度不能超过30个字符")
    private String studentNo;
}
