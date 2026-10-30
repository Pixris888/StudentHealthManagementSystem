package com.health.management.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.health.management.common.exception.ServiceException;
import com.health.management.config.AliyunOssProperties;
import com.health.management.config.StorageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

/**
 * 阿里云 OSS 私有 Bucket 存储实现。
 * photo_url 保存 objectKey，接口返回时生成临时签名 URL。
 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "oss")
public class AliyunOssFileStorageService implements FileStorageService {

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private OSS ossClient;

    @Autowired
    private AliyunOssProperties ossProperties;

    @Autowired
    private StorageProperties storageProperties;

    @Override
    public String upload(byte[] content, String originalFilename, String contentType, String directory) {
        String objectKey = buildObjectKey(directory, originalFilename);
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(content.length);
            if (contentType != null && !contentType.trim().isEmpty()) {
                metadata.setContentType(contentType);
            }
            ossClient.putObject(ossProperties.getBucketName(), objectKey,
                    new ByteArrayInputStream(content), metadata);
            return objectKey;
        } catch (Exception e) {
            throw ServiceException.fail("上传照片到 OSS 失败：" + e.getMessage());
        }
    }

    @Override
    public void delete(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return;
        }
        try {
            ossClient.deleteObject(ossProperties.getBucketName(), objectKey);
        } catch (Exception ignored) {
            // 删除失败不影响数据库逻辑删除，后续可通过 OSS 生命周期规则清理
        }
    }

    @Override
    public String getUrl(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return null;
        }
        int minutes = storageProperties.getSignedUrlExpireMinutes();
        if (minutes <= 0) {
            minutes = 30;
        }
        Date expiration = new Date(System.currentTimeMillis() + minutes * 60_000L);
        return ossClient.generatePresignedUrl(
                ossProperties.getBucketName(), objectKey, expiration).toString();
    }

    private String buildObjectKey(String directory, String originalFilename) {
        String ext = extension(originalFilename);
        String safeDir = sanitizeDirectory(directory);
        String baseDir = ossProperties.getCheckinDir();
        if (baseDir == null) {
            baseDir = "";
        }
        baseDir = baseDir.replace("\\", "/").replaceAll("^/+", "");
        if (!baseDir.isEmpty() && !baseDir.endsWith("/")) {
            baseDir = baseDir + "/";
        }
        return baseDir + safeDir + "/" + LocalDate.now().format(DATE_DIR) + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + ext;
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return "png".equals(ext) ? "png" : "jpg";
    }

    private String sanitizeDirectory(String directory) {
        if (directory == null) {
            return "health/checkin";
        }
        String value = directory.replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "");
        value = value.replaceAll("[^a-zA-Z0-9/_-]", "");
        return value.isEmpty() ? "health/checkin" : value;
    }
}
