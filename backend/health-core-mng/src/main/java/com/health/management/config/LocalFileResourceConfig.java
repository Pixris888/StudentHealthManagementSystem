package com.health.management.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地存储模式下，把 /uploads/** 映射到本地文件目录，仅用于开发/演示。
 * 生产环境建议使用 OSS 私有 Bucket + 签名 URL。
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileResourceConfig implements WebMvcConfigurer {

    @Autowired
    private StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = storageProperties.getUrlPrefix();
        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = "/uploads";
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        Path root = Paths.get(storageProperties.getLocalPath()).toAbsolutePath().normalize();
        String location = root.toUri().toString();
        log.info("本地照片静态映射: handler={}** location={}", prefix, location);
        registry.addResourceHandler(prefix + "**")
                .addResourceLocations(location);
    }
}
