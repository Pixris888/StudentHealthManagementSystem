package com.health.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.health.management.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("health_report")
public class HealthReport extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId; // 用户ID(逻辑关联 sys_user.id)
    @TableField("student_no")
    private String studentNo; // 学号
    @TableField("report_year")
    private Integer reportYear; // 报告年份
    @TableField("report_month")
    private Integer reportMonth; // 报告月份
    @TableField("report_title")
    private String reportTitle; // 报告标题
    @TableField("checkin_days")
    private Integer checkinDays; // 打卡天数
    @TableField("avg_sleep_duration")
    private BigDecimal avgSleepDuration; // 平均睡眠时长(小时)
    @TableField("stay_up_late_count")
    private Integer stayUpLateCount; // 熬夜次数
    @TableField("avg_exercise_duration")
    private Integer avgExerciseDuration; // 平均运动时长(分钟)
    @TableField("exercise_insufficient_count")
    private Integer exerciseInsufficientCount; // 运动不足次数
    @TableField("avg_mood_score")
    private BigDecimal avgMoodScore; // 平均心情评分
    @TableField("report_content")
    private String reportContent; // 报告内容/总结
    @TableField("report_data")
    private String reportData; // 报告结构化指标JSON快照
    @TableField("file_url")
    private String fileUrl; // 报告文件(PDF等)地址
    @TableField("generate_time")
    private LocalDateTime generateTime; // 生成时间
}
