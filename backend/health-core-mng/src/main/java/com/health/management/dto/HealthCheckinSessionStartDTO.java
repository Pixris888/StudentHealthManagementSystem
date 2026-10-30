package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 开始睡眠/运动计时请求。
 */
@Data
public class HealthCheckinSessionStartDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "会话类型不能为空")
    @Min(value = 1, message = "会话类型只能是1-睡眠、2-运动")
    @Max(value = 2, message = "会话类型只能是1-睡眠、2-运动")
    private Integer sessionType;

    /** 运动类型，sessionType=2 时必填 */
    @Size(max = 50, message = "运动类型长度不能超过50个字符")
    private String exerciseType;

    /** 运动强度，sessionType=2 时必填 */
    @Min(value = 1, message = "运动强度只能是1-低、2-中、3-高")
    @Max(value = 3, message = "运动强度只能是1-低、2-中、3-高")
    private Integer exerciseIntensity;
}
