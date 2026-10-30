package com.health.management.storage;

import com.health.management.common.exception.ServiceException;
import com.health.management.config.StorageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地磁盘存储实现，开发/测试环境使用。
 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private StorageProperties storageProperties;

    @Override
    public String upload(byte[] content, String originalFilename, String contentType, String directory) {
        String objectKey = buildObjectKey(directory, originalFilename);
        Path root = rootPath();
        Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) {
            throw ServiceException.badRequest("非法的文件路径");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            return objectKey;
        } catch (IOException e) {
            throw ServiceException.fail("保存文件失败：" + e.getMessage());
        }
    }

    @Override
    public void delete(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return;
        }
        Path root = rootPath();
        Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // 文件删除失败不影响数据库逻辑删除
        }
    }

    @Override
    public String getUrl(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return null;
        }
        String prefix = storageProperties.getUrlPrefix();
        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = "/uploads";
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix + "/" + objectKey.replace("\\", "/");
    }

    private Path rootPath() {
        return Paths.get(storageProperties.getLocalPath()).toAbsolutePath().normalize();
    }

    private String buildObjectKey(String directory, String originalFilename) {
        String ext = extension(originalFilename);
        String safeDir = sanitizeDirectory(directory);
        return safeDir + "/" + LocalDate.now().format(DATE_DIR) + "/"
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
