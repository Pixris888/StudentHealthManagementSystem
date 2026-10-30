package com.health.management.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.common.vo.MenuTree;
import com.health.management.dto.MenuSaveDTO;
import com.health.management.entity.SysMenu;

import java.util.List;

/**
 * 菜单与权限服务
 */
public interface SysMenuService extends IService<SysMenu> {

    /** 管理端完整菜单树（含按钮、含停用） */
    List<MenuTree> listMenuTree();

    /** 当前登录用户的路由树（目录 + 菜单，不含按钮） */
    List<MenuTree> listCurrentUserMenuTree();

    SysMenu getMenuDetail(Long menuId);

    Long createMenu(MenuSaveDTO dto);

    void updateMenu(MenuSaveDTO dto);

    void deleteMenu(Long menuId);

    /** 用户拥有的权限标识（含按钮） */
    List<String> getPermissionsByUserId(Long userId);

    /** 角色已授权的菜单 ID（含按钮） */
    List<Long> getMenuIdsByRoleId(Long roleId);
}
