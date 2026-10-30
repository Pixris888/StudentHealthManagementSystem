package com.health.management.vo.ai;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * AI 使用统计。
 */
@Data
public class AiStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer days;

    private Long total;

    private Long todayCount;

    private Long fallbackCount;

    private BigDecimal fallbackRate;

    private Long emergencyCount;

    private Long feedbackCount;

    private Long helpfulCount;

    private BigDecimal satisfactionRate;

    private Integer avgLatencyMs;

    private Long unansweredCount;
}
