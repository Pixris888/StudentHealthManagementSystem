package com.health.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.common.constant.RoleConstants;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.config.CheckinProperties;
import com.health.management.convert.HealthCheckinConverter;
import com.health.management.dto.HealthCheckinQueryDTO;
import com.health.management.dto.HealthCheckinSaveDTO;
import com.health.management.entity.HealthCheckin;
import com.health.management.entity.HealthCheckinPhoto;
import com.health.management.entity.HealthCheckinSession;
import com.health.management.entity.SysUser;
import com.health.management.mapper.HealthCheckinMapper;
import com.health.management.mapper.HealthCheckinPhotoMapper;
import com.health.management.mapper.HealthCheckinSessionMapper;
import com.health.management.mapper.SysUserMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.service.HealthCheckinPhotoService;
import com.health.management.service.HealthCheckinService;
import com.health.management.service.support.CheckinRuleCalculator;
import com.health.management.service.support.HealthUserSupport;
import com.health.management.vo.HealthCheckinPhotoVO;
import com.health.management.vo.HealthCheckinSummaryVO;
import com.health.management.vo.HealthCheckinVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 每日健康打卡业务实现。
 */
@Service
public class HealthCheckinServiceImpl extends ServiceImpl<HealthCheckinMapper, HealthCheckin>
        implements HealthCheckinService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final BigDecimal MIN_SLEEP_HOURS = new BigDecimal("0.5");
    private static final BigDecimal MAX_SLEEP_HOURS = new BigDecimal("24");

    @Autowired
    private HealthUserSupport healthUserSupport;

    @Autowired
    private HealthCheckinPhotoService healthCheckinPhotoService;

    @Autowired
    private HealthCheckinSessionMapper healthCheckinSessionMapper;

    @Autowired
    private HealthCheckinPhotoMapper healthCheckinPhotoMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private CheckinRuleCalculator checkinRuleCalculator;

    @Autowired
    private CheckinProperties checkinProperties;

    @Override
    public HealthCheckinVO getToday() {
        SysUser user = healthUserSupport.requireStudent();
        LocalDate today = checkinRuleCalculator.today();
        HealthCheckin checkin = findByUserAndDate(user.getId(), today);
        List<HealthCheckinPhotoVO> photos = new ArrayList<>();
        int photoCount = 0;
        if (checkin != null) {
            photos = healthCheckinPhotoService.listByCheckin(checkin.getId());
            photoCount = photos.size();
        }
        HealthCheckinVO vo = HealthCheckinConverter.toCheckinVO(
                checkin, user, false, checkinRuleCalculator.isBackfilled(checkin), photos, photoCount);
        vo.setCheckinDate(today);
        vo.setCheckedIn(checkin != null);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthCheckinVO saveManual(HealthCheckinSaveDTO dto) {
        SysUser user = healthUserSupport.requireStudent();
        ValidateUtils.requireNotNull(dto, "请求参数");
        LocalDate date = resolveCheckinDate(dto.getCheckinDate());
        HealthCheckin checkin = findByUserAndDate(user.getId(), date);
        boolean insert = checkin == null;
        if (insert) {
            checkin = new HealthCheckin();
            checkin.setUserId(user.getId());
            checkin.setStudentNo(user.getStudentNo());
            checkin.setCheckinDate(date);
            checkin.setSleepSource(0);
            checkin.setExerciseSource(0);
            checkin.setDataQuality(0);
            checkin.setAbnormalReason("");
        }
        applyManualFields(user, checkin, dto, date);
        persist(checkin, insert);
        return buildOwnerVO(checkin, user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthCheckinVO updateManual(Long id, HealthCheckinSaveDTO dto) {
        SysUser user = healthUserSupport.requireStudent();
        ValidateUtils.requireNotNull(id, "打卡记录ID");
        ValidateUtils.requireNotNull(dto, "请求参数");
        HealthCheckin checkin = getById(id);
        if (checkin == null || !user.getId().equals(checkin.getUserId())) {
            throw ServiceException.notFound("打卡记录不存在");
        }
        if (!isWithinBackfill(checkin.getCheckinDate())) {
            throw ServiceException.badRequest("只能修改最近" + checkinProperties.getBackfillDays() + "天的打卡");
        }
        applyManualFields(user, checkin, dto, checkin.getCheckinDate());
        updateById(checkin);
        return buildOwnerVO(checkin, user);
    }

    @Override
    public IPage<HealthCheckinVO> pageMine(HealthCheckinQueryDTO query) {
        SysUser user = healthUserSupport.requireStudent();
        if (query == null) {
            query = new HealthCheckinQueryDTO();
        }
        ValidateUtils.requirePage(query.getPage(), query.getSize());
        validateDateRange(query.getStartDate(), query.getEndDate());
        int current = query.getPage() == null ? 1 : query.getPage();
        int size = query.getSize() == null ? 10 : query.getSize();

        LambdaQueryWrapper<HealthCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthCheckin::getUserId, user.getId());
        if (query.getStartDate() != null) {
            wrapper.ge(HealthCheckin::getCheckinDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(HealthCheckin::getCheckinDate, query.getEndDate());
        }
        if (query.getStayUpLate() != null) {
            wrapper.eq(HealthCheckin::getStayUpLate, query.getStayUpLate());
        }
        if (query.getExerciseSufficient() != null) {
            wrapper.eq(HealthCheckin::getExerciseSufficient, query.getExerciseSufficient());
        }
        if (query.getDataQuality() != null) {
            wrapper.eq(HealthCheckin::getDataQuality, query.getDataQuality());
        }
        wrapper.orderByDesc(HealthCheckin::getCheckinDate)
                .orderByDesc(HealthCheckin::getId);

        IPage<HealthCheckin> page = page(new Page<>(current, size), wrapper);
        Map<Long, Integer> photoCounts = loadPhotoCounts(collectCheckinIds(page.getRecords()));
        Page<HealthCheckinVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<HealthCheckinVO> records = new ArrayList<>();
        for (HealthCheckin checkin : page.getRecords()) {
            records.add(HealthCheckinConverter.toCheckinVO(
                    checkin, user, false, checkinRuleCalculator.isBackfilled(checkin),
                    null, photoCounts.getOrDefault(checkin.getId(), 0)));
        }
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public HealthCheckinVO getDetail(Long id) {
        ValidateUtils.requireNotNull(id, "打卡记录ID");
        SysUser current = healthUserSupport.requireUser();
        HealthCheckin checkin = getById(id);
        if (checkin == null) {
            throw ServiceException.notFound("打卡记录不存在");
        }
        if (current.getId().equals(checkin.getUserId())) {
            return buildOwnerVO(checkin, current);
        }
        if (!SecurityUtils.isDoctorOrAdmin()) {
            throw ServiceException.forbidden("没有访问权限");
        }
        SysUser target = sysUserMapper.selectById(checkin.getUserId());
        boolean desensitize = !SecurityUtils.hasRole(RoleConstants.SUPER_ADMIN);
        int photoCount = healthCheckinPhotoService.countByCheckin(id);
        return HealthCheckinConverter.toCheckinVO(
                checkin, target, desensitize, checkinRuleCalculator.isBackfilled(checkin), null, photoCount);
    }

    @Override
    public IPage<HealthCheckinVO> pageAll(HealthCheckinQueryDTO query) {
        if (!SecurityUtils.isDoctorOrAdmin()) {
            throw ServiceException.forbidden("仅校医或超级管理员可查看打卡记录");
        }
        if (query == null) {
            query = new HealthCheckinQueryDTO();
        }
        ValidateUtils.requirePage(query.getPage(), query.getSize());
        validateDateRange(query.getStartDate(), query.getEndDate());
        int current = query.getPage() == null ? 1 : query.getPage();
        int size = query.getSize() == null ? 10 : query.getSize();

        LambdaQueryWrapper<HealthCheckin> wrapper = new LambdaQueryWrapper<>();
        if (ValidateUtils.hasText(query.getStudentNo())) {
            wrapper.like(HealthCheckin::getStudentNo, query.getStudentNo().trim());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(HealthCheckin::getCheckinDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(HealthCheckin::getCheckinDate, query.getEndDate());
        }
        if (query.getStayUpLate() != null) {
            wrapper.eq(HealthCheckin::getStayUpLate, query.getStayUpLate());
        }
        if (query.getExerciseSufficient() != null) {
            wrapper.eq(HealthCheckin::getExerciseSufficient, query.getExerciseSufficient());
        }
        if (query.getDataQuality() != null) {
            wrapper.eq(HealthCheckin::getDataQuality, query.getDataQuality());
        }
        wrapper.orderByDesc(HealthCheckin::getCheckinDate)
                .orderByDesc(HealthCheckin::getId);

        IPage<HealthCheckin> page = page(new Page<>(current, size), wrapper);
        Map<Long, SysUser> userMap = loadUsers(page.getRecords());
        Map<Long, Integer> photoCounts = loadPhotoCounts(collectCheckinIds(page.getRecords()));
        boolean desensitize = !SecurityUtils.hasRole(RoleConstants.SUPER_ADMIN);

        Page<HealthCheckinVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<HealthCheckinVO> records = new ArrayList<>();
        for (HealthCheckin checkin : page.getRecords()) {
            records.add(HealthCheckinConverter.toCheckinVO(
                    checkin, userMap.get(checkin.getUserId()), desensitize,
                    checkinRuleCalculator.isBackfilled(checkin), null,
                    photoCounts.getOrDefault(checkin.getId(), 0)));
        }
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public HealthCheckinSummaryVO summary(String month) {
        SysUser user = healthUserSupport.requireStudent();
        YearMonth yearMonth = parseMonth(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        LocalDate today = checkinRuleCalculator.today();

        List<HealthCheckin> records = list(new LambdaQueryWrapper<HealthCheckin>()
                .eq(HealthCheckin::getUserId, user.getId())
                .ge(HealthCheckin::getCheckinDate, start)
                .le(HealthCheckin::getCheckinDate, end)
                .orderByAsc(HealthCheckin::getCheckinDate));

        int checkinDays = records.size();
        int lateCount = 0;
        int exerciseDays = 0;
        int totalExerciseMinutes = 0;
        int breakfastDays = 0;
        int abnormalCount = 0;
        int sleepCount = 0;
        int waterCount = 0;
        int moodCount = 0;
        BigDecimal sleepSum = BigDecimal.ZERO;
        BigDecimal moodSum = BigDecimal.ZERO;
        long waterSum = 0L;
        for (HealthCheckin checkin : records) {
            if (Integer.valueOf(1).equals(checkin.getStayUpLate())) {
                lateCount++;
            }
            if (checkin.getExerciseDuration() != null && checkin.getExerciseDuration() > 0) {
                exerciseDays++;
                totalExerciseMinutes += checkin.getExerciseDuration();
            }
            if (Integer.valueOf(1).equals(checkin.getDietBreakfast())) {
                breakfastDays++;
            }
            if (Integer.valueOf(1).equals(checkin.getDataQuality())) {
                abnormalCount++;
            }
            if (checkin.getSleepDuration() != null) {
                sleepSum = sleepSum.add(checkin.getSleepDuration());
                sleepCount++;
            }
            if (checkin.getWaterIntake() != null) {
                waterSum += checkin.getWaterIntake();
                waterCount++;
            }
            if (checkin.getMoodScore() != null) {
                moodSum = moodSum.add(BigDecimal.valueOf(checkin.getMoodScore()));
                moodCount++;
            }
        }

        int monthDays = yearMonth.lengthOfMonth();
        BigDecimal rate = monthDays == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(checkinDays * 100.0)
                .divide(BigDecimal.valueOf(monthDays), 1, RoundingMode.HALF_UP);

        HealthCheckinSummaryVO vo = new HealthCheckinSummaryVO();
        vo.setMonth(yearMonth.toString());
        vo.setCheckinDays(checkinDays);
        vo.setMonthDays(monthDays);
        vo.setCheckinRate(rate);
        vo.setConsecutiveDays(calcConsecutiveDays(user.getId(), today));
        vo.setLateCount(lateCount);
        vo.setExerciseDays(exerciseDays);
        vo.setTotalExerciseMinutes(totalExerciseMinutes);
        vo.setAvgSleepDuration(sleepCount == 0 ? null
                : sleepSum.divide(BigDecimal.valueOf(sleepCount), 1, RoundingMode.HALF_UP));
        vo.setAvgWaterIntake(waterCount == 0 ? null
                : BigDecimal.valueOf(waterSum)
                .divide(BigDecimal.valueOf(waterCount), 0, RoundingMode.HALF_UP).intValue());
        vo.setAvgMoodScore(moodCount == 0 ? null
                : moodSum.divide(BigDecimal.valueOf(moodCount), 1, RoundingMode.HALF_UP));
        vo.setBreakfastDays(breakfastDays);
        vo.setAbnormalCount(abnormalCount);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthCheckinVO syncFromSession(HealthCheckinSession session) {
        ValidateUtils.requireNotNull(session, "计时会话");
        ValidateUtils.requireNotNull(session.getUserId(), "用户ID");
        ValidateUtils.requireNotNull(session.getCheckinDate(), "打卡日期");
        SysUser user = sysUserMapper.selectById(session.getUserId());
        if (user == null) {
            throw ServiceException.notFound("用户不存在");
        }

        HealthCheckin checkin = findByUserAndDate(session.getUserId(), session.getCheckinDate());
        boolean insert = checkin == null;
        if (insert) {
            checkin = new HealthCheckin();
            checkin.setUserId(session.getUserId());
            checkin.setStudentNo(user.getStudentNo());
            checkin.setCheckinDate(session.getCheckinDate());
            checkin.setSleepSource(0);
            checkin.setExerciseSource(0);
            checkin.setDataQuality(0);
            checkin.setAbnormalReason("");
        }

        if (Integer.valueOf(HealthCheckinSession.TYPE_SLEEP).equals(session.getSessionType())) {
            aggregateSleep(checkin, session);
        } else if (Integer.valueOf(HealthCheckinSession.TYPE_EXERCISE).equals(session.getSessionType())) {
            aggregateExercise(checkin, session);
        }

        List<String> extraReasons = collectSessionReasons(session.getUserId(), session.getCheckinDate());
        List<String> reasons = checkinRuleCalculator.dailyReasons(checkin, extraReasons);
        checkin.setDataQuality(checkinRuleCalculator.qualityOf(reasons));
        checkin.setAbnormalReason(checkinRuleCalculator.joinReasons(reasons));
        persist(checkin, insert);
        return buildOwnerVO(checkin, user);
    }

    /**
     * 手动填写/补录字段。已有计时记录的板块不允许再手动填写。
     */
    private void applyManualFields(SysUser user, HealthCheckin checkin, HealthCheckinSaveDTO dto, LocalDate date) {
        checkin.setStudentNo(user.getStudentNo());

        boolean timerSleep = hasTimerSession(user.getId(), date, HealthCheckinSession.TYPE_SLEEP);
        boolean timerExercise = hasTimerSession(user.getId(), date, HealthCheckinSession.TYPE_EXERCISE);

        boolean hasSleepInput = dto.getSleepTime() != null || dto.getWakeTime() != null;
        if (hasSleepInput) {
            if (timerSleep) {
                throw ServiceException.conflict("该日期已有睡眠计时记录，不能手动填写睡眠时间");
            }
            if (dto.getSleepTime() == null || dto.getWakeTime() == null) {
                throw ServiceException.badRequest("入睡时间和起床时间必须同时填写");
            }
            BigDecimal duration = checkinRuleCalculator.calcSleepDuration(dto.getSleepTime(), dto.getWakeTime());
            if (duration == null || duration.compareTo(MIN_SLEEP_HOURS) < 0
                    || duration.compareTo(MAX_SLEEP_HOURS) > 0) {
                throw ServiceException.badRequest("睡眠时长需在0.5-24小时之间");
            }
            checkin.setSleepTime(dto.getSleepTime());
            checkin.setWakeTime(dto.getWakeTime());
            checkin.setSleepDuration(duration);
            checkin.setStayUpLate(checkinRuleCalculator.calcStayUpLate(dto.getSleepTime()));
            checkin.setSleepSource(2);
        }

        boolean hasExerciseInput = dto.getExerciseDuration() != null
                || ValidateUtils.hasText(dto.getExerciseType())
                || dto.getExerciseIntensity() != null;
        if (hasExerciseInput) {
            if (timerExercise) {
                throw ServiceException.conflict("该日期已有运动计时记录，不能手动填写运动时长");
            }
            if (dto.getExerciseDuration() == null) {
                throw ServiceException.badRequest("填写运动类型或强度时，必须同时填写运动时长");
            }
            if (!ValidateUtils.hasText(dto.getExerciseType())) {
                throw ServiceException.badRequest("运动时长大于0时，运动类型不能为空");
            }
            if (dto.getExerciseIntensity() == null) {
                throw ServiceException.badRequest("运动时长大于0时，运动强度不能为空");
            }
            checkin.setExerciseDuration(dto.getExerciseDuration());
            checkin.setExerciseType(dto.getExerciseType().trim());
            checkin.setExerciseIntensity(dto.getExerciseIntensity());
            checkin.setExerciseSufficient(
                    checkinRuleCalculator.calcExerciseSufficient(dto.getExerciseDuration()));
            checkin.setExerciseSource(2);
        }

        if (dto.getDietBreakfast() != null) {
            checkin.setDietBreakfast(dto.getDietBreakfast());
        }
        if (dto.getDietBalance() != null) {
            checkin.setDietBalance(dto.getDietBalance());
        }
        if (dto.getWaterIntake() != null) {
            checkin.setWaterIntake(dto.getWaterIntake());
        }

        if (dto.getEmotionType() != null || dto.getMoodScore() != null) {
            Integer emotionType = dto.getEmotionType();
            Integer moodScore = dto.getMoodScore();
            if (emotionType == null) {
                emotionType = emotionTypeFromScore(moodScore);
            }
            if (moodScore == null) {
                moodScore = scoreFromEmotionType(emotionType);
            }
            checkin.setEmotionType(emotionType);
            checkin.setMoodScore(moodScore);
        }

        if (dto.getContent() != null) {
            checkin.setContent(dto.getContent().trim());
        }
        if (dto.getRemark() != null) {
            checkin.setRemark(trimToNull(dto.getRemark()));
        }

        List<String> extraReasons = collectSessionReasons(user.getId(), date);
        List<String> reasons = checkinRuleCalculator.dailyReasons(checkin, extraReasons);
        checkin.setDataQuality(checkinRuleCalculator.qualityOf(reasons));
        checkin.setAbnormalReason(checkinRuleCalculator.joinReasons(reasons));
    }

    /**
     * 日汇总的睡眠字段取当天所有已结束睡眠会话的汇总，支持分段睡眠。
     */
    private void aggregateSleep(HealthCheckin checkin, HealthCheckinSession current) {
        List<HealthCheckinSession> sessions = finishedSessions(
                checkin.getUserId(), checkin.getCheckinDate(), HealthCheckinSession.TYPE_SLEEP);
        if (sessions.isEmpty()) {
            sessions = Collections.singletonList(current);
        }
        int totalMinutes = 0;
        LocalDateTime earliestStart = null;
        LocalDateTime latestEnd = null;
        boolean anyLate = false;
        for (HealthCheckinSession session : sessions) {
            if (session.getDurationMinutes() != null) {
                totalMinutes += session.getDurationMinutes();
            }
            if (session.getStartTime() != null
                    && (earliestStart == null || session.getStartTime().isBefore(earliestStart))) {
                earliestStart = session.getStartTime();
            }
            if (session.getEndTime() != null
                    && (latestEnd == null || session.getEndTime().isAfter(latestEnd))) {
                latestEnd = session.getEndTime();
            }
            Integer late = checkinRuleCalculator.calcStayUpLate(
                    session.getStartTime() == null ? null : session.getStartTime().toLocalTime());
            if (Integer.valueOf(1).equals(late)) {
                anyLate = true;
            }
        }
        if (earliestStart != null) {
            checkin.setSleepTime(earliestStart.toLocalTime());
        }
        if (latestEnd != null) {
            checkin.setWakeTime(latestEnd.toLocalTime());
        }
        checkin.setSleepDuration(checkinRuleCalculator.minutesToHours(totalMinutes));
        checkin.setStayUpLate(anyLate ? 1 : 0);
        checkin.setSleepSource(1);
    }

    /**
     * 日汇总的运动字段取当天所有已结束运动会话的合计。
     */
    private void aggregateExercise(HealthCheckin checkin, HealthCheckinSession current) {
        List<HealthCheckinSession> sessions = finishedSessions(
                checkin.getUserId(), checkin.getCheckinDate(), HealthCheckinSession.TYPE_EXERCISE);
        if (sessions.isEmpty()) {
            sessions = Collections.singletonList(current);
        }
        int totalMinutes = 0;
        Integer maxIntensity = null;
        Set<String> types = new LinkedHashSet<>();
        for (HealthCheckinSession session : sessions) {
            if (session.getDurationMinutes() != null) {
                totalMinutes += session.getDurationMinutes();
            }
            if (session.getExerciseIntensity() != null
                    && (maxIntensity == null || session.getExerciseIntensity() > maxIntensity)) {
                maxIntensity = session.getExerciseIntensity();
            }
            if (ValidateUtils.hasText(session.getExerciseType())) {
                types.add(session.getExerciseType().trim());
            }
        }
        checkin.setExerciseDuration(totalMinutes);
        checkin.setExerciseIntensity(maxIntensity);
        if (types.isEmpty()) {
            checkin.setExerciseType(null);
        } else if (types.size() == 1) {
            checkin.setExerciseType(types.iterator().next());
        } else {
            checkin.setExerciseType("多种运动");
        }
        checkin.setExerciseSufficient(checkinRuleCalculator.calcExerciseSufficient(totalMinutes));
        checkin.setExerciseSource(1);
    }

    private List<HealthCheckinSession> finishedSessions(Long userId, LocalDate date, Integer sessionType) {
        return healthCheckinSessionMapper.selectList(new LambdaQueryWrapper<HealthCheckinSession>()
                .eq(HealthCheckinSession::getUserId, userId)
                .eq(HealthCheckinSession::getCheckinDate, date)
                .eq(HealthCheckinSession::getSessionType, sessionType)
                .eq(HealthCheckinSession::getStatus, HealthCheckinSession.STATUS_FINISHED)
                .orderByAsc(HealthCheckinSession::getStartTime));
    }

    private List<String> collectSessionReasons(Long userId, LocalDate date) {
        List<String> reasons = new ArrayList<>();
        List<HealthCheckinSession> sessions = healthCheckinSessionMapper.selectList(
                new LambdaQueryWrapper<HealthCheckinSession>()
                        .eq(HealthCheckinSession::getUserId, userId)
                        .eq(HealthCheckinSession::getCheckinDate, date)
                        .eq(HealthCheckinSession::getStatus, HealthCheckinSession.STATUS_FINISHED));
        for (HealthCheckinSession session : sessions) {
            if (Integer.valueOf(1).equals(session.getDataQuality())
                    && ValidateUtils.hasText(session.getAbnormalReason())) {
                for (String reason : session.getAbnormalReason().split("；")) {
                    if (ValidateUtils.hasText(reason)) {
                        reasons.add(reason.trim());
                    }
                }
            }
        }
        return reasons;
    }

    private boolean hasTimerSession(Long userId, LocalDate date, Integer sessionType) {
        Long count = healthCheckinSessionMapper.selectCount(
                new LambdaQueryWrapper<HealthCheckinSession>()
                        .eq(HealthCheckinSession::getUserId, userId)
                        .eq(HealthCheckinSession::getCheckinDate, date)
                        .eq(HealthCheckinSession::getSessionType, sessionType)
                        .eq(HealthCheckinSession::getStatus, HealthCheckinSession.STATUS_FINISHED));
        return count != null && count > 0;
    }

    private HealthCheckin findByUserAndDate(Long userId, LocalDate date) {
        return getOne(new LambdaQueryWrapper<HealthCheckin>()
                .eq(HealthCheckin::getUserId, userId)
                .eq(HealthCheckin::getCheckinDate, date), false);
    }

    private void persist(HealthCheckin checkin, boolean insert) {
        if (insert) {
            save(checkin);
        } else {
            // 从数据库加载的实体 updateTime 非空，strictUpdateFill 不会覆盖，这里显式刷新
            checkin.setUpdateTime(checkinRuleCalculator.now());
            checkin.setUpdateBy(SecurityUtils.getUserId());
            updateById(checkin);
        }
    }

    private HealthCheckinVO buildOwnerVO(HealthCheckin checkin, SysUser user) {
        List<HealthCheckinPhotoVO> photos = new ArrayList<>();
        int photoCount = 0;
        if (checkin.getId() != null) {
            photos = healthCheckinPhotoService.listByCheckin(checkin.getId());
            photoCount = photos.size();
        }
        return HealthCheckinConverter.toCheckinVO(
                checkin, user, false, checkinRuleCalculator.isBackfilled(checkin), photos, photoCount);
    }

    private LocalDate resolveCheckinDate(LocalDate requested) {
        LocalDate today = checkinRuleCalculator.today();
        LocalDate date = requested == null ? today : requested;
        if (date.isAfter(today)) {
            throw ServiceException.badRequest("不能打卡未来日期");
        }
        int days = Math.max(1, checkinProperties.getBackfillDays());
        LocalDate earliest = today.minusDays(days - 1L);
        if (date.isBefore(earliest)) {
            throw ServiceException.badRequest("只能补最近" + days + "天的打卡");
        }
        return date;
    }

    private boolean isWithinBackfill(LocalDate date) {
        if (date == null) {
            return false;
        }
        LocalDate today = checkinRuleCalculator.today();
        int days = Math.max(1, checkinProperties.getBackfillDays());
        LocalDate earliest = today.minusDays(days - 1L);
        return !date.isBefore(earliest) && !date.isAfter(today);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw ServiceException.badRequest("开始日期不能大于结束日期");
        }
    }

    private YearMonth parseMonth(String month) {
        if (!ValidateUtils.hasText(month)) {
            return YearMonth.now(ZONE);
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (Exception e) {
            throw ServiceException.badRequest("月份格式应为 yyyy-MM");
        }
    }

    private int calcConsecutiveDays(Long userId, LocalDate today) {
        List<HealthCheckin> all = list(new LambdaQueryWrapper<HealthCheckin>()
                .select(HealthCheckin::getCheckinDate)
                .eq(HealthCheckin::getUserId, userId)
                .le(HealthCheckin::getCheckinDate, today));
        Set<LocalDate> dates = new HashSet<>();
        for (HealthCheckin checkin : all) {
            if (checkin.getCheckinDate() != null) {
                dates.add(checkin.getCheckinDate());
            }
        }
        LocalDate cursor = dates.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private List<Long> collectCheckinIds(List<HealthCheckin> records) {
        List<Long> ids = new ArrayList<>();
        if (records != null) {
            for (HealthCheckin checkin : records) {
                if (checkin.getId() != null) {
                    ids.add(checkin.getId());
                }
            }
        }
        return ids;
    }

    private Map<Long, Integer> loadPhotoCounts(List<Long> checkinIds) {
        Map<Long, Integer> counts = new HashMap<>();
        if (checkinIds == null || checkinIds.isEmpty()) {
            return counts;
        }
        List<HealthCheckinPhoto> photos = healthCheckinPhotoMapper.selectList(
                new LambdaQueryWrapper<HealthCheckinPhoto>()
                        .in(HealthCheckinPhoto::getCheckinId, checkinIds));
        for (HealthCheckinPhoto photo : photos) {
            counts.merge(photo.getCheckinId(), 1, Integer::sum);
        }
        return counts;
    }

    private Map<Long, SysUser> loadUsers(List<HealthCheckin> records) {
        Map<Long, SysUser> users = new HashMap<>();
        if (records == null || records.isEmpty()) {
            return users;
        }
        List<Long> userIds = new ArrayList<>();
        for (HealthCheckin checkin : records) {
            if (checkin.getUserId() != null) {
                userIds.add(checkin.getUserId());
            }
        }
        if (userIds.isEmpty()) {
            return users;
        }
        for (SysUser user : sysUserMapper.selectBatchIds(userIds)) {
            users.put(user.getId(), user);
        }
        return users;
    }

    private Integer emotionTypeFromScore(Integer moodScore) {
        if (moodScore == null) {
            return null;
        }
        switch (moodScore) {
            case 5:
                return 1;
            case 4:
                return 2;
            case 3:
                return 2;
            case 2:
                return 4;
            default:
                return 5;
        }
    }

    private Integer scoreFromEmotionType(Integer emotionType) {
        if (emotionType == null) {
            return null;
        }
        switch (emotionType) {
            case 1:
                return 5;
            case 2:
                return 4;
            case 3:
                return 2;
            case 4:
                return 2;
            default:
                return 1;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
