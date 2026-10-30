package com.health.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 数据备份配置（application.yml 的 health.backup 节点）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "health.backup")
public class BackupProperties {

    /** 备份文件存放目录，默认与项目上传目录分开，避免被静态资源直接访问 */
    private String dir = "./backup";
}
