package com.health.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.audit.AuditLogRecorder;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.dto.SysDictDataQueryDTO;
import com.health.management.dto.SysDictDataSaveDTO;
import com.health.management.entity.SysDictData;
import com.health.management.entity.SysDictType;
import com.health.management.mapper.SysDictDataMapper;
import com.health.management.mapper.SysDictTypeMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.service.SysDictDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 字典数据业务实现。
 */
@Service
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData>
        implements SysDictDataService {

    @Autowired
    private SysDictTypeMapper sysDictTypeMapper;

    @Autowired
    private AuditLogRecorder auditLogRecorder;

    @Override
    public IPage<SysDictData> pageData(SysDictDataQueryDTO query) {
        SysDictDataQueryDTO q = query == null ? new SysDictDataQueryDTO() : query;
        ValidateUtils.requirePage(q.getPage(), q.getSize());
        int current = q.getPage() == null ? 1 : q.getPage();
        int size = q.getSize() == null ? 10 : q.getSize();

        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        if (ValidateUtils.hasText(q.getDictType())) {
            wrapper.eq(SysDictData::getDictType, q.getDictType().trim());
        }
        if (ValidateUtils.hasText(q.getKeyword())) {
            String keyword = q.getKeyword().trim();
            wrapper.and(w -> w.like(SysDictData::getDictLabel, keyword)
                    .or()
                    .like(SysDictData::getDictValue, keyword));
        }
        if (q.getStatus() != null) {
            wrapper.eq(SysDictData::getStatus, q.getStatus());
        }
        wrapper.orderByAsc(SysDictData::getDictType)
                .orderByAsc(SysDictData::getDictSort)
                .orderByAsc(SysDictData::getId);
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public List<SysDictData> listEnabledByType(String dictType) {
        ValidateUtils.requireText(dictType, "字典类型");
        SysDictType type = findType(dictType.trim());
        if (type == null || !Integer.valueOf(1).equals(type.getStatus())) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, type.getDictType())
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getDictSort)
                .orderByAsc(SysDictData::getId));
    }

    @Override
    public SysDictData getData(Long id) {
        ValidateUtils.requireNotNull(id, "字典数据ID");
        SysDictData data = getById(id);
        if (data == null) {
            throw ServiceException.notFound("字典数据不存在");
        }
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createData(SysDictDataSaveDTO dto) {
        validateDto(dto, false);
        String dictType = dto.getDictType().trim();
        requireType(dictType);
        String dictValue = dto.getDictValue().trim();
        checkDictValueUnique(dictType, dictValue, null);

        SysDictData data = new SysDictData();
        data.setDictType(dictType);
        data.setDictLabel(dto.getDictLabel().trim());
        data.setDictValue(dictValue);
        data.setDictSort(dto.getDictSort() == null ? 0 : dto.getDictSort());
        data.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        data.setRemark(trimToNull(dto.getRemark()));
        if (!save(data)) {
            throw ServiceException.fail("字典数据保存失败，请稍后重试");
        }
        auditLogRecorder.record("字典管理", AuditLogRecorder.TYPE_ADD,
                "新增字典数据：" + dictType + "/" + dictValue, true, "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateData(SysDictDataSaveDTO dto) {
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireNotNull(dto.getId(), "字典数据ID");
        SysDictData existing = getData(dto.getId());
        validateDto(dto, true);

        String dictType = dto.getDictType().trim();
        requireType(dictType);
        String dictValue = dto.getDictValue().trim();
        checkDictValueUnique(dictType, dictValue, existing.getId());

        boolean updated = update(new LambdaUpdateWrapper<SysDictData>()
                .eq(SysDictData::getId, existing.getId())
                .set(SysDictData::getDictType, dictType)
                .set(SysDictData::getDictLabel, dto.getDictLabel().trim())
                .set(SysDictData::getDictValue, dictValue)
                .set(SysDictData::getDictSort, dto.getDictSort() == null
                        ? existing.getDictSort() : dto.getDictSort())
                .set(SysDictData::getStatus, dto.getStatus() == null ? existing.getStatus() : dto.getStatus())
                .set(SysDictData::getRemark, trimToNull(dto.getRemark()))
                .set(SysDictData::getUpdateBy, SecurityUtils.getUserId())
                .set(SysDictData::getUpdateTime, LocalDateTime.now()));
        if (!updated && getById(existing.getId()) == null) {
            throw ServiceException.notFound("字典数据不存在");
        }
        auditLogRecorder.record("字典管理", AuditLogRecorder.TYPE_UPDATE,
                "修改字典数据：" + dictType + "/" + dictValue, true, "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteData(List<Long> ids) {
        ValidateUtils.requireIds(ids, "字典数据ID");
        List<SysDictData> dataList = listByIds(ids);
        if (dataList.size() != ids.size()) {
            throw ServiceException.notFound("部分字典数据不存在");
        }
        removeByIds(ids);
        auditLogRecorder.record("字典管理", AuditLogRecorder.TYPE_DELETE,
                "删除字典数据ID：" + ids, true, "");
    }

    private void validateDto(SysDictDataSaveDTO dto, boolean update) {
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireText(dto.getDictType(), "字典类型");
        ValidateUtils.requireMaxLength(dto.getDictType(), 100, "字典类型");
        ValidateUtils.requireText(dto.getDictLabel(), "字典标签");
        ValidateUtils.requireMaxLength(dto.getDictLabel(), 100, "字典标签");
        ValidateUtils.requireText(dto.getDictValue(), "字典键值");
        ValidateUtils.requireMaxLength(dto.getDictValue(), 100, "字典键值");
        if (dto.getDictSort() != null && dto.getDictSort() < 0) {
            throw ServiceException.badRequest("显示顺序不能小于0");
        }
        if (dto.getStatus() != null) {
            ValidateUtils.requireRange(dto.getStatus(), 0, 1, "状态");
        }
        if (dto.getRemark() != null) {
            ValidateUtils.requireMaxLength(dto.getRemark(), 500, "备注");
        }
        if (update && dto.getId() == null) {
            throw ServiceException.badRequest("字典数据ID不能为空");
        }
    }

    private SysDictType requireType(String dictType) {
        SysDictType type = findType(dictType);
        if (type == null) {
            throw ServiceException.badRequest("字典类型不存在：" + dictType);
        }
        return type;
    }

    private SysDictType findType(String dictType) {
        return sysDictTypeMapper.selectOne(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictType, dictType)
                .last("LIMIT 1"));
    }

    private void checkDictValueUnique(String dictType, String dictValue, Long excludeId) {
        long count = count(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getDictValue, dictValue)
                .ne(excludeId != null, SysDictData::getId, excludeId));
        if (count > 0) {
            throw ServiceException.conflict("该字典类型下键值已存在：" + dictValue);
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
