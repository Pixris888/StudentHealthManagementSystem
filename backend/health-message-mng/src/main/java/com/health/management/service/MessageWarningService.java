package com.health.management.service;

import com.health.management.vo.WarningRunResultVO;

import java.time.LocalDate;

/**
 * 健康预警生成服务。
 */
public interface MessageWarningService {

    /**
     * 扫描指定日期的打卡数据并生成预警消息。
     * 通过 Redis 锁 + dedup_key 唯一索引保证幂等。
     */
    WarningRunResultVO run(LocalDate date);
}
