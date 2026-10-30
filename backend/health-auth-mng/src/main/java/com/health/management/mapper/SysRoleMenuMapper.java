package com.health.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.management.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    List<SysRoleMenu> selectByRoleId(@Param("roleId") Long roleId);

    List<SysRoleMenu> selectByMenuId(@Param("menuId") Long menuId);

    /**
     * 关联表物理删除，避免逻辑删除与唯一索引冲突
     */
    int deleteByRoleIdPhysically(@Param("roleId") Long roleId);

    /**
     * 菜单被角色引用数量，删除菜单前校验
     */
    int countByMenuId(@Param("menuId") Long menuId);
}
