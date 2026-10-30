package com.health.management.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 打卡记录分页查询条件（个人查询与校医查询共用）。
 */
@Data
public class HealthCheckinQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 10;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Min(value = 0, message = "熬夜标记只能是0或1")
    @Max(value = 1, message = "熬夜标记只能是0或1")
    private Integer stayUpLate;

    @Min(value = 0, message = "运动达标标记只能是0或1")
    @Max(value = 1, message = "运动达标标记只能是0或1")
    private Integer exerciseSufficient;

    @Min(value = 0, message = "数据质量只能是0或1")
    @Max(value = 1, message = "数据质量只能是0或1")
    private Integer dataQuality;

    /**
     * 仅校医/超级管理员查询时生效
     */
    @Size(max = 30, message = "学号长度不能超过30个字符")
    private String studentNo;
}
