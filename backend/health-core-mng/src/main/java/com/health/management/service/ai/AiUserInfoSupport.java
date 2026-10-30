package com.health.management.service.ai;

import com.health.management.common.constant.RoleConstants;
import com.health.management.common.util.ValidateUtils;
import com.health.management.entity.SysUser;
import com.health.management.mapper.SysUserMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.vo.ai.AiConsultationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 管理端问答记录的用户信息加载与脱敏。
 */
@Component
public class AiUserInfoSupport {

    @Autowired
    private SysUserMapper sysUserMapper;

    public Map<Long, SysUser> loadUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> distinct = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId != null) {
                distinct.add(userId);
            }
        }
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUser> users = sysUserMapper.selectBatchIds(new ArrayList<>(distinct));
        Map<Long, SysUser> result = new HashMap<>();
        for (SysUser user : users) {
            result.put(user.getId(), user);
        }
        return result;
    }

    public boolean canViewFullIdentity() {
        return SecurityUtils.hasRole(RoleConstants.SUPER_ADMIN);
    }

    public void fillIdentity(AiConsultationVO vo, SysUser user, boolean full) {
        if (vo == null || user == null) {
            return;
        }
        if (full) {
            vo.setStudentNo(user.getStudentNo());
            vo.setRealName(user.getRealName());
        } else {
            vo.setStudentNo(maskStudentNo(user.getStudentNo()));
            vo.setRealName(maskName(user.getRealName()));
        }
    }

    public String maskStudentNo(String value) {
        if (!ValidateUtils.hasText(value)) {
            return "";
        }
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    public String maskName(String value) {
        if (!ValidateUtils.hasText(value)) {
            return "";
        }
        if (value.length() == 1) {
            return "*";
        }
        return value.substring(0, 1) + "**";
    }
}
