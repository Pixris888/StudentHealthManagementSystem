package com.health.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.health.management.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("health_checkin")
public class HealthCheckin extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId; // 用户ID(逻辑关联 sys_user.id)
    @TableField("student_no")
    private String studentNo; // 学号
    @TableField("checkin_date")
    private LocalDate checkinDate; // 打卡日期
    @TableField("sleep_time")
    private LocalTime sleepTime; // 入睡时间
    @TableField("wake_time")
    private LocalTime wakeTime; // 起床时间
    @TableField("sleep_duration")
    private BigDecimal sleepDuration; // 睡眠时长(小时)
    @TableField("stay_up_late")
    private Integer stayUpLate; // 是否熬夜 0-否 1-是 null-未填写睡眠
    @TableField("sleep_source")
    private Integer sleepSource; // 睡眠来源 0-未填写 1-计时打卡 2-手动填写
    @TableField("exercise_type")
    private String exerciseType; // 运动类型
    @TableField("exercise_duration")
    private Integer exerciseDuration; // 运动时长(分钟)
    @TableField("exercise_intensity")
    private Integer exerciseIntensity; // 运动强度 1-低 2-中 3-高
    @TableField("exercise_sufficient")
    private Integer exerciseSufficient; // 运动是否达标 0-否 1-是 null-未填写运动
    @TableField("exercise_source")
    private Integer exerciseSource; // 运动来源 0-未填写 1-计时打卡 2-手动填写
    @TableField("diet_breakfast")
    private Integer dietBreakfast; // 是否吃早餐 0-否 1-是
    @TableField("diet_balance")
    private Integer dietBalance; // 饮食均衡度 1-不均衡 2-一般 3-均衡
    @TableField("water_intake")
    private Integer waterIntake; // 饮水量(ml)
    @TableField("emotion_type")
    private Integer emotionType; // 情绪类型 1-愉悦 2-平静 3-焦虑 4-低落 5-愤怒
    @TableField("mood_score")
    private Integer moodScore; // 心情评分 1-5
    @TableField("content")
    private String content; // 打卡备注
    @TableField("data_quality")
    private Integer dataQuality; // 数据质量 0-正常 1-异常待复核
    @TableField("abnormal_reason")
    private String abnormalReason; // 异常原因
}
