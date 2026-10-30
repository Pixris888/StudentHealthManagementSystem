package com.health.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.health.management.audit.AuditLogRecorder;
import com.health.management.common.util.ValidateUtils;
import com.health.management.dto.SysBackupQueryDTO;
import com.health.management.service.SysBackupService;
import com.health.management.vo.SysBackupRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;

/**
 * 数据库逻辑备份：仅超级管理员可操作。
 */
@RestController
@RequestMapping("/api/system/backup")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SysBackupController {

    @Autowired
    private SysBackupService sysBackupService;

    @Autowired
    private AuditLogRecorder auditLogRecorder;

    /**
     * 创建备份。备份内容为所有业务表的 CSV + manifest.txt，打包为 ZIP。
     */
    @PostMapping
    public SysBackupRecordVO create() {
        return sysBackupService.createBackup();
    }

    @GetMapping("/page")
    public IPage<SysBackupRecordVO> page(@Valid SysBackupQueryDTO query) {
        return sysBackupService.pageBackups(query);
    }

    @GetMapping("/{id}")
    public SysBackupRecordVO detail(@PathVariable Long id) {
        return sysBackupService.getBackup(id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Resource resource = sysBackupService.loadBackupResource(id);
        String fileName = resource.getFilename() == null ? "health_management_backup.zip" : resource.getFilename();
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM);
        try {
            builder.contentLength(resource.contentLength());
        } catch (IOException ignored) {
            // 无法获取大小时不设置 Content-Length，仍可下载
        }
        auditLogRecorder.record("数据备份", AuditLogRecorder.TYPE_EXPORT,
                "下载备份：" + fileName, true, "");
        return builder.body(resource);
    }

    @DeleteMapping("/{ids}")
    public boolean remove(@PathVariable String ids) {
        return sysBackupService.deleteBackups(ValidateUtils.parseIds(ids));
    }
}
