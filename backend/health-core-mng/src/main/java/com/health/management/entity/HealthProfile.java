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

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("health_profile")
public class HealthProfile extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId; // 用户ID(逻辑关联 sys_user.id)
    @TableField("student_no")
    private String studentNo; // 学号
    @TableField("real_name")
    private String realName; // 姓名
    @TableField("gender")
    private Integer gender; // 性别 0-未知 1-男 2-女
    @TableField("age")
    private Integer age; // 年龄
    @TableField("birth_date")
    private LocalDate birthDate; // 出生日期
    @TableField("blood_type")
    private String bloodType; // 血型
    @TableField("height")
    private BigDecimal height; // 身高(cm)
    @TableField("weight")
    private BigDecimal weight; // 体重(kg)
    @TableField("bmi")
    private BigDecimal bmi; // BMI指数
    @TableField("college")
    private String college; // 学院
    @TableField("major")
    private String major; // 专业
    @TableField("grade")
    private String grade; // 年级
    @TableField("class_name")
    private String className; // 班级
    @TableField("chronic_disease")
    private String chronicDisease; // 慢性病史
    @TableField("allergy_history")
    private String allergyHistory; // 过敏史
    @TableField("emergency_contact")
    private String emergencyContact; // 紧急联系人
    @TableField("emergency_phone")
    private String emergencyPhone; // 紧急联系电话
}
