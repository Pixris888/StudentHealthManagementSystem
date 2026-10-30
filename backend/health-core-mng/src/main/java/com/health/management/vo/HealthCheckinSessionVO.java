package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 睡眠/运动计时会话展示对象。
 */
@Data
public class HealthCheckinSessionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String studentNo;
    private LocalDate checkinDate;
    private Integer sessionType;
    private String sessionTypeName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Integer status;
    private String statusName;
    private String exerciseType;
    private Integer exerciseIntensity;
    private String exerciseIntensityName;
    private Integer dataQuality;
    private String dataQualityName;
    private String abnormalReason;
    private LocalDateTime createTime;
}
