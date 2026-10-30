package com.health.management.convert;

import com.health.management.entity.HealthPhysicalTest;
import com.health.management.entity.HealthProfile;
import com.health.management.entity.SysUser;
import com.health.management.util.HealthMetricUtils;
import com.health.management.vo.HealthPhysicalTestVO;
import com.health.management.vo.HealthProfileVO;

import java.math.BigDecimal;

/**
 * 健康业务实体 → VO 转换，负责派生指标计算和敏感信息脱敏。
 */
public final class HealthConverter {

    private HealthConverter() {
    }

    /**
     * @param desensitize 是否对学号、姓名、联系电话、紧急联系人做脱敏（校医查看时开启）
     */
    public static HealthProfileVO toProfileVO(HealthProfile profile, SysUser user, boolean desensitize) {
        HealthProfileVO vo = new HealthProfileVO();
        if (profile != null) {
            vo.setId(profile.getId());
            vo.setUserId(profile.getUserId());
            vo.setStudentNo(profile.getStudentNo());
            vo.setRealName(profile.getRealName());
            vo.setGender(profile.getGender());
            vo.setAge(profile.getAge());
            vo.setBirthDate(profile.getBirthDate());
            vo.setBloodType(profile.getBloodType());
            vo.setHeight(profile.getHeight());
            vo.setWeight(profile.getWeight());
            vo.setBmi(profile.getBmi());
            vo.setCollege(profile.getCollege());
            vo.setMajor(profile.getMajor());
            vo.setGrade(profile.getGrade());
            vo.setClassName(profile.getClassName());
            vo.setChronicDisease(profile.getChronicDisease());
            vo.setAllergyHistory(profile.getAllergyHistory());
            vo.setEmergencyContact(profile.getEmergencyContact());
            vo.setEmergencyPhone(profile.getEmergencyPhone());
            vo.setRemark(profile.getRemark());
            vo.setCreateTime(profile.getCreateTime());
            vo.setUpdateTime(profile.getUpdateTime());
        }
        if (user != null) {
            vo.setUserId(user.getId());
            // 学号、姓名以账号表为准，避免档案表中的历史快照过期
            vo.setStudentNo(user.getStudentNo());
            vo.setRealName(user.getRealName());
            if (vo.getGender() == null) {
                vo.setGender(user.getGender());
            }
            vo.setPhone(user.getPhone());
        }

        // 年龄、BMI 以实时计算为准，出生日期/身高体重缺失时回退到表内值
        Integer age = HealthMetricUtils.calcAge(vo.getBirthDate());
        if (age != null) {
            vo.setAge(age);
        }
        BigDecimal bmi = HealthMetricUtils.calcBmi(vo.getHeight(), vo.getWeight());
        if (bmi != null) {
            vo.setBmi(bmi);
        }
        vo.setBmiLevel(HealthMetricUtils.bmiLevel(vo.getBmi()));
        vo.setGenderName(HealthMetricUtils.genderName(vo.getGender()));

        if (desensitize) {
            vo.setStudentNo(maskStudentNo(vo.getStudentNo()));
            vo.setRealName(maskName(vo.getRealName()));
            vo.setPhone(maskPhone(vo.getPhone()));
            vo.setEmergencyContact(maskName(vo.getEmergencyContact()));
            vo.setEmergencyPhone(maskPhone(vo.getEmergencyPhone()));
        }
        return vo;
    }

    public static HealthPhysicalTestVO toPhysicalTestVO(HealthPhysicalTest test, boolean desensitize) {
        if (test == null) {
            return null;
        }
        HealthPhysicalTestVO vo = new HealthPhysicalTestVO();
        vo.setId(test.getId());
        vo.setUserId(test.getUserId());
        vo.setStudentNo(test.getStudentNo());
        vo.setTestYear(test.getTestYear());
        vo.setTestTerm(test.getTestTerm());
        vo.setHeight(test.getHeight());
        vo.setWeight(test.getWeight());
        vo.setBmi(test.getBmi());
        vo.setVitalCapacity(test.getVitalCapacity());
        vo.setRun50m(test.getRun50m());
        vo.setSitAndReach(test.getSitAndReach());
        vo.setStandingJump(test.getStandingJump());
        vo.setEnduranceRun(test.getEnduranceRun());
        vo.setStrengthItem(test.getStrengthItem());
        vo.setTotalScore(test.getTotalScore());
        vo.setGradeLevel(test.getGradeLevel());
        vo.setTestDate(test.getTestDate());
        vo.setCreateTime(test.getCreateTime());

        BigDecimal bmi = HealthMetricUtils.calcBmi(test.getHeight(), test.getWeight());
        if (bmi != null) {
            vo.setBmi(bmi);
        }
        if (desensitize) {
            vo.setStudentNo(maskStudentNo(vo.getStudentNo()));
        }
        return vo;
    }

    /**
     * 学号脱敏：保留前4位和末2位，如 20230001 → 2023****01。
     */
    public static String maskStudentNo(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() <= 4) {
            return "****";
        }
        int tail = value.length() >= 8 ? 2 : 1;
        return value.substring(0, 4) + "****" + value.substring(value.length() - tail);
    }

    /**
     * 姓名脱敏：保留姓氏，其余用 * 代替，如 张三 → 张*。
     */
    public static String maskName(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        StringBuilder sb = new StringBuilder(value.substring(0, 1));
        for (int i = 1; i < value.length(); i++) {
            sb.append('*');
        }
        return sb.toString();
    }

    /**
     * 手机号脱敏：保留前3位和末4位，如 13812345678 → 138****5678。
     */
    public static String maskPhone(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() < 7) {
            return "****";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}
