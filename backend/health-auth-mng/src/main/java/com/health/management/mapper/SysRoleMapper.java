package com.health.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.management.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 只加载启用且未删除的角色
     */
    List<SysRole> selectEnabledRolesByUserId(@Param("userId") Long userId);

    int countByRoleCodeIncludingDeleted(@Param("roleCode") String roleCode);

    int countByRoleCodeExcludingId(@Param("roleCode") String roleCode, @Param("id") Long id);

    /**
     * 按编码加载启用且未删除的角色，用于自主注册绑定内置角色
     */
    SysRole selectEnabledByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 锁定超级管理员角色行，串行化“最后一个管理员”相关操作
     */
    SysRole selectByRoleCodeForUpdate(@Param("roleCode") String roleCode);
}
