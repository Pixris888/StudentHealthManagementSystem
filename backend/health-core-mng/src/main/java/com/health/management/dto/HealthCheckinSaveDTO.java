package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 手动填写/补录每日打卡请求。
 * 睡眠、运动的计时数据由服务端会话生成；这里用于补卡、手动补填以及饮食/情绪填写。
 */
@Data
public class HealthCheckinSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 打卡日期；为空默认今天，只允许今天及前 N-1 天（N 由 health.checkin.backfill-days 配置）。
     */
    private LocalDate checkinDate;

    /** 入睡时间，和起床时间必须同时填写或同时为空 */
    private LocalTime sleepTime;

    /** 起床时间 */
    private LocalTime wakeTime;

    @Size(max = 50, message = "运动类型长度不能超过50个字符")
    private String exerciseType;

    @Min(value = 1, message = "运动时长至少1分钟")
    @Max(value = 600, message = "运动时长不能超过600分钟")
    private Integer exerciseDuration;

    @Min(value = 1, message = "运动强度只能是1-低、2-中、3-高")
    @Max(value = 3, message = "运动强度只能是1-低、2-中、3-高")
    private Integer exerciseIntensity;

    @Min(value = 0, message = "早餐只能是0-否、1-是")
    @Max(value = 1, message = "早餐只能是0-否、1-是")
    private Integer dietBreakfast;

    @Min(value = 1, message = "饮食均衡度只能是1-3")
    @Max(value = 3, message = "饮食均衡度只能是1-3")
    private Integer dietBalance;

    @Min(value = 0, message = "饮水量不能为负数")
    @Max(value = 10000, message = "饮水量不能超过10000ml")
    private Integer waterIntake;

    @Min(value = 1, message = "情绪类型只能是1-5")
    @Max(value = 5, message = "情绪类型只能是1-5")
    private Integer emotionType;

    @Min(value = 1, message = "心情评分只能是1-5")
    @Max(value = 5, message = "心情评分只能是1-5")
    private Integer moodScore;

    @Size(max = 500, message = "打卡备注长度不能超过500个字符")
    private String content;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
