package com.health.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.health.management.common.util.ValidateUtils;
import com.health.management.dto.SysConfigQueryDTO;
import com.health.management.dto.SysConfigSaveDTO;
import com.health.management.entity.SysConfig;
import com.health.management.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 系统参数设置：仅超级管理员可维护。
 */
@RestController
@RequestMapping("/api/system/config")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    @GetMapping("/page")
    public IPage<SysConfig> page(@Valid SysConfigQueryDTO query) {
        return sysConfigService.pageConfigs(query);
    }

    @GetMapping("/{id}")
    public SysConfig getById(@PathVariable Long id) {
        return sysConfigService.getConfig(id);
    }

    @GetMapping("/key/{configKey}")
    public SysConfig getByKey(@PathVariable String configKey) {
        return sysConfigService.getByKey(configKey);
    }

    @PostMapping
    public boolean add(@Valid @RequestBody SysConfigSaveDTO dto) {
        sysConfigService.createConfig(dto);
        return true;
    }

    @PutMapping
    public boolean update(@Valid @RequestBody SysConfigSaveDTO dto) {
        sysConfigService.updateConfig(dto);
        return true;
    }

    @DeleteMapping("/{ids}")
    public boolean remove(@PathVariable String ids) {
        sysConfigService.deleteConfigs(ValidateUtils.parseIds(ids));
        return true;
    }
}
