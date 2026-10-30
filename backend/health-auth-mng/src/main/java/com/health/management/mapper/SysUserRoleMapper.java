package com.health.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.management.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    List<SysUserRole> selectByUserId(@Param("userId") Long userId);

    List<SysUserRole> selectByUserIds(@Param("userIds") List<Long> userIds);

    int countByRoleId(@Param("roleId") Long roleId);

    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 关联表物理删除，避免逻辑删除与唯一索引冲突
     */
    int deleteByUserIdPhysically(@Param("userId") Long userId);

    /**
     * 统计拥有指定角色且启用、未删除的用户数
     */
    int countEnabledUsersByRoleCode(@Param("roleCode") String roleCode);

    int countByUserIdAndRoleCode(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    /**
     * 角色权限变化后，让该角色下所有用户的旧 token 失效
     */
    int incrementTokenVersionByRoleId(@Param("roleId") Long roleId);
}
