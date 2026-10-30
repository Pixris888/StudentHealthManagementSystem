package com.health.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.audit.AuditLogRecorder;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.dto.SysConfigQueryDTO;
import com.health.management.dto.SysConfigSaveDTO;
import com.health.management.entity.SysConfig;
import com.health.management.mapper.SysConfigMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统参数配置业务实现。
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig>
        implements SysConfigService {

    @Autowired
    private AuditLogRecorder auditLogRecorder;

    @Override
    public IPage<SysConfig> pageConfigs(SysConfigQueryDTO query) {
        SysConfigQueryDTO q = query == null ? new SysConfigQueryDTO() : query;
        ValidateUtils.requirePage(q.getPage(), q.getSize());
        int current = q.getPage() == null ? 1 : q.getPage();
        int size = q.getSize() == null ? 10 : q.getSize();

        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (ValidateUtils.hasText(q.getKeyword())) {
            String keyword = q.getKeyword().trim();
            wrapper.and(w -> w.like(SysConfig::getConfigName, keyword)
                    .or()
                    .like(SysConfig::getConfigKey, keyword)
                    .or()
                    .like(SysConfig::getConfigValue, keyword));
        }
        if (q.getConfigType() != null) {
            wrapper.eq(SysConfig::getConfigType, q.getConfigType());
        }
        wrapper.orderByAsc(SysConfig::getConfigType)
                .orderByAsc(SysConfig::getId);
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public SysConfig getConfig(Long id) {
        ValidateUtils.requireNotNull(id, "参数ID");
        SysConfig config = getById(id);
        if (config == null) {
            throw ServiceException.notFound("系统参数不存在");
        }
        return config;
    }

    @Override
    public SysConfig getByKey(String configKey) {
        ValidateUtils.requireText(configKey, "参数键名");
        SysConfig config = getOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey.trim()), false);
        if (config == null) {
            throw ServiceException.notFound("系统参数不存在");
        }
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createConfig(SysConfigSaveDTO dto) {
        validateDto(dto, false);
        String configKey = dto.getConfigKey().trim();
        checkConfigKeyUnique(configKey, null);

        SysConfig config = new SysConfig();
        config.setConfigName(dto.getConfigName().trim());
        config.setConfigKey(configKey);
        config.setConfigValue(dto.getConfigValue() == null ? "" : dto.getConfigValue().trim());
        config.setConfigType(dto.getConfigType() == null ? 1 : dto.getConfigType());
        config.setRemark(trimToNull(dto.getRemark()));
        if (!save(config)) {
            throw ServiceException.fail("系统参数保存失败，请稍后重试");
        }
        auditLogRecorder.record("参数设置", AuditLogRecorder.TYPE_ADD,
                "新增参数：" + configKey, true, "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(SysConfigSaveDTO dto) {
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireNotNull(dto.getId(), "参数ID");
        SysConfig existing = getConfig(dto.getId());
        validateDto(dto, true);

        String newKey = dto.getConfigKey().trim();
        int newType = dto.getConfigType() == null ? existing.getConfigType() : dto.getConfigType();
        if (Integer.valueOf(0).equals(existing.getConfigType())) {
            if (newType != 0) {
                throw ServiceException.badRequest("系统内置参数不能改为自定义参数");
            }
            if (!existing.getConfigKey().equals(newKey)) {
                throw ServiceException.badRequest("系统内置参数不能修改参数键名");
            }
        }
        checkConfigKeyUnique(newKey, existing.getId());

        boolean updated = update(new LambdaUpdateWrapper<SysConfig>()
                .eq(SysConfig::getId, existing.getId())
                .set(SysConfig::getConfigName, dto.getConfigName().trim())
                .set(SysConfig::getConfigKey, newKey)
                .set(SysConfig::getConfigValue, dto.getConfigValue() == null
                        ? existing.getConfigValue() : dto.getConfigValue().trim())
                .set(SysConfig::getConfigType, newType)
                .set(SysConfig::getRemark, trimToNull(dto.getRemark()))
                .set(SysConfig::getUpdateBy, SecurityUtils.getUserId())
                .set(SysConfig::getUpdateTime, LocalDateTime.now()));
        if (!updated && getById(existing.getId()) == null) {
            throw ServiceException.notFound("系统参数不存在");
        }
        auditLogRecorder.record("参数设置", AuditLogRecorder.TYPE_UPDATE,
                "修改参数：" + newKey, true, "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfigs(List<Long> ids) {
        ValidateUtils.requireIds(ids, "参数ID");
        List<SysConfig> configs = listByIds(ids);
        if (configs.size() != ids.size()) {
            throw ServiceException.notFound("部分系统参数不存在");
        }
        for (SysConfig config : configs) {
            if (Integer.valueOf(0).equals(config.getConfigType())) {
                throw ServiceException.badRequest("系统内置参数不能删除：" + config.getConfigKey());
            }
        }
        removeByIds(ids);
        auditLogRecorder.record("参数设置", AuditLogRecorder.TYPE_DELETE,
                "删除参数ID：" + ids, true, "");
    }

    private void validateDto(SysConfigSaveDTO dto, boolean update) {
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireText(dto.getConfigName(), "参数名称");
        ValidateUtils.requireMaxLength(dto.getConfigName(), 100, "参数名称");
        ValidateUtils.requireText(dto.getConfigKey(), "参数键名");
        ValidateUtils.requireMaxLength(dto.getConfigKey(), 100, "参数键名");
        if (dto.getConfigValue() != null) {
            ValidateUtils.requireMaxLength(dto.getConfigValue(), 500, "参数值");
        }
        if (dto.getRemark() != null) {
            ValidateUtils.requireMaxLength(dto.getRemark(), 500, "备注");
        }
        if (dto.getConfigType() != null) {
            ValidateUtils.requireRange(dto.getConfigType(), 0, 1, "参数类型");
        }
        if (update && dto.getId() == null) {
            throw ServiceException.badRequest("参数ID不能为空");
        }
    }

    private void checkConfigKeyUnique(String configKey, Long excludeId) {
        long count = count(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey)
                .ne(excludeId != null, SysConfig::getId, excludeId));
        if (count > 0) {
            throw ServiceException.conflict("参数键名已存在：" + configKey);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
