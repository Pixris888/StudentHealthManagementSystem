package com.health.management.service.support;

import com.health.management.common.constant.RoleConstants;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.entity.SysUser;
import com.health.management.mapper.SysUserMapper;
import com.health.management.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 健康业务模块的当前用户校验：统一处理登录账号、学生身份和学号绑定。
 */
@Component
public class HealthUserSupport {

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 获取当前登录账号，不存在时视为登录状态失效。
     */
    public SysUser requireUser() {
        Long userId = SecurityUtils.getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw ServiceException.unauthorized("登录账号不存在或已被删除");
        }
        return user;
    }

    /**
     * 健康档案、打卡、体测等个人健康数据仅允许学生账号操作。
     */
    public SysUser requireStudent() {
        SysUser user = requireUser();
        if (!Integer.valueOf(RoleConstants.USER_TYPE_STUDENT).equals(user.getUserType())) {
            throw ServiceException.forbidden("仅学生账号可操作个人健康数据");
        }
        if (!ValidateUtils.hasText(user.getStudentNo())) {
            throw ServiceException.badRequest("当前账号未绑定学号，请联系管理员");
        }
        return user;
    }
}
