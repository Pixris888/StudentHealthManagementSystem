package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 学生新增/修改个人体测成绩请求。
 */
@Data
public class HealthPhysicalTestSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "测试年份不能为空")
    @Min(value = 2000, message = "测试年份应在2000-2100之间")
    @Max(value = 2100, message = "测试年份应在2000-2100之间")
    private Integer testYear;

    @NotBlank(message = "测试学期不能为空")
    @Size(max = 20, message = "测试学期长度不能超过20个字符")
    private String testTerm;

    @DecimalMin(value = "50.00", message = "身高应在50-250cm之间")
    @DecimalMax(value = "250.00", message = "身高应在50-250cm之间")
    @Digits(integer = 3, fraction = 2, message = "身高最多保留两位小数")
    private BigDecimal height;

    @DecimalMin(value = "10.00", message = "体重应在10-500kg之间")
    @DecimalMax(value = "500.00", message = "体重应在10-500kg之间")
    @Digits(integer = 3, fraction = 2, message = "体重最多保留两位小数")
    private BigDecimal weight;

    @Min(value = 0, message = "肺活量不能为负数")
    @Max(value = 20000, message = "肺活量数值超出合理范围")
    private Integer vitalCapacity;

    @DecimalMin(value = "0.00", message = "50米跑成绩不能为负数")
    @DecimalMax(value = "120.00", message = "50米跑成绩超出合理范围")
    @Digits(integer = 3, fraction = 2, message = "50米跑成绩最多保留两位小数")
    private BigDecimal run50m;

    @DecimalMin(value = "-50.00", message = "坐位体前屈数值超出合理范围")
    @DecimalMax(value = "100.00", message = "坐位体前屈数值超出合理范围")
    @Digits(integer = 3, fraction = 2, message = "坐位体前屈数值最多保留两位小数")
    private BigDecimal sitAndReach;

    @DecimalMin(value = "0.00", message = "立定跳远成绩不能为负数")
    @DecimalMax(value = "500.00", message = "立定跳远成绩超出合理范围")
    @Digits(integer = 3, fraction = 2, message = "立定跳远成绩最多保留两位小数")
    private BigDecimal standingJump;

    @DecimalMin(value = "0.00", message = "耐力跑成绩不能为负数")
    @DecimalMax(value = "3600.00", message = "耐力跑成绩超出合理范围")
    @Digits(integer = 4, fraction = 2, message = "耐力跑成绩最多保留两位小数")
    private BigDecimal enduranceRun;

    @Min(value = 0, message = "力量项目成绩不能为负数")
    @Max(value = 300, message = "力量项目成绩超出合理范围")
    private Integer strengthItem;

    @DecimalMin(value = "0.00", message = "体测总分不能为负数")
    @DecimalMax(value = "200.00", message = "体测总分超出合理范围")
    @Digits(integer = 3, fraction = 2, message = "体测总分最多保留两位小数")
    private BigDecimal totalScore;

    @Size(max = 10, message = "体测等级长度不能超过10个字符")
    private String gradeLevel;

    private LocalDate testDate;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
