package com.health.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.health.management.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ai_health_plan")
public class AiHealthPlan extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId; // 用户ID(逻辑关联 sys_user.id)
    @TableField("plan_type")
    private Integer planType; // 计划类型 1-作息计划 2-运动计划 3-综合计划
    @TableField("plan_title")
    private String planTitle; // 计划标题
    @TableField("plan_content")
    private String planContent; // 计划内容
    @TableField("plan_data")
    private String planData; // 结构化计划数据(JSON，可选)
    @TableField("model_name")
    private String modelName; // 生成模型
    @TableField("provider")
    private String provider; // 模型提供方 bailian/ollama/rule/mock
    @TableField("base_checkin_date")
    private LocalDate baseCheckinDate; // 依据打卡日期
    @TableField("start_date")
    private LocalDate startDate; // 计划开始日期
    @TableField("end_date")
    private LocalDate endDate; // 计划结束日期
    @TableField("status")
    private Integer status; // 状态 0-未采纳 1-已采纳
    @TableField("adopted_time")
    private LocalDateTime adoptedTime; // 采纳时间
}
