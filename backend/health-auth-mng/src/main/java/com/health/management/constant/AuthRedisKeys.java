package com.health.management.constant;

/**
 * 认证模块 Redis key 规范
 */
public final class AuthRedisKeys {

    /** Refresh Token 白名单：auth:refresh:{userId}:{sid} -> tokenVersion */
    public static final String REFRESH_PREFIX = "auth:refresh:";

    /** 用户所有 Refresh 会话索引（Set）：auth:refresh:index:{userId} -> [sid...] */
    public static final String REFRESH_INDEX_PREFIX = "auth:refresh:index:";

    /** Access Token 黑名单：auth:blacklist:{jti} -> 1（TTL=剩余有效期） */
    public static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private AuthRedisKeys() {
    }

    public static String refresh(Long userId, String sid) {
        return REFRESH_PREFIX + userId + ":" + sid;
    }

    public static String refreshIndex(Long userId) {
        return REFRESH_INDEX_PREFIX + userId;
    }

    public static String blacklist(String jti) {
        return BLACKLIST_PREFIX + jti;
    }
}
