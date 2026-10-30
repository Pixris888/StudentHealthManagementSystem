package com.health.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 消息通知模块配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "health.message")
public class MessageProperties {

    /** 每日预警任务 cron，默认每天 08:00 */
    private String warningCron = "0 0 8 * * ?";

    /** 定时任务默认回溯天数，默认只扫描昨天 */
    private Integer warningLookbackDays = 1;

    /** 单次群发最多接收人数 */
    private Integer maxRecipients = 100;

    /** 预警任务 Redis 锁时长（分钟） */
    private Integer warningLockMinutes = 10;
}
