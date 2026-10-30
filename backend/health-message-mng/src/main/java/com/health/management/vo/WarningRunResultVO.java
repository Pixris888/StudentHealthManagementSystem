package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 预警任务执行结果，便于后台演示与排障。
 */
@Data
public class WarningRunResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate date;
    /** 扫描到的打卡记录数 */
    private Integer scannedCount = 0;
    /** 实际生成的消息数 */
    private Integer generatedCount = 0;
    /** 因幂等或数据不合法跳过的数量 */
    private Integer skippedCount = 0;
    /** 是否获取到分布式锁 */
    private Boolean locked = false;
    private String message;
}
