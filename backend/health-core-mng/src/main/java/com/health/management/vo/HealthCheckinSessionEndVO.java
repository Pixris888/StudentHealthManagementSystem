package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 结束计时会话后的返回：会话详情 + 同步后的每日打卡。
 */
@Data
public class HealthCheckinSessionEndVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private HealthCheckinSessionVO session;
    private HealthCheckinVO checkin;
}
