package com.health.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.redis.RedisUtils;
import com.health.management.common.util.ValidateUtils;
import com.health.management.config.CheckinProperties;
import com.health.management.convert.HealthCheckinConverter;
import com.health.management.dto.HealthCheckinSessionQueryDTO;
import com.health.management.dto.HealthCheckinSessionStartDTO;
import com.health.management.entity.HealthCheckinSession;
import com.health.management.entity.SysUser;
import com.health.management.mapper.HealthCheckinSessionMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.service.HealthCheckinService;
import com.health.management.service.HealthCheckinSessionService;
import com.health.management.service.support.CheckinRuleCalculator;
import com.health.management.service.support.HealthUserSupport;
import com.health.management.vo.HealthCheckinSessionEndVO;
import com.health.management.vo.HealthCheckinSessionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 睡眠/运动计时会话业务实现。
 * MySQL 保存最终开始/结束时间；Redis 只缓存进行中的会话和防重复开始锁。
 */
@Slf4j
@Service
public class HealthCheckinSessionServiceImpl extends ServiceImpl<HealthCheckinSessionMapper, HealthCheckinSession>
        implements HealthCheckinSessionService {

    private static final String ACTIVE_KEY_PREFIX = "health:session:active:";
    private static final String LOCK_KEY_PREFIX = "health:session:lock:";
    private static final long ACTIVE_TTL_HOURS = 24L;
    private static final long LOCK_TTL_SECONDS = 10L;

    @Autowired
    private HealthUserSupport healthUserSupport;

    @Autowired
    private HealthCheckinService healthCheckinService;

    @Autowired
    private CheckinRuleCalculator checkinRuleCalculator;

    @Autowired
    private CheckinProperties checkinProperties;

    @Autowired
    private RedisUtils redisUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthCheckinSessionVO start(HealthCheckinSessionStartDTO dto) {
        SysUser user = healthUserSupport.requireStudent();
        ValidateUtils.requireNotNull(dto, "请求参数");
        Integer type = dto.getSessionType();
        validateSessionType(type);
        if (Integer.valueOf(HealthCheckinSession.TYPE_EXERCISE).equals(type)) {
            if (!ValidateUtils.hasText(dto.getExerciseType())) {
                throw ServiceException.badRequest("运动打卡必须选择运动类型");
            }
            if (dto.getExerciseIntensity() == null) {
                throw ServiceException.badRequest("运动打卡必须选择运动强度");
            }
        }

        expireStale(user.getId(), type);
        if (!tryLock(user.getId(), type)) {
            throw ServiceException.conflict("操作过于频繁，请稍后重试");
        }
        try {
            HealthCheckinSession ongoing = findOngoing(user.getId(), type);
            if (ongoing != null) {
                // 幂等：已有进行中的会话时直接返回，前端可继续显示计时
                return HealthCheckinConverter.toSessionVO(ongoing);
            }

            LocalDateTime now = checkinRuleCalculator.now();
            HealthCheckinSession session = new HealthCheckinSession();
            session.setUserId(user.getId());
            session.setStudentNo(user.getStudentNo());
            session.setSessionType(type);
            session.setStartTime(now);
            session.setStatus(HealthCheckinSession.STATUS_ONGOING);
            if (Integer.valueOf(HealthCheckinSession.TYPE_EXERCISE).equals(type)) {
                session.setCheckinDate(now.toLocalDate());
                session.setExerciseType(dto.getExerciseType().trim());
                session.setExerciseIntensity(dto.getExerciseIntensity());
            }
            session.setDataQuality(0);
            session.setAbnormalReason("");
            try {
                save(session);
            } catch (DuplicateKeyException e) {
                HealthCheckinSession existing = findOngoing(user.getId(), type);
                if (existing != null) {
                    return HealthCheckinConverter.toSessionVO(existing);
                }
                throw ServiceException.conflict("已有进行中的" + sessionTypeName(type) + "打卡，请先结束");
            }
            setActiveKey(user.getId(), type, session.getId());
            return HealthCheckinConverter.toSessionVO(session);
        } finally {
            unlock(user.getId(), type);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthCheckinSessionEndVO end(Long id) {
        SysUser user = healthUserSupport.requireStudent();
        ValidateUtils.requireNotNull(id, "会话ID");
        HealthCheckinSession session = getById(id);
        if (session == null || !user.getId().equals(session.getUserId())) {
            throw ServiceException.notFound("计时会话不存在");
        }
        Integer type = session.getSessionType();
        if (!tryLock(user.getId(), type)) {
            throw ServiceException.conflict("操作过于频繁，请稍后重试");
        }
        try {
            session = getById(id);
            if (session == null || !user.getId().equals(session.getUserId())) {
                throw ServiceException.notFound("计时会话不存在");
            }
            if (Integer.valueOf(HealthCheckinSession.STATUS_FINISHED).equals(session.getStatus())) {
                return buildEndVO(session);
            }
            if (Integer.valueOf(HealthCheckinSession.STATUS_EXPIRED).equals(session.getStatus())) {
                throw ServiceException.badRequest("该计时会话已过期或已作废");
            }

            LocalDateTime now = checkinRuleCalculator.now();
            // 数据库 DATETIME 秒级精度可能把开始时间四舍五入到下一秒，保证结束时间不早于开始时间
            if (now.isBefore(session.getStartTime())) {
                now = session.getStartTime();
            }
            long minutes = Duration.between(session.getStartTime(), now).toMinutes();
            long maxMinutes = (long) Math.max(1, checkinProperties.getSessionMaxHours()) * 60L;

            if (minutes < 1) {
                markExpired(session, now, "计时时长过短，本次记录已作废");
                clearActiveKey(user.getId(), type);
                return buildExpiredVO(session);
            }
            if (minutes > maxMinutes) {
                markExpired(session, now, "计时超过" + checkinProperties.getSessionMaxHours()
                        + "小时，本次记录已作废");
                clearActiveKey(user.getId(), type);
                return buildExpiredVO(session);
            }

            session.setEndTime(now);
            session.setDurationMinutes((int) minutes);
            session.setStatus(HealthCheckinSession.STATUS_FINISHED);
            if (Integer.valueOf(HealthCheckinSession.TYPE_SLEEP).equals(type)) {
                // 睡眠归属到起床日期
                session.setCheckinDate(now.toLocalDate());
            } else if (session.getCheckinDate() == null) {
                session.setCheckinDate(session.getStartTime().toLocalDate());
            }

            List<String> reasons = checkinRuleCalculator.sessionReasons(type, (int) minutes);
            if (hasOverlappingSession(session)) {
                reasons.add("睡眠与运动时间重叠");
            }
            session.setDataQuality(checkinRuleCalculator.qualityOf(reasons));
            session.setAbnormalReason(checkinRuleCalculator.joinReasons(reasons));
            updateSession(session);

            HealthCheckinSessionEndVO result = buildEndVO(session);
            clearActiveKey(user.getId(), type);
            return result;
        } finally {
            unlock(user.getId(), type);
        }
    }

    @Override
    public HealthCheckinSessionVO getActive(Integer sessionType) {
        SysUser user = healthUserSupport.requireStudent();
        validateSessionType(sessionType);
        expireStale(user.getId(), sessionType);

        Long activeId = getActiveId(user.getId(), sessionType);
        if (activeId != null) {
            HealthCheckinSession session = getById(activeId);
            if (session != null
                    && user.getId().equals(session.getUserId())
                    && Integer.valueOf(HealthCheckinSession.STATUS_ONGOING).equals(session.getStatus())) {
                return HealthCheckinConverter.toSessionVO(session);
            }
            clearActiveKey(user.getId(), sessionType);
        }

        HealthCheckinSession ongoing = findOngoing(user.getId(), sessionType);
        if (ongoing == null) {
            return null;
        }
        setActiveKey(user.getId(), sessionType, ongoing.getId());
        return HealthCheckinConverter.toSessionVO(ongoing);
    }

    @Override
    public IPage<HealthCheckinSessionVO> pageMine(HealthCheckinSessionQueryDTO query) {
        SysUser user = healthUserSupport.requireStudent();
        if (query == null) {
            query = new HealthCheckinSessionQueryDTO();
        }
        ValidateUtils.requirePage(query.getPage(), query.getSize());
        if (query.getStartDate() != null && query.getEndDate() != null
                && query.getStartDate().isAfter(query.getEndDate())) {
            throw ServiceException.badRequest("开始日期不能大于结束日期");
        }
        int current = query.getPage() == null ? 1 : query.getPage();
        int size = query.getSize() == null ? 10 : query.getSize();

        LambdaQueryWrapper<HealthCheckinSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthCheckinSession::getUserId, user.getId());
        if (query.getSessionType() != null) {
            wrapper.eq(HealthCheckinSession::getSessionType, query.getSessionType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(HealthCheckinSession::getStatus, query.getStatus());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(HealthCheckinSession::getCheckinDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(HealthCheckinSession::getCheckinDate, query.getEndDate());
        }
        wrapper.orderByDesc(HealthCheckinSession::getStartTime)
                .orderByDesc(HealthCheckinSession::getId);

        IPage<HealthCheckinSession> page = page(new Page<>(current, size), wrapper);
        Page<HealthCheckinSessionVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<HealthCheckinSessionVO> records = new ArrayList<>();
        for (HealthCheckinSession session : page.getRecords()) {
            records.add(HealthCheckinConverter.toSessionVO(session));
        }
        voPage.setRecords(records);
        return voPage;
    }

    private HealthCheckinSessionEndVO buildEndVO(HealthCheckinSession session) {
        HealthCheckinSessionEndVO result = new HealthCheckinSessionEndVO();
        result.setSession(HealthCheckinConverter.toSessionVO(session));
        result.setCheckin(healthCheckinService.syncFromSession(session));
        return result;
    }

    private HealthCheckinSessionEndVO buildExpiredVO(HealthCheckinSession session) {
        HealthCheckinSessionEndVO result = new HealthCheckinSessionEndVO();
        result.setSession(HealthCheckinConverter.toSessionVO(session));
        result.setCheckin(null);
        return result;
    }

    private void markExpired(HealthCheckinSession session, LocalDateTime endTime, String reason) {
        session.setEndTime(endTime);
        session.setDurationMinutes(null);
        session.setStatus(HealthCheckinSession.STATUS_EXPIRED);
        session.setDataQuality(1);
        session.setAbnormalReason(reason);
        if (session.getCheckinDate() == null) {
            session.setCheckinDate(session.getStartTime().toLocalDate());
        }
        updateSession(session);
    }

    private void updateSession(HealthCheckinSession session) {
        session.setUpdateTime(checkinRuleCalculator.now());
        session.setUpdateBy(SecurityUtils.getUserId());
        updateById(session);
    }

    /**
     * 超过最大时长仍未结束的会话自动作废，避免唯一键一直被占用。
     */
    private void expireStale(Long userId, Integer sessionType) {
        HealthCheckinSession ongoing = findOngoing(userId, sessionType);
        if (ongoing == null || ongoing.getStartTime() == null) {
            return;
        }
        int maxHours = Math.max(1, checkinProperties.getSessionMaxHours());
        LocalDateTime deadline = ongoing.getStartTime().plusHours(maxHours);
        if (checkinRuleCalculator.now().isBefore(deadline)) {
            return;
        }
        ongoing.setStatus(HealthCheckinSession.STATUS_EXPIRED);
        ongoing.setEndTime(null);
        ongoing.setDurationMinutes(null);
        ongoing.setDataQuality(1);
        ongoing.setAbnormalReason("计时超过" + maxHours + "小时未结束，已自动作废");
        if (ongoing.getCheckinDate() == null) {
            ongoing.setCheckinDate(ongoing.getStartTime().toLocalDate());
        }
        updateSession(ongoing);
        clearActiveKey(userId, sessionType);
    }

    private HealthCheckinSession findOngoing(Long userId, Integer sessionType) {
        return getOne(new LambdaQueryWrapper<HealthCheckinSession>()
                .eq(HealthCheckinSession::getUserId, userId)
                .eq(HealthCheckinSession::getSessionType, sessionType)
                .eq(HealthCheckinSession::getStatus, HealthCheckinSession.STATUS_ONGOING), false);
    }

    /**
     * 检查当前会话时间是否与另一类型（睡眠/运动）的已结束会话重叠。
     */
    private boolean hasOverlappingSession(HealthCheckinSession session) {
        if (session.getStartTime() == null || session.getEndTime() == null) {
            return false;
        }
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<HealthCheckinSession>()
                .eq(HealthCheckinSession::getUserId, session.getUserId())
                .ne(HealthCheckinSession::getId, session.getId())
                .ne(HealthCheckinSession::getSessionType, session.getSessionType())
                .eq(HealthCheckinSession::getStatus, HealthCheckinSession.STATUS_FINISHED)
                .lt(HealthCheckinSession::getStartTime, session.getEndTime())
                .gt(HealthCheckinSession::getEndTime, session.getStartTime()));
        return count != null && count > 0;
    }

    private void validateSessionType(Integer sessionType) {
        if (sessionType == null
                || (!Integer.valueOf(HealthCheckinSession.TYPE_SLEEP).equals(sessionType)
                && !Integer.valueOf(HealthCheckinSession.TYPE_EXERCISE).equals(sessionType))) {
            throw ServiceException.badRequest("会话类型只能是1-睡眠、2-运动");
        }
    }

    private String sessionTypeName(Integer type) {
        return HealthCheckinConverter.sessionTypeName(type);
    }

    private boolean tryLock(Long userId, Integer type) {
        try {
            return redisUtils.setIfAbsent(lockKey(userId, type), "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (RuntimeException e) {
            // Redis 不可用时降级为数据库唯一键校验
            log.warn("Redis 打卡锁不可用，降级为数据库校验: {}", e.getMessage());
            return true;
        }
    }

    private void unlock(Long userId, Integer type) {
        try {
            redisUtils.delete(lockKey(userId, type));
        } catch (RuntimeException e) {
            log.warn("释放 Redis 打卡锁失败: {}", e.getMessage());
        }
    }

    private void setActiveKey(Long userId, Integer type, Long sessionId) {
        try {
            redisUtils.set(activeKey(userId, type), String.valueOf(sessionId),
                    ACTIVE_TTL_HOURS, TimeUnit.HOURS);
        } catch (RuntimeException e) {
            log.warn("写入 Redis 活跃会话缓存失败: {}", e.getMessage());
        }
    }

    private void clearActiveKey(Long userId, Integer type) {
        try {
            redisUtils.delete(activeKey(userId, type));
        } catch (RuntimeException e) {
            log.warn("删除 Redis 活跃会话缓存失败: {}", e.getMessage());
        }
    }

    private Long getActiveId(Long userId, Integer type) {
        try {
            String value = redisUtils.get(activeKey(userId, type));
            return ValidateUtils.hasText(value) ? Long.valueOf(value.trim()) : null;
        } catch (RuntimeException e) {
            log.warn("读取 Redis 活跃会话缓存失败: {}", e.getMessage());
            return null;
        }
    }

    private String activeKey(Long userId, Integer type) {
        return ACTIVE_KEY_PREFIX + userId + ":" + type;
    }

    private String lockKey(Long userId, Integer type) {
        return LOCK_KEY_PREFIX + userId + ":" + type;
    }
}
