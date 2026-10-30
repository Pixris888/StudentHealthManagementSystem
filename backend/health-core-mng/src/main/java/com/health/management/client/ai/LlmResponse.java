package com.health.management.client.ai;

import lombok.Data;

/**
 * 大模型返回结果。
 */
@Data
public class LlmResponse {

    private String content;

    /** bailian / ollama / mock / rule */
    private String provider;

    private String model;

    private Integer promptTokens = 0;

    private Integer completionTokens = 0;

    private Integer latencyMs = 0;

    /** 主模型失败后切换到备用模型时，记录失败原因，便于写入 ai_consultation.error_msg */
    private String fallbackReason;
}
