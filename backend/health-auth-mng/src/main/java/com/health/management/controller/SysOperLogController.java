package com.health.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.health.management.dto.SysOperLogQueryDTO;
import com.health.management.entity.SysOperLog;
import com.health.management.service.SysOperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 操作日志：只读，禁止新增/修改/删除。
 */
@RestController
@RequestMapping("/api/system/oper_log")
public class SysOperLogController {

    @Autowired
    private SysOperLogService sysOperLogService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:log:list')")
    public IPage<SysOperLog> page(@Valid SysOperLogQueryDTO query) {
        return sysOperLogService.pageLogs(query);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:log:list')")
    public SysOperLog getById(@PathVariable Long id) {
        return sysOperLogService.getLog(id);
    }
}
