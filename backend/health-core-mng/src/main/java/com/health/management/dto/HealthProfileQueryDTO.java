package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 校医/超级管理员查询健康档案分页条件。
 */
@Data
public class HealthProfileQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 10;

    @Size(max = 30, message = "学号长度不能超过30个字符")
    private String studentNo;

    @Size(max = 50, message = "姓名长度不能超过50个字符")
    private String realName;

    @Size(max = 100, message = "学院名称长度不能超过100个字符")
    private String college;

    @Size(max = 100, message = "专业名称长度不能超过100个字符")
    private String major;

    @Size(max = 20, message = "年级长度不能超过20个字符")
    private String grade;

    @Size(max = 50, message = "班级名称长度不能超过50个字符")
    private String className;

    @Min(value = 0, message = "性别只能是0-未知、1-男、2-女")
    @Max(value = 2, message = "性别只能是0-未知、1-男、2-女")
    private Integer gender;
}
