package com.health.management.service.ai;

import com.health.management.common.exception.ServiceException;
import com.health.management.common.redis.RedisUtils;
import com.health.management.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * AI 每日次数限制。
 * Redis 异常时放行，避免限流组件影响主流程；正常时按用户+日期计数。
 */
@Component
public class AiRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AiRateLimiter.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private AiProperties aiProperties;

    public void checkConsult(Long userId) {
        check("ai:consult:limit:", userId, aiProperties.getDailyLimit());
    }

    public void checkPlan(Long userId) {
        check("ai:plan:limit:", userId, aiProperties.getPlanDailyLimit());
    }

    private void check(String prefix, Long userId, Integer limit) {
        if (userId == null || limit == null || limit <= 0) {
            return;
        }
        String key = prefix + userId + ":" + LocalDate.now(ZONE);
        try {
            long count = redisUtils.increment(key, 1);
            if (count == 1) {
                redisUtils.expire(key, 1, TimeUnit.DAYS);
            }
            if (count > limit) {
                throw ServiceException.badRequest("今日 AI 使用次数已达上限（" + limit + "次），请明天再试");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI 限流检查失败，已放行：{}", e.getMessage());
        }
    }
}
