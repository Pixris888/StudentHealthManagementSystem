package com.health.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.dto.SysConfigQueryDTO;
import com.health.management.dto.SysConfigSaveDTO;
import com.health.management.entity.SysConfig;

import java.util.List;

/**
 * 系统参数配置业务接口。
 */
public interface SysConfigService extends IService<SysConfig> {

    IPage<SysConfig> pageConfigs(SysConfigQueryDTO query);

    SysConfig getConfig(Long id);

    SysConfig getByKey(String configKey);

    void createConfig(SysConfigSaveDTO dto);

    void updateConfig(SysConfigSaveDTO dto);

    void deleteConfigs(List<Long> ids);
}
