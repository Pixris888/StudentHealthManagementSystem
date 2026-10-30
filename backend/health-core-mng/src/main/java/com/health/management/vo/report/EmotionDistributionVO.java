package com.health.management.vo.report;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 情绪分布。
 */
@Data
public class EmotionDistributionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer emotionType;
    private String emotionName;
    private Long count;
    private BigDecimal rate;
}
