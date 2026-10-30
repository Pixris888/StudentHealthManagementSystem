package com.health.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.management.common.constant.RoleConstants;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.redis.RedisUtils;
import com.health.management.config.MessageProperties;
import com.health.management.constant.MessageConstants;
import com.health.management.entity.HealthCheckin;
import com.health.management.entity.MessageNotification;
import com.health.management.entity.SysUser;
import com.health.management.mapper.HealthCheckinMapper;
import com.health.management.mapper.MessageNotificationMapper;
import com.health.management.mapper.SysUserMapper;
import com.health.management.service.MessageWarningService;
import com.health.management.service.support.HealthConfigService;
import com.health.management.vo.WarningRunResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 健康预警生成实现。
 *
 * 预警规则直接使用打卡模块已经计算好的 stay_up_late / exercise_sufficient 标志，
 * 并跳过 data_quality=1 的异常数据，避免重复计算阈值和误报。
 */
@Slf4j
@Service
public class MessageWarningServiceImpl implements MessageWarningService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TEXT = DateTimeFormatter.ofPattern("M月d日");
    private static final DateTimeFormatter TIME_TEXT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String LOCK_PREFIX = "health:message:warning:lock:";
    private static final int INSERT_BATCH_SIZE = 500;

    @Autowired
    private HealthCheckinMapper healthCheckinMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private HealthConfigService healthConfigService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private MessageProperties messageProperties;

    @Autowired
    private MessageNotificationMapper messageNotificationMapper;

    @Override
    public WarningRunResultVO run(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now(ZONE).minusDays(1) : date;
        if (targetDate.isAfter(LocalDate.now(ZONE))) {
            throw ServiceException.badRequest("不能为未来日期生成健康预警");
        }

        WarningRunResultVO result = new WarningRunResultVO();
        result.setDate(targetDate);

        String lockKey = LOCK_PREFIX + targetDate;
        int lockMinutes = messageProperties.getWarningLockMinutes() == null
                ? 10 : messageProperties.getWarningLockMinutes();
        boolean lockAcquired = false;
        boolean redisAvailable = true;
        try {
            lockAcquired = redisUtils.setIfAbsent(lockKey, UUID.randomUUID().toString(),
                    lockMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("获取预警任务Redis锁失败，降级为数据库唯一键防重: {}", e.getMessage());
        }
        if (redisAvailable && !lockAcquired) {
            result.setLocked(false);
            result.setMessage("该日期的预警任务正在执行，请稍后重试");
            return result;
        }
        result.setLocked(lockAcquired);

        try {
            LambdaQueryWrapper<HealthCheckin> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HealthCheckin::getCheckinDate, targetDate)
                    .eq(HealthCheckin::getDataQuality, 0)
                    .and(w -> w.eq(HealthCheckin::getStayUpLate, 1)
                            .or()
                            .eq(HealthCheckin::getExerciseSufficient, 0));
            List<HealthCheckin> checkins = healthCheckinMapper.selectList(wrapper);
            result.setScannedCount(checkins.size());
            if (checkins.isEmpty()) {
                result.setMessage("该日期没有需要预警的打卡数据");
                return result;
            }

            Map<Long, SysUser> userMap = loadUsers(checkins);
            LocalDateTime now = LocalDateTime.now();
            LocalTime stayUpLateTime = healthConfigService.getStayUpLateTime();
            int exerciseThreshold = healthConfigService.getExerciseThresholdMinutes();
            List<MessageNotification> messages = new ArrayList<>();
            int invalidCheckins = 0;
            for (HealthCheckin checkin : checkins) {
                SysUser user = userMap.get(checkin.getUserId());
                if (user == null
                        || !Integer.valueOf(RoleConstants.USER_TYPE_STUDENT).equals(user.getUserType())
                        || !Integer.valueOf(1).equals(user.getStatus())) {
                    invalidCheckins++;
                    continue;
                }
                if (Integer.valueOf(1).equals(checkin.getStayUpLate())) {
                    messages.add(buildStayUpWarning(checkin, targetDate, now, stayUpLateTime));
                }
                if (Integer.valueOf(0).equals(checkin.getExerciseSufficient())) {
                    messages.add(buildExerciseWarning(checkin, targetDate, now, exerciseThreshold));
                }
            }

            int generated = 0;
            for (int i = 0; i < messages.size(); i += INSERT_BATCH_SIZE) {
                int end = Math.min(i + INSERT_BATCH_SIZE, messages.size());
                generated += messageNotificationMapper.insertIgnoreBatch(messages.subList(i, end));
            }
            int skipped = Math.max(0, messages.size() - generated) + invalidCheckins;
            result.setGeneratedCount(generated);
            result.setSkippedCount(skipped);
            result.setMessage("扫描打卡 " + checkins.size() + " 条，生成预警 "
                    + generated + " 条，跳过 " + skipped + " 条");
            if (!redisAvailable) {
                result.setMessage(result.getMessage() + "（Redis 不可用，已降级执行）");
            }
            return result;
        } finally {
            if (lockAcquired) {
                try {
                    redisUtils.delete(lockKey);
                } catch (Exception e) {
                    log.warn("释放预警任务Redis锁失败: {}", e.getMessage());
                }
            }
        }
    }

    private Map<Long, SysUser> loadUsers(List<HealthCheckin> checkins) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (HealthCheckin checkin : checkins) {
            if (checkin.getUserId() != null) {
                userIds.add(checkin.getUserId());
            }
        }
        Map<Long, SysUser> result = new HashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }
        for (SysUser user : sysUserMapper.selectBatchIds(userIds)) {
            result.put(user.getId(), user);
        }
        return result;
    }

    private MessageNotification buildStayUpWarning(HealthCheckin checkin, LocalDate date,
                                                   LocalDateTime now, LocalTime targetTime) {
        String dateText = date.format(DATE_TEXT);
        String sleepTime = checkin.getSleepTime() == null
                ? "未记录" : checkin.getSleepTime().format(TIME_TEXT);
        String duration = checkin.getSleepDuration() == null
                ? "未记录" : checkin.getSleepDuration().stripTrailingZeros().toPlainString();
        String targetText = targetTime == null ? "23:30" : targetTime.format(TIME_TEXT);

        MessageNotification message = baseWarning(checkin, MessageConstants.TYPE_STAY_UP_LATE,
                "熬夜提醒：昨晚入睡较晚", now);
        message.setContent("你 " + dateText + " 的打卡显示入睡时间为 " + sleepTime
                + "，睡眠时长 " + duration + " 小时。建议今晚尽量在 " + targetText
                + " 前入睡，睡前减少手机使用。如长期入睡困难，建议到校医院进一步评估。");
        message.setDedupKey("warning:" + MessageConstants.TYPE_STAY_UP_LATE + ":"
                + checkin.getUserId() + ":" + date);
        return message;
    }

    private MessageNotification buildExerciseWarning(HealthCheckin checkin, LocalDate date,
                                                      LocalDateTime now, int threshold) {
        String dateText = date.format(DATE_TEXT);
        String duration = checkin.getExerciseDuration() == null
                ? "未记录" : checkin.getExerciseDuration() + " 分钟";

        MessageNotification message = baseWarning(checkin,
                MessageConstants.TYPE_EXERCISE_INSUFFICIENT,
                "运动提醒：昨日运动未达标", now);
        message.setContent("你 " + dateText + " 的运动时长为 " + duration
                + "，未达到每日 " + threshold + " 分钟的目标。建议今天安排快走、慢跑或宿舍力量训练，"
                + "运动前注意热身，循序渐进。");
        message.setDedupKey("warning:" + MessageConstants.TYPE_EXERCISE_INSUFFICIENT + ":"
                + checkin.getUserId() + ":" + date);
        return message;
    }

    private MessageNotification baseWarning(HealthCheckin checkin, int messageType,
                                            String title, LocalDateTime now) {
        MessageNotification message = new MessageNotification();
        message.setReceiverId(checkin.getUserId());
        message.setMessageType(messageType);
        message.setTitle(title);
        message.setSenderId(0L);
        message.setSendType(MessageConstants.SEND_TYPE_PERSONAL);
        message.setPublishTime(now);
        message.setCreateBy(0L);
        message.setCreateTime(now);
        message.setUpdateBy(0L);
        message.setUpdateTime(now);
        message.setRemark("系统自动预警");
        return message;
    }
}
