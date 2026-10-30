package com.health.management.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

/**
 * 健康指标计算工具：BMI、年龄、BMI 等级等。
 */
public final class HealthMetricUtils {

    private HealthMetricUtils() {
    }

    /**
     * BMI = 体重(kg) / 身高(m)^2，四舍五入保留两位小数。
     *
     * @param heightCm 身高，单位厘米
     * @param weightKg 体重，单位千克
     * @return BMI；身高或体重缺失/非法时返回 null
     */
    public static BigDecimal calcBmi(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null
                || heightCm.compareTo(BigDecimal.ZERO) <= 0
                || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal heightM = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal square = heightM.multiply(heightM);
        if (square.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return weightKg.divide(square, 2, RoundingMode.HALF_UP);
    }

    /**
     * 根据出生日期计算周岁。
     */
    public static Integer calcAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        if (birthDate.isAfter(today)) {
            return 0;
        }
        return Period.between(birthDate, today).getYears();
    }

    /**
     * 按中国成人 BMI 标准划分等级：偏瘦 / 正常 / 超重 / 肥胖。
     */
    public static String bmiLevel(BigDecimal bmi) {
        if (bmi == null) {
            return null;
        }
        if (bmi.compareTo(new BigDecimal("18.5")) < 0) {
            return "偏瘦";
        }
        if (bmi.compareTo(new BigDecimal("24")) < 0) {
            return "正常";
        }
        if (bmi.compareTo(new BigDecimal("28")) < 0) {
            return "超重";
        }
        return "肥胖";
    }

    public static String genderName(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        switch (gender) {
            case 1:
                return "男";
            case 2:
                return "女";
            default:
                return "未知";
        }
    }
}
