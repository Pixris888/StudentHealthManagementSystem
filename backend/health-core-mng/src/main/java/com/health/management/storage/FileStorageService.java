package com.health.management.storage;

/**
 * 文件存储抽象：本地磁盘与阿里云 OSS 两种实现。
 * 数据库只保存 objectKey，展示时再生成访问 URL（OSS 为临时签名 URL）。
 */
public interface FileStorageService {

    /**
     * 上传文件并返回 objectKey。
     *
     * @param content        文件内容（已加水印）
     * @param originalFilename 原始文件名，用于提取扩展名
     * @param contentType    文件 MIME 类型
     * @param directory      业务目录，例如 health/checkin/7
     */
    String upload(byte[] content, String originalFilename, String contentType, String directory);

    /**
     * 删除 objectKey 对应的文件。
     */
    void delete(String objectKey);

    /**
     * 生成文件访问 URL。OSS 私有 Bucket 返回临时签名 URL。
     */
    String getUrl(String objectKey);
}
