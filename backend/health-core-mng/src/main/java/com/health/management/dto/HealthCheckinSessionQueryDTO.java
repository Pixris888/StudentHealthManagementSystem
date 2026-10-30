package com.health.management.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 计时会话分页查询条件。
 */
@Data
public class HealthCheckinSessionQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 10;

    @Min(value = 1, message = "会话类型只能是1-睡眠、2-运动")
    @Max(value = 2, message = "会话类型只能是1-睡眠、2-运动")
    private Integer sessionType;

    @Min(value = 0, message = "状态只能是0、1、2")
    @Max(value = 2, message = "状态只能是0、1、2")
    private Integer status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
