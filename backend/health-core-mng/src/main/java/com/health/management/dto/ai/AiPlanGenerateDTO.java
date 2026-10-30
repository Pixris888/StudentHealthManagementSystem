package com.health.management.dto.ai;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * AI 个性化计划生成请求。
 */
@Data
public class AiPlanGenerateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 计划类型：1-作息计划，2-运动计划，3-综合计划
     */
    @NotNull(message = "请选择计划类型")
    @Min(value = 1, message = "计划类型只能是1-作息、2-运动、3-综合")
    @Max(value = 3, message = "计划类型只能是1-作息、2-运动、3-综合")
    private Integer planType;
}
