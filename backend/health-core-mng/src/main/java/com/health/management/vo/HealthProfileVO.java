package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 健康档案展示对象。校医查看时学号、姓名、联系电话等身份字段会脱敏。
 */
@Data
public class HealthProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String studentNo;
    private String realName;
    private Integer gender;
    private String genderName;
    private Integer age;
    private LocalDate birthDate;
    private String bloodType;
    private BigDecimal height;
    private BigDecimal weight;
    private BigDecimal bmi;
    private String bmiLevel;
    private String college;
    private String major;
    private String grade;
    private String className;
    private String chronicDisease;
    private String allergyHistory;
    private String emergencyContact;
    private String emergencyPhone;
    private String phone;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
