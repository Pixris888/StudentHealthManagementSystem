package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 学生保存个人健康档案请求。
 * 学号、姓名、年龄、BMI 由服务端维护，不允许前端直接修改。
 */
@Data
public class HealthProfileSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 0, message = "性别只能是0-未知、1-男、2-女")
    @Max(value = 2, message = "性别只能是0-未知、1-男、2-女")
    private Integer gender;

    @Past(message = "出生日期必须早于今天")
    private LocalDate birthDate;

    @Size(max = 10, message = "血型长度不能超过10个字符")
    private String bloodType;

    @DecimalMin(value = "50.00", message = "身高应在50-250cm之间")
    @DecimalMax(value = "250.00", message = "身高应在50-250cm之间")
    @Digits(integer = 3, fraction = 2, message = "身高最多保留两位小数")
    private BigDecimal height;

    @DecimalMin(value = "10.00", message = "体重应在10-500kg之间")
    @DecimalMax(value = "500.00", message = "体重应在10-500kg之间")
    @Digits(integer = 3, fraction = 2, message = "体重最多保留两位小数")
    private BigDecimal weight;

    @Size(max = 100, message = "学院名称长度不能超过100个字符")
    private String college;

    @Size(max = 100, message = "专业名称长度不能超过100个字符")
    private String major;

    @Size(max = 20, message = "年级长度不能超过20个字符")
    private String grade;

    @Size(max = 50, message = "班级名称长度不能超过50个字符")
    private String className;

    @Size(max = 500, message = "慢性病史长度不能超过500个字符")
    private String chronicDisease;

    @Size(max = 500, message = "过敏史长度不能超过500个字符")
    private String allergyHistory;

    @Size(max = 50, message = "紧急联系人长度不能超过50个字符")
    private String emergencyContact;

    @Pattern(regexp = "^$|^[0-9+\\- ]{5,20}$", message = "紧急联系电话格式不正确")
    private String emergencyPhone;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
