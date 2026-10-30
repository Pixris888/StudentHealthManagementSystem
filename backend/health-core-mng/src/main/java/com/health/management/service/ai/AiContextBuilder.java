package com.health.management.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.management.common.util.ValidateUtils;
import com.health.management.entity.HealthCheckin;
import com.health.management.entity.HealthPhysicalTest;
import com.health.management.entity.HealthProfile;
import com.health.management.mapper.HealthCheckinMapper;
import com.health.management.mapper.HealthPhysicalTestMapper;
import com.health.management.mapper.HealthProfileMapper;
import com.health.management.util.HealthMetricUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 构建学生健康画像。
 * 只读取生成建议所需的指标，不把姓名、学号、电话写入 Prompt。
 */
@Component
public class AiContextBuilder {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private HealthProfileMapper healthProfileMapper;

    @Autowired
    private HealthCheckinMapper healthCheckinMapper;

    @Autowired
    private HealthPhysicalTestMapper healthPhysicalTestMapper;

    public AiHealthContext build(Long userId) {
        AiHealthContext context = new AiHealthContext();
        context.setUserId(userId);

        HealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<HealthProfile>()
                        .eq(HealthProfile::getUserId, userId)
                        .last("LIMIT 1"));
        context.setProfileText(buildProfileText(profile));

        LocalDate today = LocalDate.now(ZONE);
        List<HealthCheckin> checkins = healthCheckinMapper.selectList(
                new LambdaQueryWrapper<HealthCheckin>()
                        .eq(HealthCheckin::getUserId, userId)
                        .ge(HealthCheckin::getCheckinDate, today.minusDays(6))
                        .le(HealthCheckin::getCheckinDate, today)
                        .orderByAsc(HealthCheckin::getCheckinDate)
                        .orderByAsc(HealthCheckin::getId));
        fillCheckinContext(context, checkins);

        HealthPhysicalTest test = healthPhysicalTestMapper.selectOne(
                new LambdaQueryWrapper<HealthPhysicalTest>()
                        .eq(HealthPhysicalTest::getUserId, userId)
                        .orderByDesc(HealthPhysicalTest::getTestDate)
                        .orderByDesc(HealthPhysicalTest::getId)
                        .last("LIMIT 1"));
        context.setPhysicalTestText(buildPhysicalTestText(test));
        return context;
    }

    private void fillCheckinContext(AiHealthContext context, List<HealthCheckin> checkins) {
        if (checkins == null || checkins.isEmpty()) {
            context.setCheckinDays(0);
            context.setCheckinText("暂无打卡记录。");
            return;
        }

        List<BigDecimal> sleepValues = new ArrayList<>();
        List<Integer> exerciseValues = new ArrayList<>();
        List<BigDecimal> moodValues = new ArrayList<>();
        int stayUpLateCount = 0;
        int exerciseInsufficientCount = 0;
        int breakfastDays = 0;

        for (HealthCheckin checkin : checkins) {
            if (checkin.getSleepDuration() != null) {
                sleepValues.add(checkin.getSleepDuration());
            }
            if (Integer.valueOf(1).equals(checkin.getStayUpLate())) {
                stayUpLateCount++;
            }
            if (checkin.getExerciseDuration() != null) {
                exerciseValues.add(checkin.getExerciseDuration());
            }
            if (Integer.valueOf(0).equals(checkin.getExerciseSufficient())) {
                exerciseInsufficientCount++;
            }
            if (Integer.valueOf(1).equals(checkin.getDietBreakfast())) {
                breakfastDays++;
            }
            if (checkin.getMoodScore() != null) {
                moodValues.add(BigDecimal.valueOf(checkin.getMoodScore()));
            }
        }

        BigDecimal avgSleep = averageDecimal(sleepValues);
        Integer avgExercise = averageInteger(exerciseValues);
        BigDecimal avgMood = averageDecimal(moodValues);
        HealthCheckin latest = checkins.get(checkins.size() - 1);
        context.setCheckinDays(checkins.size());
        context.setLatestCheckinDate(latest.getCheckinDate());
        context.setAvgSleepDuration(avgSleep);
        context.setStayUpLateCount(stayUpLateCount);
        context.setAvgExerciseMinutes(avgExercise);
        context.setExerciseInsufficientCount(exerciseInsufficientCount);
        context.setBreakfastDays(breakfastDays);
        context.setAvgMoodScore(avgMood);

        StringBuilder text = new StringBuilder();
        text.append("打卡 ").append(checkins.size()).append(" 天；");
        text.append("平均睡眠 ").append(avgSleep == null ? "未知" : avgSleep.toPlainString()).append(" 小时；");
        text.append("熬夜 ").append(stayUpLateCount).append(" 次；");
        text.append("平均运动 ").append(avgExercise == null ? "未知" : avgExercise).append(" 分钟；");
        text.append("运动不足 ").append(exerciseInsufficientCount).append(" 次；");
        text.append("早餐 ").append(breakfastDays).append(" 天；");
        text.append("平均心情 ").append(avgMood == null ? "未知" : avgMood.toPlainString()).append("/5。");
        context.setCheckinText(text.toString());
    }

    private String buildProfileText(HealthProfile profile) {
        if (profile == null) {
            return "未完善健康档案。";
        }
        BigDecimal bmi = profile.getBmi();
        if (bmi == null && profile.getHeight() != null && profile.getWeight() != null) {
            bmi = HealthMetricUtils.calcBmi(profile.getHeight(), profile.getWeight());
        }
        Integer age = profile.getAge();
        if (age == null && profile.getBirthDate() != null) {
            age = HealthMetricUtils.calcAge(profile.getBirthDate());
        }

        StringBuilder text = new StringBuilder();
        text.append("年龄 ").append(age == null ? "未知" : age).append("；");
        text.append("性别 ").append(genderName(profile.getGender())).append("；");
        text.append("BMI ").append(bmi == null ? "未知" : bmi.toPlainString()).append("；");
        text.append("身高 ").append(profile.getHeight() == null ? "未知" : profile.getHeight().toPlainString()).append("cm；");
        text.append("体重 ").append(profile.getWeight() == null ? "未知" : profile.getWeight().toPlainString()).append("kg；");
        text.append("慢性病 ").append(hasText(profile.getChronicDisease()) ? profile.getChronicDisease() : "无").append("；");
        text.append("过敏史 ").append(hasText(profile.getAllergyHistory()) ? profile.getAllergyHistory() : "无").append("。");
        return text.toString();
    }

    private String buildPhysicalTestText(HealthPhysicalTest test) {
        if (test == null) {
            return "暂无体测记录。";
        }
        StringBuilder text = new StringBuilder();
        text.append(test.getTestYear() == null ? "未知年份" : test.getTestYear());
        if (hasText(test.getTestTerm())) {
            text.append(" ").append(test.getTestTerm());
        }
        text.append("；BMI ").append(test.getBmi() == null ? "未知" : test.getBmi().toPlainString());
        text.append("；肺活量 ").append(test.getVitalCapacity() == null ? "未知" : test.getVitalCapacity()).append("ml");
        text.append("；50米 ").append(test.getRun50m() == null ? "未知" : test.getRun50m().toPlainString()).append("s");
        text.append("；总分 ").append(test.getTotalScore() == null ? "未知" : test.getTotalScore().toPlainString());
        text.append("；等级 ").append(hasText(test.getGradeLevel()) ? test.getGradeLevel() : "未知").append("。");
        return text.toString();
    }

    private BigDecimal averageDecimal(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            sum = sum.add(value);
        }
        return sum.divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
    }

    private Integer averageInteger(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        int sum = 0;
        for (Integer value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    private String genderName(Integer gender) {
        if (Integer.valueOf(1).equals(gender)) {
            return "男";
        }
        if (Integer.valueOf(2).equals(gender)) {
            return "女";
        }
        return "未知";
    }

    private boolean hasText(String value) {
        return ValidateUtils.hasText(value);
    }
}
