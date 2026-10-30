package com.health.management.service.support;

import com.health.management.config.CheckinProperties;
import com.health.management.entity.HealthCheckin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 打卡规则计算：睡眠时长、熬夜、运动达标、数据质量异常。
 */
@Component
public class CheckinRuleCalculator {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int AFTER_MIDNIGHT_END_MINUTES = 6 * 60;
    private static final int MAX_REASON_LENGTH = 500;

    @Autowired
    private HealthConfigService healthConfigService;

    @Autowired
    private CheckinProperties checkinProperties;

    public LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    /**
     * 由入睡/起床时间计算睡眠时长，跨午夜自动加 24 小时。
     */
    public BigDecimal calcSleepDuration(LocalTime sleepTime, LocalTime wakeTime) {
        if (sleepTime == null || wakeTime == null) {
            return null;
        }
        Duration duration = Duration.between(sleepTime, wakeTime);
        if (!wakeTime.isAfter(sleepTime)) {
            duration = duration.plusHours(24);
        }
        return minutesToHours((int) duration.toMinutes());
    }

    public BigDecimal minutesToHours(Integer minutes) {
        if (minutes == null) {
            return null;
        }
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
    }

    /**
     * 熬夜判定：入睡时间晚于配置阈值，或入睡时间在 00:00～05:59。
     */
    public Integer calcStayUpLate(LocalTime sleepTime) {
        if (sleepTime == null) {
            return null;
        }
        LocalTime threshold = healthConfigService.getStayUpLateTime();
        int minutes = sleepTime.getHour() * 60 + sleepTime.getMinute();
        int thresholdMinutes = threshold.getHour() * 60 + threshold.getMinute();
        boolean afterMidnight = minutes < AFTER_MIDNIGHT_END_MINUTES;
        boolean lateEvening = minutes > thresholdMinutes;
        return (afterMidnight || lateEvening) ? 1 : 0;
    }

    public Integer calcExerciseSufficient(Integer durationMinutes) {
        if (durationMinutes == null) {
            return null;
        }
        return durationMinutes >= healthConfigService.getExerciseThresholdMinutes() ? 1 : 0;
    }

    public boolean isSleepDurationAbnormal(BigDecimal hours) {
        if (hours == null) {
            return false;
        }
        return hours.compareTo(checkinProperties.getSleepMinHours()) < 0
                || hours.compareTo(checkinProperties.getSleepMaxHours()) > 0;
    }

    public boolean isExerciseTooLong(Integer minutes) {
        return minutes != null && minutes > checkinProperties.getExerciseMaxMinutes();
    }

    public boolean isWaterTooMuch(Integer waterIntake) {
        return waterIntake != null && waterIntake > checkinProperties.getWaterMaxMl();
    }

    /**
     * 计算每日汇总记录的异常原因。
     *
     * @param checkin      每日记录
     * @param extraReasons 来自计时会话的异常原因
     */
    public List<String> dailyReasons(HealthCheckin checkin, List<String> extraReasons) {
        List<String> reasons = new ArrayList<>();
        if (checkin == null) {
            return reasons;
        }
        if (isSleepDurationAbnormal(checkin.getSleepDuration())) {
            reasons.add("睡眠时长异常(" + checkin.getSleepDuration() + "小时)");
        }
        if (isExerciseTooLong(checkin.getExerciseDuration())) {
            reasons.add("运动时长异常(" + checkin.getExerciseDuration() + "分钟)");
        }
        if (isWaterTooMuch(checkin.getWaterIntake())) {
            reasons.add("饮水量异常(" + checkin.getWaterIntake() + "ml)");
        }
        // 当天手动填写睡眠时，提交时间不应早于填写的起床时间
        if (checkin.getCheckinDate() != null
                && checkin.getCheckinDate().equals(today())
                && checkin.getWakeTime() != null
                && now().toLocalTime().isBefore(checkin.getWakeTime())) {
            reasons.add("提交时间早于填写的起床时间");
        }
        if (extraReasons != null) {
            reasons.addAll(extraReasons);
        }
        return dedupe(reasons);
    }

    /**
     * 计时会话自身的异常原因。
     */
    public List<String> sessionReasons(Integer sessionType, Integer durationMinutes) {
        List<String> reasons = new ArrayList<>();
        if (durationMinutes == null) {
            return reasons;
        }
        BigDecimal hours = minutesToHours(durationMinutes);
        if (sessionType != null && sessionType == 1 && isSleepDurationAbnormal(hours)) {
            reasons.add("睡眠时长异常(" + hours + "小时)");
        }
        if (sessionType != null && sessionType == 2 && isExerciseTooLong(durationMinutes)) {
            reasons.add("单次运动时长异常(" + durationMinutes + "分钟)");
        }
        return reasons;
    }

    public int qualityOf(List<String> reasons) {
        return reasons == null || reasons.isEmpty() ? 0 : 1;
    }

    public String joinReasons(List<String> reasons) {
        List<String> deduped = dedupe(reasons);
        if (deduped.isEmpty()) {
            return "";
        }
        String joined = String.join("；", deduped);
        return joined.length() > MAX_REASON_LENGTH ? joined.substring(0, MAX_REASON_LENGTH) : joined;
    }

    /**
     * 补卡判断：没有计时来源，且记录创建日期晚于打卡日期。
     */
    public boolean isBackfilled(HealthCheckin checkin) {
        if (checkin == null || checkin.getCreateTime() == null || checkin.getCheckinDate() == null) {
            return false;
        }
        boolean hasTimer = Integer.valueOf(1).equals(checkin.getSleepSource())
                || Integer.valueOf(1).equals(checkin.getExerciseSource());
        return !hasTimer && checkin.getCreateTime().toLocalDate().isAfter(checkin.getCheckinDate());
    }

    private List<String> dedupe(List<String> reasons) {
        Set<String> set = new LinkedHashSet<>();
        if (reasons != null) {
            for (String reason : reasons) {
                if (reason != null && !reason.trim().isEmpty()) {
                    set.add(reason.trim());
                }
            }
        }
        return new ArrayList<>(set);
    }
}
