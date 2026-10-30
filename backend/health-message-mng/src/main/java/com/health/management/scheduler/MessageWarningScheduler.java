package com.health.management.scheduler;

import com.health.management.config.MessageProperties;
import com.health.management.service.MessageWarningService;
import com.health.management.vo.WarningRunResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 每日健康预警定时任务。
 * 默认每天 08:00 扫描昨天；可通过 health.message.warning-lookback-days 回溯多天。
 */
@Slf4j
@Component
public class MessageWarningScheduler {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private MessageWarningService messageWarningService;

    @Autowired
    private MessageProperties messageProperties;

    @Scheduled(cron = "${health.message.warning-cron:0 0 8 * * ?}", zone = "Asia/Shanghai")
    public void scanDaily() {
        int lookbackDays = messageProperties.getWarningLookbackDays() == null
                ? 1 : Math.max(1, messageProperties.getWarningLookbackDays());
        LocalDate today = LocalDate.now(ZONE);
        for (int i = lookbackDays; i >= 1; i--) {
            LocalDate date = today.minusDays(i);
            try {
                WarningRunResultVO result = messageWarningService.run(date);
                log.info("健康预警任务完成 date={}, scanned={}, generated={}, skipped={}, locked={}, message={}",
                        date, result.getScannedCount(), result.getGeneratedCount(),
                        result.getSkippedCount(), result.getLocked(), result.getMessage());
            } catch (Exception e) {
                log.error("健康预警任务执行失败 date={}", date, e);
            }
        }
    }
}
