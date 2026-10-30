package com.health.management.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.health.management.common.util.ValidateUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 仅当 storage.type=oss 时创建 OSSClient。
 */
@Configuration
@ConditionalOnProperty(name = "storage.type", havingValue = "oss")
public class OssConfig {

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient(AliyunOssProperties properties) {
        if (!ValidateUtils.hasText(properties.getEndpoint())
                || !ValidateUtils.hasText(properties.getBucketName())
                || !ValidateUtils.hasText(properties.getAccessKeyId())
                || !ValidateUtils.hasText(properties.getAccessKeySecret())) {
            throw new IllegalStateException(
                    "storage.type=oss 时，必须配置 aliyun.oss.endpoint/bucket-name/access-key-id/access-key-secret");
        }
        return new OSSClientBuilder().build(
                properties.getEndpoint(),
                properties.getAccessKeyId(),
                properties.getAccessKeySecret());
    }
}
