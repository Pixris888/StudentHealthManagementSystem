package com.health.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.health.management.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 睡眠/运动计时会话。
 * 开始和结束时间由服务端记录，数据库是唯一数据源；Redis 只缓存进行中的会话。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("health_checkin_session")
public class HealthCheckinSession extends BaseEntity {

    public static final int TYPE_SLEEP = 1;
    public static final int TYPE_EXERCISE = 2;

    public static final int STATUS_ONGOING = 0;
    public static final int STATUS_FINISHED = 1;
    public static final int STATUS_EXPIRED = 2;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("student_no")
    private String studentNo;

    @TableField("checkin_date")
    private LocalDate checkinDate;

    @TableField("session_type")
    private Integer sessionType;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("duration_minutes")
    private Integer durationMinutes;

    @TableField("status")
    private Integer status;

    @TableField("exercise_type")
    private String exerciseType;

    @TableField("exercise_intensity")
    private Integer exerciseIntensity;

    @TableField("data_quality")
    private Integer dataQuality;

    @TableField("abnormal_reason")
    private String abnormalReason;
}
