package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 每日健康打卡展示对象。
 */
@Data
public class HealthCheckinVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String studentNo;
    private String realName;
    private LocalDate checkinDate;

    /** 今天接口使用：是否已经打卡 */
    private Boolean checkedIn;

    private LocalTime sleepTime;
    private LocalTime wakeTime;
    private BigDecimal sleepDuration;
    private Integer stayUpLate;
    private String stayUpLateName;
    private Integer sleepSource;
    private String sleepSourceName;

    private String exerciseType;
    private Integer exerciseDuration;
    private Integer exerciseIntensity;
    private String exerciseIntensityName;
    private Integer exerciseSufficient;
    private String exerciseSufficientName;
    private Integer exerciseSource;
    private String exerciseSourceName;

    private Integer dietBreakfast;
    private Integer dietBalance;
    private String dietBalanceName;
    private Integer waterIntake;

    private Integer emotionType;
    private String emotionTypeName;
    private Integer moodScore;

    private String content;

    private Integer dataQuality;
    private String dataQualityName;
    private String abnormalReason;
    private Boolean backfilled;

    /** 四个板块是否已填写 */
    private Boolean sleepCompleted;
    private Boolean exerciseCompleted;
    private Boolean dietCompleted;
    private Boolean emotionCompleted;

    private Integer photoCount;
    private List<HealthCheckinPhotoVO> photos;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
