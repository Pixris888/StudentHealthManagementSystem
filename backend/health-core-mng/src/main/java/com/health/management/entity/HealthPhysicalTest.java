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
@TableName("health_physical_test")
public class HealthPhysicalTest extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId; // 用户ID(逻辑关联 sys_user.id)
    @TableField("student_no")
    private String studentNo; // 学号
    @TableField("test_year")
    private Integer testYear; // 测试年份
    @TableField("test_term")
    private String testTerm; // 学期 如 上学期/下学期
    @TableField("height")
    private BigDecimal height; // 身高(cm)
    @TableField("weight")
    private BigDecimal weight; // 体重(kg)
    @TableField("bmi")
    private BigDecimal bmi; // BMI
    @TableField("vital_capacity")
    private Integer vitalCapacity; // 肺活量(ml)
    @TableField("run_50m")
    private BigDecimal run50m; // 50米跑(秒)
    @TableField("sit_and_reach")
    private BigDecimal sitAndReach; // 坐位体前屈(cm)
    @TableField("standing_jump")
    private BigDecimal standingJump; // 立定跳远(cm)
    @TableField("endurance_run")
    private BigDecimal enduranceRun; // 耐力跑 1000/800米(秒)
    @TableField("strength_item")
    private Integer strengthItem; // 力量项目 引体向上/仰卧起坐(个)
    @TableField("total_score")
    private BigDecimal totalScore; // 总分
    @TableField("grade_level")
    private String gradeLevel; // 等级 优秀/良好/及格/不及格
    @TableField("test_date")
    private LocalDate testDate; // 测试日期
}
