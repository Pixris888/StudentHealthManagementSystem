package com.health.management.security;

import com.health.management.common.redis.RedisUtils;
import com.health.management.config.JwtProperties;
import com.health.management.constant.AuthRedisKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Refresh Token 会话管理：
 * - Redis 白名单保存 refresh 会话；
 * - 每个登录端一个独立 sid；
 * - 支持单会话注销、全端注销；
 * - Access Token 注销后进入黑名单，直到其自然过期。
 */
@Service
public class TokenSessionService {

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private JwtProperties jwtProperties;

    public String createSession(Long userId, Integer tokenVersion) {
        String sid = UUID.randomUUID().toString().replace("-", "");
        createSession(userId, tokenVersion, sid);
        return sid;
    }

    public void createSession(Long userId, Integer tokenVersion, String sid) {
        String key = AuthRedisKeys.refresh(userId, sid);
        String value = String.valueOf(tokenVersion == null ? 0 : tokenVersion);
        redisUtils.set(key, value, jwtProperties.getRefreshExpiration(), TimeUnit.MILLISECONDS);

        String indexKey = AuthRedisKeys.refreshIndex(userId);
        redisUtils.sAdd(indexKey, sid);
        // 索引比 refresh 多保留1小时，避免会话索引提前消失
        redisUtils.expire(indexKey, jwtProperties.getRefreshExpiration() + 3600000L, TimeUnit.MILLISECONDS);
    }

    public boolean validateSession(Long userId, String sid, Integer tokenVersion) {
        if (userId == null || sid == null || tokenVersion == null) {
            return false;
        }
        String value = redisUtils.get(AuthRedisKeys.refresh(userId, sid));
        return value != null
                && value.equals(String.valueOf(tokenVersion))
                && redisUtils.sIsMember(AuthRedisKeys.refreshIndex(userId), sid);
    }

    public void removeSession(Long userId, String sid) {
        if (userId == null || sid == null) {
            return;
        }
        redisUtils.delete(AuthRedisKeys.refresh(userId, sid));
        redisUtils.sRemove(AuthRedisKeys.refreshIndex(userId), sid);
    }

    public void removeAllSessions(Long userId) {
        if (userId == null) {
            return;
        }
        Set<String> sids = redisUtils.sMembers(AuthRedisKeys.refreshIndex(userId));
        for (String sid : sids) {
            redisUtils.delete(AuthRedisKeys.refresh(userId, sid));
        }
        redisUtils.delete(AuthRedisKeys.refreshIndex(userId));
    }

    public void removeAllSessions(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : userIds) {
            removeAllSessions(userId);
        }
    }

    public void blacklistAccessToken(String jti, long ttlMillis) {
        if (jti == null || ttlMillis <= 0) {
            return;
        }
        redisUtils.set(AuthRedisKeys.blacklist(jti), "1", ttlMillis, TimeUnit.MILLISECONDS);
    }

    public boolean isAccessTokenBlacklisted(String jti) {
        return jti != null && redisUtils.hasKey(AuthRedisKeys.blacklist(jti));
    }
}
