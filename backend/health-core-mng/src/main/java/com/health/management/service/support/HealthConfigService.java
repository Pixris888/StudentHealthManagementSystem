package com.health.management.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.management.common.util.ValidateUtils;
import com.health.management.entity.SysConfig;
import com.health.management.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 读取健康预警阈值（sys_config）。
 * 目前使用：
 *  - health.warn.stayuplate.time      熬夜判定时间，默认 23:30
 *  - health.warn.exercise.minutes     运动达标分钟数，默认 30
 */
@Service
public class HealthConfigService {

    private static final String KEY_STAY_UP_LATE = "health.warn.stayuplate.time";
    private static final String KEY_EXERCISE_MINUTES = "health.warn.exercise.minutes";

    private static final LocalTime DEFAULT_STAY_UP_LATE = LocalTime.of(23, 30);
    private static final int DEFAULT_EXERCISE_MINUTES = 30;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_FORMAT_SECONDS = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private SysConfigService sysConfigService;

    public LocalTime getStayUpLateTime() {
        String value = getValue(KEY_STAY_UP_LATE);
        if (!ValidateUtils.hasText(value)) {
            return DEFAULT_STAY_UP_LATE;
        }
        String text = value.trim();
        try {
            return LocalTime.parse(text, TIME_FORMAT);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalTime.parse(text, TIME_FORMAT_SECONDS);
            } catch (DateTimeParseException ignoredAgain) {
                return DEFAULT_STAY_UP_LATE;
            }
        }
    }

    public int getExerciseThresholdMinutes() {
        String value = getValue(KEY_EXERCISE_MINUTES);
        if (!ValidateUtils.hasText(value)) {
            return DEFAULT_EXERCISE_MINUTES;
        }
        try {
            int minutes = Integer.parseInt(value.trim());
            return minutes > 0 ? minutes : DEFAULT_EXERCISE_MINUTES;
        } catch (NumberFormatException e) {
            return DEFAULT_EXERCISE_MINUTES;
        }
    }

    private String getValue(String key) {
        SysConfig config = sysConfigService.getOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key), false);
        return config == null ? null : config.getConfigValue();
    }
}
