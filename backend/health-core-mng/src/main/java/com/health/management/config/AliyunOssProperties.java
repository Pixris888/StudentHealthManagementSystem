package com.health.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置（application.yml 的 aliyun.oss 节点）。
 * AccessKey 建议通过环境变量传入，不要提交到代码仓库。
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliyunOssProperties {

    private String endpoint;

    private String bucketName;

    private String accessKeyId;

    private String accessKeySecret;

    /** 打卡照片在 bucket 中的根目录 */
    private String checkinDir = "health/checkin/";
}
