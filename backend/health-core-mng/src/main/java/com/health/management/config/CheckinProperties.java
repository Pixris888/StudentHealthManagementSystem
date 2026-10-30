package com.health.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 健康打卡规则配置（application.yml 的 health.checkin 节点）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "health.checkin")
public class CheckinProperties {

    /** 允许补卡天数（含今天） */
    private int backfillDays = 7;

    /** 单次计时会话最大小时数，超过视为过期 */
    private int sessionMaxHours = 24;

    /** 每条打卡最多照片数 */
    private int maxPhotos = 6;

    /** 睡眠时长低于该值视为异常 */
    private BigDecimal sleepMinHours = new BigDecimal("3");

    /** 睡眠时长高于该值视为异常 */
    private BigDecimal sleepMaxHours = new BigDecimal("12");

    /** 单日运动总时长高于该值视为异常 */
    private int exerciseMaxMinutes = 300;

    /** 单日饮水量高于该值视为异常 */
    private int waterMaxMl = 5000;
}
