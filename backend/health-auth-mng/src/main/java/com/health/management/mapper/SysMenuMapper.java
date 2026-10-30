package com.health.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.management.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 用户拥有的权限标识：用户 → 启用角色 → 启用菜单，过滤逻辑删除和空权限
     */
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 用户拥有的启用菜单（含按钮，业务层按 menu_type 过滤）
     */
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

    /**
     * 超级管理员使用：全部启用菜单的权限
     */
    List<String> selectAllEnabledPermissions();

    int countByPermsIncludingDeleted(@Param("perms") String perms);

    int countByPermsExcludingId(@Param("perms") String perms, @Param("id") Long id);

    int countByMenuNameAndParent(@Param("menuName") String menuName,
                                 @Param("parentId") Long parentId,
                                 @Param("id") Long id);
}
