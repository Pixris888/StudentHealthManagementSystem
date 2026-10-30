package com.health.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.dto.SysOperLogQueryDTO;
import com.health.management.entity.SysOperLog;
import com.health.management.mapper.SysOperLogMapper;
import com.health.management.service.SysOperLogService;
import org.springframework.stereotype.Service;

/**
 * 操作日志业务实现（只读）。
 */
@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog>
        implements SysOperLogService {

    @Override
    public IPage<SysOperLog> pageLogs(SysOperLogQueryDTO query) {
        SysOperLogQueryDTO q = query == null ? new SysOperLogQueryDTO() : query;
        ValidateUtils.requirePage(q.getPage(), q.getSize());
        int current = q.getPage() == null ? 1 : q.getPage();
        int size = q.getSize() == null ? 10 : q.getSize();

        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        if (ValidateUtils.hasText(q.getKeyword())) {
            String keyword = q.getKeyword().trim();
            wrapper.and(w -> w.like(SysOperLog::getTitle, keyword)
                    .or()
                    .like(SysOperLog::getOperUserName, keyword)
                    .or()
                    .like(SysOperLog::getOperIp, keyword));
        }
        if (q.getBusinessType() != null) {
            wrapper.eq(SysOperLog::getBusinessType, q.getBusinessType());
        }
        if (q.getStatus() != null) {
            wrapper.eq(SysOperLog::getStatus, q.getStatus());
        }
        if (q.getOperUserId() != null) {
            wrapper.eq(SysOperLog::getOperUserId, q.getOperUserId());
        }
        if (q.getStartDate() != null) {
            wrapper.ge(SysOperLog::getOperTime, q.getStartDate().atStartOfDay());
        }
        if (q.getEndDate() != null) {
            wrapper.lt(SysOperLog::getOperTime, q.getEndDate().plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(SysOperLog::getOperTime)
                .orderByDesc(SysOperLog::getId);
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public SysOperLog getLog(Long id) {
        ValidateUtils.requireNotNull(id, "日志ID");
        SysOperLog log = getById(id);
        if (log == null) {
            throw ServiceException.notFound("操作日志不存在");
        }
        return log;
    }
}
