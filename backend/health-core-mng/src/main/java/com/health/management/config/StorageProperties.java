package com.health.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置（application.yml 的 storage 节点）。
 * type=local 时使用本地磁盘，type=oss 时使用阿里云 OSS。
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** local 或 oss */
    private String type = "local";

    /** 本地存储根目录，默认用户目录下的 health-management-uploads */
    private String localPath = System.getProperty("user.home") + "/health-management-uploads";

    /** 本地文件访问 URL 前缀，默认 /uploads */
    private String urlPrefix = "/uploads";

    /** OSS 私有读签名 URL 有效期（分钟） */
    private int signedUrlExpireMinutes = 30;
}
