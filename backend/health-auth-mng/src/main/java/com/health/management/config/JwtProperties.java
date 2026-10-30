package com.health.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置（对应 application.yml 的 jwt 节点）
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret = "health-management-secret-key-2026-very-long-random-string-for-jwt-signing";

    /** Access Token 有效期（毫秒），默认30分钟 */
    private long accessExpiration = 1800000L;

    /** Refresh Token 有效期（毫秒），默认7天 */
    private long refreshExpiration = 604800000L;
}
