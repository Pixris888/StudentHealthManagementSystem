package com.health.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.audit.AuditLogRecorder;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.dto.SysDictTypeQueryDTO;
import com.health.management.dto.SysDictTypeSaveDTO;
import com.health.management.entity.SysDictData;
import com.health.management.entity.SysDictType;
import com.health.management.mapper.SysDictDataMapper;
import com.health.management.mapper.SysDictTypeMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.service.SysDictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 字典类型业务实现。
 */
@Service
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType>
        implements SysDictTypeService {

    @Autowired
    private SysDictDataMapper sysDictDataMapper;

    @Autowired
    private AuditLogRecorder auditLogRecorder;

    @Override
    public IPage<SysDictType> pageTypes(SysDictTypeQueryDTO query) {
        SysDictTypeQueryDTO q = query == null ? new SysDictTypeQueryDTO() : query;
        ValidateUtils.requirePage(q.getPage(), q.getSize());
        int current = q.getPage() == null ? 1 : q.getPage();
        int size = q.getSize() == null ? 10 : q.getSize();

        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        if (ValidateUtils.hasText(q.getKeyword())) {
            String keyword = q.getKeyword().trim();
            wrapper.and(w -> w.like(SysDictType::getDictName, keyword)
                    .or()
                    .like(SysDictType::getDictType, keyword));
        }
        if (q.getStatus() != null) {
            wrapper.eq(SysDictType::getStatus, q.getStatus());
        }
        wrapper.orderByAsc(SysDictType::getId);
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public List<SysDictType> listEnabledTypes() {
        return list(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getStatus, 1)
                .orderByAsc(SysDictType::getId));
    }

    @Override
    public SysDictType getType(Long id) {
        ValidateUtils.requireNotNull(id, "字典类型ID");
        SysDictType type = getById(id);
        if (type == null) {
            throw ServiceException.notFound("字典类型不存在");
        }
        return type;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createType(SysDictTypeSaveDTO dto) {
        validateDto(dto, false);
        String dictType = dto.getDictType().trim();
        checkDictTypeUnique(dictType, null);

        SysDictType type = new SysDictType();
        type.setDictName(dto.getDictName().trim());
        type.setDictType(dictType);
        type.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        type.setRemark(trimToNull(dto.getRemark()));
        if (!save(type)) {
            throw ServiceException.fail("字典类型保存失败，请稍后重试");
        }
        auditLogRecorder.record("字典管理", AuditLogRecorder.TYPE_ADD,
                "新增字典类型：" + dictType, true, "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateType(SysDictTypeSaveDTO dto) {
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireNotNull(dto.getId(), "字典类型ID");
        SysDictType existing = getType(dto.getId());
        validateDto(dto, true);

        String newType = dto.getDictType().trim();
        if (!existing.getDictType().equals(newType)) {
            long dataCount = countDataByType(existing.getDictType());
            if (dataCount > 0) {
                throw ServiceException.conflict("该字典类型下已有数据，不能修改字典类型编码");
            }
            checkDictTypeUnique(newType, existing.getId());
        }

        boolean updated = update(new LambdaUpdateWrapper<SysDictType>()
                .eq(SysDictType::getId, existing.getId())
                .set(SysDictType::getDictName, dto.getDictName().trim())
                .set(SysDictType::getDictType, newType)
                .set(SysDictType::getStatus, dto.getStatus() == null ? existing.getStatus() : dto.getStatus())
                .set(SysDictType::getRemark, trimToNull(dto.getRemark()))
                .set(SysDictType::getUpdateBy, SecurityUtils.getUserId())
                .set(SysDictType::getUpdateTime, LocalDateTime.now()));
        if (!updated && getById(existing.getId()) == null) {
            throw ServiceException.notFound("字典类型不存在");
        }
        auditLogRecorder.record("字典管理", AuditLogRecorder.TYPE_UPDATE,
                "修改字典类型：" + newType, true, "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTypes(List<Long> ids) {
        ValidateUtils.requireIds(ids, "字典类型ID");
        List<SysDictType> types = listByIds(ids);
        if (types.size() != ids.size()) {
            throw ServiceException.notFound("部分字典类型不存在");
        }
        for (SysDictType type : types) {
            if (countDataByType(type.getDictType()) > 0) {
                throw ServiceException.conflict("字典类型下存在字典数据，不能删除：" + type.getDictType());
            }
        }
        removeByIds(ids);
        auditLogRecorder.record("字典管理", AuditLogRecorder.TYPE_DELETE,
                "删除字典类型ID：" + ids, true, "");
    }

    private void validateDto(SysDictTypeSaveDTO dto, boolean update) {
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireText(dto.getDictName(), "字典名称");
        ValidateUtils.requireMaxLength(dto.getDictName(), 100, "字典名称");
        ValidateUtils.requireText(dto.getDictType(), "字典类型");
        ValidateUtils.requireMaxLength(dto.getDictType(), 100, "字典类型");
        if (!dto.getDictType().trim().matches("^[a-z][a-z0-9_]{1,99}$")) {
            throw ServiceException.badRequest("字典类型必须为小写字母开头，只能包含小写字母、数字、下划线");
        }
        if (dto.getStatus() != null) {
            ValidateUtils.requireRange(dto.getStatus(), 0, 1, "状态");
        }
        if (dto.getRemark() != null) {
            ValidateUtils.requireMaxLength(dto.getRemark(), 500, "备注");
        }
        if (update && dto.getId() == null) {
            throw ServiceException.badRequest("字典类型ID不能为空");
        }
    }

    private void checkDictTypeUnique(String dictType, Long excludeId) {
        long count = count(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictType, dictType)
                .ne(excludeId != null, SysDictType::getId, excludeId));
        if (count > 0) {
            throw ServiceException.conflict("字典类型已存在：" + dictType);
        }
    }

    private long countDataByType(String dictType) {
        return sysDictDataMapper.selectCount(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
