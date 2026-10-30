package com.health.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.dto.SysOperLogQueryDTO;
import com.health.management.entity.SysOperLog;

/**
 * 操作日志业务接口（只读）。
 */
public interface SysOperLogService extends IService<SysOperLog> {

    IPage<SysOperLog> pageLogs(SysOperLogQueryDTO query);

    SysOperLog getLog(Long id);
}
