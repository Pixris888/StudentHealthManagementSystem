package com.health.management.vo.ai;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI 健康计划展示对象。
 */
@Data
public class AiHealthPlanVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private Integer planType;

    private String planTypeName;

    private String planTitle;

    private String planContent;

    private String planData;

    private LocalDate baseCheckinDate;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer status;

    private String modelName;

    private String provider;

    private LocalDateTime adoptedTime;

    private LocalDateTime createTime;
}
